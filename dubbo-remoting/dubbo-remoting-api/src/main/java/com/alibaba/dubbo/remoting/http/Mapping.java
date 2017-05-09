package com.alibaba.dubbo.remoting.http;

import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

/**
 * Created by haoning1 on 2017/4/6.
 */
public class Mapping {
    private static LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
    public static Map<Method, Schema> cache = new LinkedHashMap<Method, Schema>();
    public static Map<String, Method> mapping = new LinkedHashMap<String, Method>();
    public static Map<String, List<Method>> defaultMapping = new LinkedHashMap<String, List<Method>>();
    public static Map<String, RequestMeta> metas = new LinkedHashMap<String, RequestMeta>();
    public static Map<String, List<RequestMeta>> defaultMetas = new LinkedHashMap<String, List<RequestMeta>>();
    public static Map<Method, Method> methods = new LinkedHashMap<Method, Method>();

    public static void push(Method method, RequestMeta requestMeta, String interfaceName) {
        String[] parameterNames = discoverer.getParameterNames(method);
        Type[] types = method.getGenericParameterTypes();

        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<String, ParameterMeta> parameterMeta = new LinkedHashMap<String, ParameterMeta>();
        ParameterMeta[] parameterMetas = new ParameterMeta[parameterNames.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> classType = parameterTypes[i];
            String parameterType = classType.getName();
            String parameterName = parameterNames[i];
            ParameterMeta parameter = new ParameterMeta();
            parameter.setName(parameterName);
            parameter.setRealname(parameterName);
            parameter.setParameterType(parameterType);
            parameter.setType(getType(parameterType));
            parameter.setIndex(i);
            parameter.setParameterTypePlus(types[i]);

            parameterMeta.put(parameterName, parameter);
            parameterMetas[i] = parameter;
        }

        requestMeta.setParameterMetas(parameterMetas);

        Schema schema = new Schema();
        schema.setRequestMeta(requestMeta);
        schema.setInterfaceName(interfaceName);
        schema.setMethodName(method.getName());
        schema.setParameterMeta(parameterMeta);
        push(method, schema);

    }

    public static String getType(String parameterType) {
        String type = "";
        if (parameterType.matches("(byte|short|int|long|float|double|boolean|char)")) return parameterType;
        if (parameterType.equals("java.lang.Byte")) return "int";
        if (parameterType.equals("java.lang.Short")) return "short";
        if (parameterType.equals("java.lang.Integer")) return "int";
        if (parameterType.equals("java.lang.Long")) return "long";
        if (parameterType.equals("java.lang.Float")) return "float";
        if (parameterType.equals("java.lang.Double")) return "double";
        if (parameterType.equals("java.lang.Boolean")) return "boolean";
        if (parameterType.equals("java.lang.Character")) return "char";
        if (parameterType.equals("java.lang.String")) return "String";
        if (parameterType.startsWith("[L") || parameterType.endsWith("List") || parameterType.endsWith("Set"))
            return "JSONArray";
        return "JSONString";
    }

    public static void push(Method method, Schema schema) {
        cache.put(method, schema);
    }

    public static void push(Method method, String uri, RequestMeta requestMeta) {
        mapping.put(uri, method);
        metas.put(uri, requestMeta);
    }

    public static void push(Method method, String[] interfaces) {
        for (String interfaceName : interfaces) {
            String uri = "/" + interfaceName + "/" + method.getName();
            List<Method> methods = null;
            List<RequestMeta> requestMetas = null;
            if (!mapping.containsKey(uri)) {
                methods = new ArrayList<Method>();
                requestMetas = new ArrayList<RequestMeta>();
                defaultMapping.put(uri, methods);
                defaultMetas.put(uri, requestMetas);
            }
            methods.add(method);
            RequestMeta requestMeta = new RequestMeta();
            requestMetas.add(requestMeta);
//            push(method, requestMeta);
            push(method, requestMeta, interfaceName);
        }
    }

    public static void push(Method interfaceMethod, Method implMethod) {
        methods.put(interfaceMethod, implMethod);
    }

    public static boolean isMapping(String uri) {
        return mapping.containsKey(uri) || defaultMapping.containsKey(uri);
    }

    public static boolean isMapping(Method method) {
        return methods.containsKey(method);
    }

    public static boolean isGet(String uri) throws Exception {
        return isMethod(uri, "GET");
    }

    public static boolean isPost(String uri) throws Exception {
        return isMethod(uri, "POST");
    }

    public static boolean isMethod(String uri, String method) throws Exception {
        if (!isMapping(uri)) {
            throw new Exception(String.format("Request Uri %s is not Mapping", uri));
        }

        RequestMeta requestMeta = metas.get(uri);

        String[] methods = requestMeta.getMethod();

        for (String requestMethod : methods) {
            if (method.equals(requestMethod))
                return true;
        }

        return false;
    }

    public static Schema getSchema(Method method) {
        Method m = methods.get(method);
        return cache.get(m);
    }

    public static String decode(String uri, Map<String, Object> parameters, String[] schemas) throws Exception {
        if (!isMapping(uri))
            return null;
        String json = null;
        if (schemas == null) {
            Method method = mapping.get(uri);
            Schema schema = cache.get(method);
            json = pojo(parameters, schema);
        } else if (schemas.length == 0) {
            List<Method> methods = defaultMapping.get(uri);
            if (methods.size() > 1)
                throw new RpcException("the method " + uri + " isn't just one implemented, please enter method parameter schema!!");
            Method method = methods.get(0);
            Schema schema = cache.get(method);
            json = pojo(parameters, schema);
        } else {
            List<Method> methods = defaultMapping.get(uri);
            List<RequestMeta> requestMetas = defaultMetas.get(uri);
            int count = 0;
            outer:
            for (int i = 0; i < methods.size(); i++) {
                Method method = methods.get(i);
                RequestMeta requestMeta = requestMetas.get(i);
                ParameterMeta[] parameterMetas = requestMeta.getParameterMetas();
                if (parameterMetas.length == schemas.length) {
                    inner:
                    for (int j = 0; j < parameterMetas.length; j++) {
                        ParameterMeta parameterMeta = parameterMetas[j];
                        String schema = schemas[j];
                        if (!parameterMeta.getParameterType().equals(schema)) count++;
                        break outer;
                    }
                    Schema schema = cache.get(method);
                    json = pojo(parameters, schema);
                    return json;
                }
            }

            throw new RpcException("the method " + uri + " is not implemented");

        }

        return json;
    }

    public static String decode(String uri, Map<String, Object> parameters) throws Exception {
        return decode(uri, parameters, null);
    }

    private static String pojo(Map<String, Object> parameters, Schema schema) throws Exception {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Map<String, ParameterMeta> parameterMetas = schema.getParameterMeta();
        String[] parameterTypes = new String[parameterMetas.size()];
        JSONObject result = new JSONObject();
        JSONArray args = new JSONArray();
        StringBuffer schemaJson = new StringBuffer("[");
        for (int i = 0; i < parameterMetas.size(); i++) {
            if (i > 0) {
                schemaJson.append(",");
            }
            schemaJson.append("%s");
        }
        schemaJson.append("]");

        for (String key : parameterMetas.keySet()) {
            ParameterMeta meta = parameterMetas.get(key);
            Object value = parameters.get(key);
            if (!isValid(key)) continue;
            parameterTypes[meta.getIndex()] = meta.getParameterType();
            if (value instanceof String) {
                String v = value.toString();
                if (v.startsWith("[")) {
                    if (meta.getType().equals("JSONArray"))
                        args.add(JSON.parseArray(v));
                    else if (meta.getType().equals("String"))
                        args.add(v);
                    else
                        throw new RpcException(String.format("%s is valid", key));
                } else if (v.startsWith("{")) {
                    if (meta.getType().equals("JSONString"))
                        args.add(JSON.parseObject(v));
                    else if (meta.getType().equals("String"))
                        args.add(v);
                    else
                        throw new RpcException(String.format("%s is valid", key));
                } else if (v.isEmpty()) args.add("");
                else if (v.matches("([0-9]+(.[0-9]+)*)"))
                    args.add(NumberFormat.getInstance().parse(v));
                else if (v.matches("(true|false)"))
                    args.add(Boolean.parseBoolean(v));
                else args.add(v);

            }

            if (value instanceof List) {
                if (!meta.getType().equals("JSONArray"))
                    throw new RpcException("parameter is valid");

                args.add(pojo((List<String>) value));
            }
        }

        String schemas = String.format(schemaJson.toString(), parameterTypes);

        result.put("interface", schema.getInterfaceName());
        result.put("method", schema.getMethodName());
        result.put("schema", schemas);
        result.put("args", args.toJSONString());

        return result.toJSONString();
    }

    private static List<Object> pojo(List<String> list) throws ParseException {
        List<Object> result = new ArrayList<Object>();
        for (String v : list) {
            if (v.startsWith("["))
                result.add(JSONArray.parse(v));
            else if (v.startsWith("{"))
                result.add(JSONObject.parse(v));
            else if (v.matches("([0-9]+(.[0-9]+)*)"))
                result.add(NumberFormat.getInstance().parse(v));
            else if (v.matches("(true|false)"))
                result.add(Boolean.parseBoolean(v));
            else
                result.add(v);
        }
        return result;
    }

    private static boolean isValid(String key) {
        return key.matches("^([a-zA-Z]|_)(\\w|_*)+");
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
//        map.put("person.name", "haoning");
//        map.put("student.name", "hujia");
//        map.put("person.age", "24");
//        map.put("person.father.name", "haoning1");
//        map.put("person.father.age", "25");
//        map.put("person.father1.name", "haoning1");
//        map.put("person.father1.age", "25");
//        map.put("student.friend.name", "hujia1");
//        map.put("student.friendAgesIsTure", "true");
//        map.put("student.friend.age", "24");
//        map.put("person.friendNames", "[\"zhangsan\",\"lisi\"]");
//        map.put("person.father.child.name", "haoning2");
//        map.put("person.father.child.age", "12");
//        map.put("a.d", "1");
//        map.put("a.e.c.d", "1");
//        map.put("a.c", "1");
//        map.put("a.b.d", "1");
//        map.put("a.b.e.f", "1");
//        map.put("a.b.e.f.d", "1");
//
//
//        Set<String> keySet = map.keySet();
//
//        List<String> keys = new ArrayList<String>(keySet);
//        Collections.sort(keys, new Comparator<String>() {
//            public int compare(String o1, String o2) {
//                return o1.compareToIgnoreCase(o2);
//            }
//        });
//        for (String key : keys) {
//            System.out.println("key is " + key);
//        }
//
//        StringBuffer sb = new StringBuffer("[");
//        Map<Integer, Arg> argMap = new HashMap<Integer, Arg>();
//
//        int nlen = 0, len = 0;
//        int i = 0, j = 0;
//        String[] next_propes = null;
//        String[] propes = null;
//        String key, next_key = null;
//        int size = keys.size();
//        out :
//        for (i = 0; i < size; i++) {
//            key = i == 0 ? keys.get(i) : next_key;
//            next_key = i == size - 1 ? null : keys.get(i + 1);
//            String value = map.get(key);
//            propes = i == 0 ? key.split("\\.") : next_propes;
//            next_propes = next_key == null ? null : next_key.split("\\.");
//            String pname = propes[0];
//            len = nlen == 0 ? propes.length : nlen;
//            nlen = i == size - 1 ? 0 : next_propes.length;
//            Arg arg = null;
//            if (argMap.isEmpty() || !pname.equals(argMap.get(0).getArg())) { // first
//                if (argMap.isEmpty()) sb.append("{");
//                arg = new Arg();
//                arg.setArg(pname);
//                arg.setLast(true);
//                argMap.put(0, arg);
//            } else {
//                if (arg.isLast()) {
//                    System.out.println("参数错误");
//                    return;
//                }
//            }
//
//
//            if (len == 2) {
//                sb.append("\"" + propes[1] + "\"");
//                sb.append(":");
//                join(sb, value);
//            }
//
//            if (len > 2) {
//                int brace = 0;
//                in:
//                for (j = 1; j <= len - 1; j++) {
//                    String p_index = propes[j];
//
//                    if (argMap.size() == j || !argMap.get(j).equals(p_index)) {
//                        if (argMap.size() == j) {
//                            sb.append("\"" + p_index + "\"");
//                        } else {
//
//                            sb.append("\"" + p_index + "\"");
//                        }
//                        if (j <= len - 1 && (next_propes == null ||  propes[0].equals(next_propes[0]))) {
//                            if ((len == nlen && !next_propes[j - 1].equals(propes[j - 1])) || len > nlen) {
//                                brace++;
//                            }
//                        }
//                        if (j < len - 1) {
//                            sb.append(":{");
//                        } else
//                            sb.append(":");
//                        if (!argMap.containsKey(j)) {
//                            arg = new Arg();
//                            argMap.put(j, arg);
//                        } else {
//                            arg = argMap.get(j);
//                        }
//
//                        arg.setArg(p_index);
//                        arg.setLast(false);
//
//                    }
//                }
//
//                join(sb, value);
//                for (j = 0; j < brace; j++)
//                    sb.append("}");
//
//            }
//            if (i < size - 1)
//                if (propes[0].equals(next_propes[0]))
//                    sb.append(",");
//                else
//                    sb.append("},{");
//
//        }
//        sb.append("]");
//        System.out.println(sb.toString());

        String regex = "^([a-zA-Z]|_)(\\w|_*)+";

        System.out.println("a".matches(regex));
        System.out.println("a1".matches(regex));
        System.out.println("a_1".matches(regex));
        System.out.println("a_".matches(regex));
        System.out.println("_a".matches(regex));

        System.out.println("1_a".matches(regex));
        System.out.println("!_a".matches(regex));
    }

    public static class Arg {
        private String arg;
        private boolean isLast;

        public String getArg() {
            return arg;
        }

        public void setArg(String arg) {
            this.arg = arg;
        }

        public boolean isLast() {
            return isLast;
        }

        public void setLast(boolean last) {
            isLast = last;
        }
    }

    private static void join(StringBuffer sb, String value) {
        if (value.startsWith("[") || value.startsWith("{")) {
            sb.append(value);
        } else {
            sb.append("\"" + value + "\"");
        }
    }

    public static String[] getParameters(Method method) {
        return discoverer.getParameterNames(method);
    }

}

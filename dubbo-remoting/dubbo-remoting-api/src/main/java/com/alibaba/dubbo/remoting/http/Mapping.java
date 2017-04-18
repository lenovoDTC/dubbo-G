package com.alibaba.dubbo.remoting.http;

import com.alibaba.dubbo.common.json.*;
import org.omg.Dynamic.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by haoning1 on 2017/4/6.
 */
public class Mapping {
    private static LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();
    public static Map<Method, Schema> cache = new LinkedHashMap<Method, Schema>();
    public static Map<String, Method> mapping = new LinkedHashMap<String, Method>();
    public static Map<String, RequestMeta> metas = new LinkedHashMap<String, RequestMeta>();
//    public static LinkedHashMap<Method, ParameterMeta[]> params;

    public static void push(Method method) {
        String[] parameterNames = discoverer.getParameterNames(method);
        Type[] types = method.getGenericParameterTypes();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<String, ParameterMeta> parameterMeta = new HashMap<String, ParameterMeta>();

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
            //parameter.setGenericType(types[i].toString());
            parameterMeta.put(parameterName, parameter);

        }

        Schema schema = new Schema();
        schema.setMethodName(method.getName());
        schema.setParameterMeta(parameterMeta);
        push(method, schema);

    }

    private static String getType (String parameterType) {
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
        if (parameterType.startsWith("[L") || parameterType.endsWith("List") || parameterType.endsWith("Set")) return "JSONArray";
        return "JSONString";


    }

    public static void push(Method method, Schema schema) {
        cache.put(method, schema);
    }

    public static void push(Method method, String uri, RequestMeta requestMeta) {
        mapping.put(uri, method);
        metas.put(uri, requestMeta);
    }

    public static boolean isMapping(String uri) {
        return mapping.containsValue(uri);
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

    public static String decode(String uri, Map<String, Object> parameters) throws Exception{
        if (!isMapping(uri))
            return null;

        Method method = mapping.get(uri);
        Schema schema = cache.get(method);

        String json = pojo(parameters, schema);
        return json;
    }

    private static String pojo(Map<String, Object> parameters, Schema schema) throws Exception{
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        Map<String, ParameterMeta> parameterMetas = schema.getParameterMeta();
        String[] parameterValues = new String[parameterMetas.size()];
        StringBuffer json = new StringBuffer("[");
        for (int i = 0; i < parameterMetas.size(); i++) {
            if (i > 0)
                json.append(",");
            json.append("%s");
        }
        json.append("]");
        for (String key : parameterMetas.keySet()) {
            ParameterMeta meta = parameterMetas.get(key);
            Object value = parameters.get(key);
            if (!isValid(key)) continue;
            if (value instanceof String) {
                String v = value.toString();
                if (v.startsWith("[")) {
                    if (meta.getType().equals("JSONArray"))
                        parameterValues[meta.getIndex()] = v;
                    else
                        throw new Exception(String.format("%s is valid", key));
                }
                else if (v.startsWith("{")) {
                    if (meta.getType().equals("JSONString"))
                        parameterValues[meta.getIndex()] = v;
                    else
                        throw new Exception(String.format("%s is valid", key));
                }

                else if (v.isEmpty()) parameterValues[meta.getIndex()] = "\"\"";
                else if (v.matches("([0-9]+|true|false)")) parameterValues[meta.getIndex()] = v;
                else parameterValues[meta.getIndex()] = "\"" + v + "\"";

            }

            if (value instanceof List) {
                if (!meta.getType().equals("JSONArray"))
                    throw new Exception("parameter is valid");


            }
        }

        return String.format(json.toString(), parameterValues);
    }

    private static String pojo (List<String> list) {
        StringBuffer json = new StringBuffer("[");
        int i = 0;
        for (String v : list) {
            if (i > 0)
                json.append(",");
            if (v.startsWith("[") || v.startsWith("{") || v.matches("([0-9]+|true|false)"))
                json.append(v);
            else
                json.append("\"" + v + "\"");
        }
        json.append("]");
        return json.toString();
    }

    private static boolean isValid (String key) {
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

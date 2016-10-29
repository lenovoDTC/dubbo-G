package com.alibaba.dubbo.proxy;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

public class JavaCodeCompile {

	private static Logger log = LoggerFactory.getLogger(JavaCodeCompile.class);

	public static final String tempOutputFolder = "/tmp";

	public static Class<?> compile(String javaCode, String className)
			throws ClassNotFoundException, MalformedURLException {
		String classOutputFolder = tempOutputFolder;
		// 通过 ToolProvider 取得 JavaCompiler 对象，JavaCompiler 对象是动态编译工具的主要对象
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		// 通过 JavaCompiler 取得标准 StandardJavaFileManager
		// 对象，StandardJavaFileManager 对象主要负责
		// 编译文件对象的创建，编译的参数等等，我们只对它做些基本设置比如编译 CLASSPATH 等。
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		// 因为是从内存中读取 Java 源文件，所以需要创建我们的自己的 JavaFileObject，即
		// InMemoryJavaFileObject
		JavaFileObject fileObject = new InMemoryJavaFileObject(className, javaCode);
		Iterable<? extends JavaFileObject> files = Arrays.asList(fileObject);

		// 编译结果信息的记录
		StringWriter sw = new StringWriter();

		// 编译目的地设置
		Iterable<String> options = Arrays.asList("-d", classOutputFolder);

		// 通过 JavaCompiler 对象取得编译 Task
		JavaCompiler.CompilationTask task = compiler.getTask(sw, fileManager, null, options, null, files);

		boolean call = task.call();
		System.out.println(call);

		// 调用 call 命令执行编译，如果不成功输出错误信息
		if (!call) {
			String failedMsg = sw.toString();
			log.error(className + "java code compile fail, return message is " + failedMsg);
			throw new IllegalStateException(className + " java code compile fail !!");
		}

		URL[] urls = new URL[] { new URL("file://" + tempOutputFolder + File.separator) };
		URLClassLoader classLoader = new URLClassLoader(urls);
		return classLoader.loadClass(className);
		// 自定义 JavaFileObject 实现了 SimpleJavaFileObject，指定 string 为 java
		// 源代码，这样就不用将源代码
		// 存在内存中，直接从变量

	}

	// 自定义 JavaFileObject 实现了 SimpleJavaFileObject，指定 string 为 java
	// 源代码，这样就不用将源代码
	// 存在内存中，直接从变量中读入即可。
	public static class InMemoryJavaFileObject extends SimpleJavaFileObject {
		private String contents = null;

		public InMemoryJavaFileObject(String className, String contents) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.contents = contents;
		}

		public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
			return contents;
		}
	}

	public static void main(String[] args) {
		String javaCode = "package com.lenovo.api; \n public class Hello {public String sayHello(String name){return name;}}";
		try {
			Class<?> className = JavaCodeCompile.compile(javaCode, "com.lenovo.api.Hello");
			System.out.println(className.getCanonicalName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

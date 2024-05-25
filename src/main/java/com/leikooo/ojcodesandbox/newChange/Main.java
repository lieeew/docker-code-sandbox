package com.leikooo.ojcodesandbox.newChange;

import cn.hutool.core.io.FileUtil;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        String sourceFilePath = "src/main/resources/code/Solution.java";
        String classDir = "src/main/resources/code";
        String className = "code.Solution";
        String methodName = "solution";
        Class<?>[] paramTypes = {int.class};
        Object[] methodParams = {5};

        try {
            // 编译 Java 代码
            boolean isCompiled = compile(sourceFilePath);
            if (isCompiled) {
                // 加载类
                Class<?> cls = loadClass(classDir, className);
                // 创建类的实例
                Object instance = cls.getDeclaredConstructor().newInstance();
                // 调用方法
//                Object result = invokeMethod(cls, methodName, paramTypes, instance, methodParams);
//                System.out.println("Result: " + result);
            } else {
                System.out.println("Compilation failed.");
            }
        } catch (IOException | ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static boolean compile(String filePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, filePath);
        return result == 0;

    }

    private static Class<?> loadClass(String classDir, String className) throws MalformedURLException, ClassNotFoundException {
        URL[] urls = {new URL("file:/" + classDir)};
        URLClassLoader classLoader = new URLClassLoader(urls);
        return classLoader.loadClass(className);
    }


    public static void saveCodeToFile(String code, String path) {
        FileUtil.writeString(code, path, StandardCharsets.UTF_8);
    }

    public static boolean compileCode(String filePath) throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec("javac -encoding utf-8 " + filePath);
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    public static void executeCode(String classDir, String methodName, Object... params) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, MalformedURLException {
        // Convert the class directory to a URL
        File file = new File(classDir);
        URL url = file.toURI().toURL();
        URL[] urls = new URL[]{url};

        // Create a new class loader with the directory
        URLClassLoader classLoader = new URLClassLoader(urls);

        // Load the class
        Class<?> cls = classLoader.loadClass("Solution");

        // Find the method by name and parameter types
        Method method = cls.getMethod(methodName, new Class[]{int.class});

        // Create an instance of the class
        Object instance = cls.getDeclaredConstructor().newInstance();

        // Invoke the method on the instance
        Object result = method.invoke(instance, params);
        System.out.println("Result: " + result);
    }
}

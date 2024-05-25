package com.yupi.yuojcodesandbox.newChange;

// 默认引入一般常用的包

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/16
 * @description arg[0] 传方法名称 arg[1]... 之后传对应的函数参数 格式 数字:数据
 * 格式说明:
 * "1:[1,2,3,4]" - 1 代表数组类型（int数组）
 * "2:hello" - 2 代表字符串类型
 * "3:[1.1,2.2]" - 3 代表double数组
 * "4:[1.1,2.2]" - 4 代表float数组
 * "5:[true,false]" - 5 代表boolean数组
 * "6:[a,b,c]" - 6 代表char数组
 * "7:[1,2,3]" - 7 代表long数组
 * "8:[1,2,3]" - 8 代表short数组
 * "9:[1,2,3]" - 9 代表byte数组
 * "10:42" - 10 代表int
 * "11:3.14" - 11 代表double
 * "12:2.72" - 12 代表float
 * "13:true" - 13 代表boolean
 * "14:a" - 14 代表char
 * "15:123456789" - 15 代表long
 * "16:12345" - 16 代表short
 * "17:127" - 17 代表byte
 */
public class MainSolution {


    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        String code = getStringCode(new File("Solution.java"));
//        String code = "import java.util.*; \n class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        if (s.length()==0) return 0;\n        HashMap<Character, Integer> map = new HashMap<Character, Integer>();\n        int max = 0;\n        int left = 0;\n        for(int i = 0; i < s.length(); i ++){\n            if(map.containsKey(s.charAt(i))){\n                left = Math.max(left,map.get(s.charAt(i)) + 1);\n            }\n            map.put(s.charAt(i),i);\n            max = Math.max(max,i-left+1);\n        }\n        return max;\n        \n    }\n}";
//        args = new String[]{"lengthOfLongestSubstring", "2:bbbbb"};
        (new MainSolution()).invokeMethod(code, args);
    }

    public static String getStringCode(File file) throws IOException {
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        if (length >= Integer.MAX_VALUE) {
            throw new RuntimeException("File is larger then max array size");
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()));
        bufferedInputStream.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void invokeMethod(String code, String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Solution solution = new Solution();
        Class<? extends Solution> aClass = solution.getClass();
        List<String> methodTypes = getParameterTypesFromCode(args[0], code);
        int parameterNum = methodTypes.size();

        // 获取方法参数类型数组
        Class<?>[] parameterTypes = new Class<?>[parameterNum];
        Object[] methodArgs = new Object[parameterNum];

        for (int i = 0; i < parameterNum; i++) {
            parameterTypes[i] = getClassByString(methodTypes.get(i));
            methodArgs[i] = getMethodArgsByString(args[i + 1]);
        }

        // 获取方法并调用
        Method method = aClass.getDeclaredMethod(args[0], parameterTypes);
        Object result = method.invoke(solution, methodArgs);

        printResult(result);   }

    private void printResult(Object result) {
        if (result instanceof byte[]) {
            System.out.println(Arrays.toString((byte[]) result));
        } else if (result instanceof int[]) {
            System.out.println(Arrays.toString((int[]) result));
        } else if (result instanceof double[]) {
            System.out.println(Arrays.toString((double[]) result));
        } else if (result instanceof float[]) {
            System.out.println(Arrays.toString((float[]) result));
        } else if (result instanceof long[]) {
            System.out.println(Arrays.toString((long[]) result));
        } else if (result instanceof short[]) {
            System.out.println(Arrays.toString((short[]) result));
        } else if (result instanceof char[]) {
            System.out.println(Arrays.toString((char[]) result));
        } else if (result instanceof boolean[]) {
            System.out.println(Arrays.toString((boolean[]) result));
        } else if (result instanceof String[]) {
            for (int i = 0; i < ((String[]) result).length; i++) {
                System.out.println(((String[]) result)[i]);
            }
        } else {
            System.out.println(result);
        }
    }

    /**
     * 解析字符串参数，将其转换为相应的对象
     * <p>
     * 格式说明:
     * "1:[1,2,3,4]" - 1 代表数组类型（int数组）
     * "2:hello" - 2 代表字符串类型
     * "3:[1.1,2.2]" - 3 代表double数组
     * "4:[1.1,2.2]" - 4 代表float数组
     * "5:[true,false]" - 5 代表boolean数组
     * "6:[a,b,c]" - 6 代表char数组
     * "7:[1,2,3]" - 7 代表long数组
     * "8:[1,2,3]" - 8 代表short数组
     * "9:[1,2,3]" - 9 代表byte数组
     * "10:42" - 10 代表int
     * "11:3.14" - 11 代表double
     * "12:2.72" - 12 代表float
     * "13:true" - 13 代表boolean
     * "14:a" - 14 代表char
     * "15:123456789" - 15 代表long
     * "16:12345" - 16 代表short
     * "17:127" - 17 代表byte
     *
     * @param args arg 字符串参数
     * @return 转化后的对象
     */
    private Object getMethodArgsByString(String args) {
        // 分割前缀和内容
        String[] parts = args.split(":", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid argument format: " + args);
        }

        String prefix = parts[0];
        String content = parts[1];

        if (StringUtils.isAllBlank(prefix, content)) {
            throw new IllegalArgumentException("Argument should not be null");
        }

        switch (prefix) {
            case "1": // int array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .mapToInt(Integer::parseInt)
                        .toArray();
            case "2": // String
                return content;
            case "3": // double array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .mapToDouble(Double::parseDouble)
                        .toArray();
            case "4": // float array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .map(Float::parseFloat)
                        .toArray();
            case "5": // boolean array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .map(Boolean::parseBoolean)
                        .toArray();
            case "6": // char array
                return content.substring(1, content.length() - 1).replace(",", "").toCharArray();
            case "7": // long array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .mapToLong(Long::parseLong)
                        .toArray();
            case "8": // short array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .map(Short::parseShort)
                        .toArray(Short[]::new);
            case "9": // byte array
                return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                        .map(Byte::parseByte)
                        .toArray(Byte[]::new);
            case "10": // int
                return Integer.parseInt(content);
            case "11": // double
                return Double.parseDouble(content);
            case "12": // float
                return Float.parseFloat(content);
            case "13": // boolean
                return Boolean.parseBoolean(content);
            case "14": // char
                return content.charAt(0);
            case "15": // long
                return Long.parseLong(content);
            case "16": // short
                return Short.parseShort(content);
            case "17": // byte
                return Byte.parseByte(content);
            case "18": // String[]
                // ['a','b','c','d']
                return content.replace("'", "\"").split(",");
            default:
                throw new IllegalArgumentException("Unknown prefix: " + prefix);
        }
    }

    public Class<?> getClassByString(String stringClassName) {
        switch (stringClassName) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
            case "byte[]":
                return byte[].class;
            case "short[]":
                return short[].class;
            case "int[]":
                return int[].class;
            case "long[]":
                return long[].class;
            case "float[]":
                return float[].class;
            case "double[]":
                return double[].class;
            case "boolean[]":
                return boolean[].class;
            case "char[]":
                return char[].class;
            case "String":
                return String.class;
            default:
                throw new UnsupportedOperationException("no such type!");
        }
    }

    private List<String> getParameterTypesFromCode(String methodName, String codeContent) {
        // 定义方法名称和参数类型的正则表达式
        String methodPattern = "\\b" + methodName + "\\s*\\(\\s*([^)]*)\\s*\\)";

        // 匹配 Java 代码中的指定方法
        Pattern pattern = Pattern.compile(methodPattern);
        Matcher matcher = pattern.matcher(codeContent);

        // 输出匹配结果
        if (matcher.find()) {
            List<String> parameterTypes = new ArrayList<>();
            String parameterTypesString = matcher.group(1);
            Pattern typePattern = Pattern.compile("@?\\w+(?:\\[\\])?(?=\\s+\\w+)");
            Matcher typeMatcher = typePattern.matcher(parameterTypesString);
            while (typeMatcher.find()) {
                String typeStr = typeMatcher.group();
                // 过滤所有注解，如@NotNull
                if (!typeStr.startsWith("@")) {
                    parameterTypes.add(typeStr);
                }
            }
            return parameterTypes;
        } else {
            return new ArrayList<>();
        }
    }
}

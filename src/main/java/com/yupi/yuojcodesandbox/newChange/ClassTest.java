package com.yupi.yuojcodesandbox.newChange;

import cn.hutool.core.util.ArrayUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/22
 * @description
 */
public class ClassTest {
    public static void main(String[] args) {
        List<String> inputList = Arrays.asList("sumTwo", "1:[1,2,3,4]", "10:3");
        String inputArgsArray = "'" + String.join("' '", inputList) + "'";
//        String[] javacSolution = ArrayUtil.append(new String[]{"javac", "-encoding", "utf-8", "/app/Solution.java"});
//        String[] javacRunMain = ArrayUtil.append(new String[]{"javac", "-encoding", "utf-8", "/app/MainSolution.java"});
        // "java", "-Dfile.encoding=UTF-8", "-cp", "/app", "Solution", "twoSum" , "1:[1,2,3,4]" ,"10:3"]
        // 合并命令为一个单一的命令字符串
        String combinedCommand = String.join(" && ",
                "javac -encoding utf-8 /app/Solution.java",
                "javac -encoding utf-8 /app/MainSolution.java",
                "java -Dfile.encoding=UTF-8 -cp /app MainSolution " + inputArgsArray
        );
        System.out.println("combinedCommand = " + combinedCommand);
    }
}

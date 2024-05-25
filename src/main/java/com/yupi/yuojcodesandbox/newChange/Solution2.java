package com.yupi.yuojcodesandbox.newChange;

import cn.hutool.core.io.FileUtil;
import lombok.Data;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/17
 * @description
 */
public class Solution2 {
    @Test
    public void test() throws FileNotFoundException {
        String code =
                "import java.util.HashMap;\n" +
                        "import java.util.Map;\n" +
                        "\n" +
                        "class Solution {\n" +
                        "    public int[] twoSum(int[] nums, int target) {\n" +
                        "        Map<Integer, Integer> hashtable = new HashMap<Integer, Integer>();\n" +
                        "        for (int i = 0; i < nums.length; ++i) {\n" +
                        "            if (hashtable.containsKey(target - nums[i])) {\n" +
                        "                return new int[]{hashtable.get(target - nums[i]), i};\n" +
                        "            }\n" +
                        "            hashtable.put(nums[i], i);\n" +
                        "        }\n" +
                        "        return new int[0];\n" +
                        "    }\n" +
                        "}";
        System.out.println("code = " + code);
        String templateCode = FileUtil.readString(new File("E:\\yuoj-code-sandbox-master\\src\\main\\java\\com\\yupi\\yuojcodesandbox\\template\\template.txt"), StandardCharsets.UTF_8);
        System.out.println(String.format(templateCode, getCode(code).getMethod()));
    }

    private ExecuteCodeAndImport getCode(String code) {
        ExecuteCodeAndImport executeCodeAndImport = new ExecuteCodeAndImport();
        // 匹配 import 语句
        Pattern importPattern = Pattern.compile("import\\s+(?:static\\s+)?([\\w.]+)(?:\\.\\*)?;");
        Matcher importMatcher = importPattern.matcher(code);
        while (importMatcher.find()) {
            executeCodeAndImport.getImports().add(importMatcher.group(1));
        }
        // 匹配 class 对象里面所有的函数
        importPattern = Pattern.compile("(?s)class\\s+Solution\\s*\\{(.*?)\\}$");
        importMatcher = importPattern.matcher(code);
        while (importMatcher.find()) {
            executeCodeAndImport.setMethod(importMatcher.group(1));
        }
        return executeCodeAndImport;
    }
}

@Data
class ExecuteCodeAndImport {
    private List<String> imports = new ArrayList<>();

    private String method;
}
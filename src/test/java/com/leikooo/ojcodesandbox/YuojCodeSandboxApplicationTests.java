package com.leikooo.ojcodesandbox;

import com.github.dockerjava.core.LocalDirectorySSLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.SSLContext;

@SpringBootTest
class YuojCodeSandboxApplicationTests {

    @Test
    void contextLoads() {
        LocalDirectorySSLConfig classpath = new LocalDirectorySSLConfig("classpath://");
        SSLContext sslContext = classpath.getSSLContext();
        System.out.println("sslContext = " + sslContext);
    }

    @Test
    void test() {
        System.out.println("public class Main {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int sum = a + b;\n" +
                "        System.out.println(sum);\n" +
                "    }\n" +
                "}\n");
    }

    public static void main(String[] args) {
        args = new String[]{"1", "2"};
    }
}

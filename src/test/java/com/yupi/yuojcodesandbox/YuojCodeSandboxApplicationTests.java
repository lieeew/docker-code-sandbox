package com.yupi.yuojcodesandbox;

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

}

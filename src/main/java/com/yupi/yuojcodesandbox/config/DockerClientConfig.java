package com.yupi.yuojcodesandbox.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.yupi.yuojcodesandbox.utils.ResourceExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/16
 * @description
 */
@Configuration
public class DockerClientConfig {

    @Bean
    public DockerClient dockerClient() throws IOException {
        com.github.dockerjava.core.DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://127.0.0.1:2376")
                .withDockerTlsVerify(false)
//                .withRegistryPassword("Ee719963000")
//                .withRegistryUsername("leikooo")
//                .withRegistryEmail("liangzilixue12345@gmail.com")
//                .withDockerCertPath(ResourceExtractor.extractResourceDirectory("ca").getPath())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
//                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}

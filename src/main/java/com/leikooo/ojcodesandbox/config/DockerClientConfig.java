package com.leikooo.ojcodesandbox.config;

import cn.hutool.core.io.FileUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.leikooo.ojcodesandbox.utils.ResourceExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.File;
import java.time.Duration;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/16
 * @description
 */
@Configuration
public class DockerClientConfig {
    @Value("${docker.host}")
    private String dockerHost;

    @Value("${docker.registry.username}")
    private String userName;

    @Value("${docker.registry.email}")
    private String email;

    @Value("${docker.registry.password}")
    private String password;

    @Value("${docker.tls_verify}")
    private boolean tlsVerify;

    private final File file = ResourceExtractor.extractResourceDirectory("ca");

    @Bean
    public DockerClient dockerClient() {
        com.github.dockerjava.core.DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(tlsVerify)
                .withRegistryPassword(password)
                .withRegistryUsername(userName)
                .withRegistryEmail(email)
                .withDockerCertPath(file.getPath())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * 优雅停机，关闭之前删除临时 ca 文件
     */
    @PreDestroy
    public void delCaFolder() {
        FileUtil.del(file);
    }
}

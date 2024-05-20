package com.yupi.yuojcodesandbox.docker;

import cn.hutool.core.util.ClassLoaderUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.Duration;
import java.util.List;

@Slf4j
public class DockerDemo {

    public static void main(String[] args) throws InterruptedException {
        URL url = ClassLoaderUtil.getClassLoader().getResource("ca");
        // 获取默认的 Docker Client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://123.57.52.207:2376")
                .withDockerTlsVerify(true)
                .withRegistryPassword("Ee719963000")
                .withRegistryUsername("leikooo")
                .withRegistryEmail("liangzilixue12345@gmail.com")
                .withDockerCertPath(url.getPath().substring(1))
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
//        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();
////         拉取镜像
//        String image = "openjdk:8-alpine";
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像：" + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd
//                .exec(pullImageResultCallback)
//                .awaitCompletion();
//        System.out.println("下载完成");
//        // 创建容器
//        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
//        CreateContainerResponse createContainerResponse = containerCmd
////                .withCmd("echo", "Hello Docker")
//                .exec();
//        System.out.println(createContainerResponse);
//        String containerId = createContainerResponse.getId();
//        System.out.println("containerId = " + containerId);
////        // 查看容器状态
//        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
//        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
//        for (Container container : containerList) {
//            System.out.println(container);
//        }
//
////        // 启动容器
//        dockerClient.startContainerCmd(containerId).exec();

//        Thread.sleep(5000L);

        dockerClient.logContainerCmd("fcfb59ff4138")
                .withStdErr(true)
                .withStdOut(true)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame item) {
                        log.info("streamType {}  log {}", item.getStreamType(), new String(item.getPayload()));
                        super.onNext(item);
                    }
                })
                .awaitCompletion();
//
//        // 删除容器
//        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
//
//        // 删除镜像
//        dockerClient.removeImageCmd(image).exec();
    }
}

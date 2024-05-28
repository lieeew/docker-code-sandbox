package com.leikooo.ojcodesandbox.event.lisener;

import com.github.dockerjava.api.DockerClient;
import com.leikooo.ojcodesandbox.event.ContainerDeleteEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/22
 * @description 运行之后删除 container
 */
@Slf4j
@Component
public class DockerContainerDeleteListener {
    @Resource
    private DockerClient dockerClient;

    @EventListener(classes = ContainerDeleteEvent.class)
    public void deleteContainer(ContainerDeleteEvent containerDeleteEvent) {
        String containerId = containerDeleteEvent.getContainerId();
        log.warn("DockerContainerDeleteListener#deleteContainer container delete id = {}", containerId);
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
    }
}

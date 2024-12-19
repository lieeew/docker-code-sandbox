package com.leikooo.ojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.InvocationBuilder;
import com.leikooo.ojcodesandbox.event.ContainerDeleteEvent;
import com.leikooo.ojcodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author <a href="https://www.github.com/lieeew">leikooo</a>
 */
@Slf4j
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    public static final String IMAGE_NAME = "openjdk:8-alpine";

    private static final long TIME_OUT = 10L;

    private static final long COMPILE_TIME_OUT = 5L;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private DockerClient dockerClient;

    @PostConstruct
    private void downloadImage() {
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(IMAGE_NAME);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                log.info("下载镜像状态 {} ", item.getStatus());
                super.onNext(item);
            }
        };
        try {
            pullImageCmd
                    .exec(pullImageResultCallback)
                    .awaitCompletion();
        } catch (InterruptedException e) {
            log.error("拉取镜像异常 {}", ExceptionUtils.getStackTrace(e));
        }
        log.info("下载 {} 完成", IMAGE_NAME);
    }

    /**
     * 3、创建容器，把文件复制到容器内
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String containerId = createContainerAndGetResponse();
        copyFileToContainer(containerId, userCodeFile);
        // 执行命令并获取结果
        // docker exec ....
        try {
            compileCode(containerId);
            return runCompiledCode(containerId, inputList);
        } catch (Exception e) {
            log.error("runFile error {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        } finally {
             applicationEventPublisher.publishEvent(new ContainerDeleteEvent(this, containerId));
        }
    }

    private List<ExecuteMessage> runCompiledCode(String containerId, List<String> inputList) {
        String command = String.join(" && ",
                "cd /root/app",
                " java -Dfile.encoding=UTF-8 -cp . Main " + inputList
        );
        String[] commands = {"sh", "-c", command};
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(commands)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        ExecuteMessage executeResult = getExecuteResult(execCreateCmdResponse.getId(), containerId);
        log.error("ExecuteMessageResult: {}", executeResult);
        return Collections.singletonList(executeResult);
    }

    private void compileCode(String containerId) {
        String combinedCommand = String.join(" && ",
                " cd /root/app",
                " javac -encoding utf-8 Main.java "
        );
        String[] commands = {"sh", "-c", combinedCommand};
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(commands)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        try {
            dockerClient.execStartCmd(execCreateCmdResponse.getId())
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onComplete() {
                            log.info("CompileCode already !");
                            super.onComplete();
                        }
                    }).awaitCompletion(COMPILE_TIME_OUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("compileCode error {}", ExceptionUtils.getStackTrace(e));
        }
    }

    private void copyFileToContainer(String containerId, File userCodeFile) {
        log.info("userCodeFile = {}", userCodeFile);
        dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath("/root").withHostResource(userCodeFile.getAbsolutePath()).exec();
    }

    private String createContainerAndGetResponse() {
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_NAME);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        // hostConfig.withSecurityOpts(Arrays.asList("seccomp=leikoooDockerClient"));
        // hostConfig.setBinds(new Bind(userCodeFile.getPath(), new Volume("/app")));
        String containerId = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec().getId();
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    @Override
    public ExecuteMessage compileFile(File userCodeFile) {
        return null;
    }

    public ExecuteMessage getExecuteResult(String executeId, String containerId) {
        log.info("getExecuteResult containerId {}", executeId);
        StopWatch stopWatch = new StopWatch();
        final String[] message = new String[30];
        final String[] errorMessage = new String[30];
        final boolean[] timeout = {true};
        long time = 0;
        // 获取占用的内存
        final long[] maxMemory = {0L};

        try {
            stopWatch.start();
            dockerClient.execStartCmd(executeId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onComplete() {
                            // 如果执行完成，则表示没超时
                            timeout[0] = false;
                            super.onComplete();
                        }

                        @Override
                        public void onNext(Frame frame) {
                            log.info("getExecuteResult#onNext method");
                            StreamType streamType = frame.getStreamType();
                            if (StreamType.STDERR.equals(streamType)) {
                                errorMessage[0] = new String(frame.getPayload());
                                System.out.println("输出错误结果：" + errorMessage[0]);
                            } else {
                                message[0] = new String(frame.getPayload());
                                System.out.println("输出结果：" + message[0]);
                            }
                            super.onNext(frame);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.error(ExceptionUtils.getRootCauseMessage(throwable));
                        }
                    })
                    .awaitCompletion(TIME_OUT, TimeUnit.MINUTES);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
            getRunStatistics(containerId, maxMemory);
        } catch (InterruptedException e) {
            log.error("程序执行异常: {}", ExceptionUtils.getStackTrace(e));
        }
        return ExecuteMessage.builder()
                .errorMessage(Arrays.stream(errorMessage).filter(Objects::nonNull).collect(Collectors.joining(" ")))
                .memory(maxMemory[0])
                .message(Arrays.stream(message).filter(Objects::nonNull).collect(Collectors.joining(" ")))
                .time(time)
                .build();
    }

    private void getRunStatistics(String containerId, long[] maxMemory) {
        log.info("start to get container usage {}", containerId);
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        dockerClient.statsCmd(containerId).exec(callback);
        Statistics stats;
        try {
            stats = callback.awaitResult();
            maxMemory[0] = stats.getMemoryStats().getUsage() >= maxMemory[0] ? stats.getMemoryStats().getUsage() : maxMemory[0];
            callback.close();
        } catch (RuntimeException | IOException e) {
            log.error("getStatisticsError {}", ExceptionUtils.getStackTrace(e));
        }
    }

}




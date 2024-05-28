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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
        CreateContainerResponse createContainerResponse = createContainerAndGetResponse(userCodeFile);
        String containerId = createContainerResponse.getId();
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
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
        return inputList.stream().map(input -> {
            // "groupAnagrams 18:[eat,tea,tan,ate,nat,bat]"
            String[] argList = input.split(" ");
            String inputArgsArray = "'" + String.join("' '", argList) + "'";
            // 合并命令为一个单一的命令字符串
            String combinedCommand = String.join("&&",
                    " cd /app ",
                    " java -Dfile.encoding=UTF-8 -cp . MainSolution " + inputArgsArray
            );
            String[] commands = {"sh", "-c", combinedCommand};
            log.info("commands: {}", String.join(" ", commands));
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(commands)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            ExecuteMessage executeResult = getExecuteResult(execCreateCmdResponse.getId(), containerId);
            log.info("ExecuteMessageResult: {}", executeResult);
            return executeResult;
        }).collect(Collectors.toList());
    }

    private void compileCode(String containerId) {
        String combinedCommand = String.join("&&",
                " cd /app ",
                " javac -encoding utf-8 Solution.java ",
                " javac -encoding utf-8 MainSolution.java "
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

    private CreateContainerResponse createContainerAndGetResponse(File userCodeFile) {
        log.info("userCodeFile = {}", userCodeFile);
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_NAME);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=leikoooDockerClient"));
        hostConfig.setBinds(new Bind(userCodeFile.getPath(), new Volume("/app")));
        return containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
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
        // index 索引
        final int[] messageIndex = {0};
        final int[] errorMessageIndex = {0};
        long time = 0L;
        // 判断是否超时
        final boolean[] isTimeOut = {true};
        // 获取占用的内存
        final long[] maxMemory = {0L};

        try {
            stopWatch.start();
            dockerClient.execStartCmd(executeId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onComplete() {
                            // 如果执行完成，则表示没超时
                            log.info("getExecuteResult#onComplete be invoke");
                            isTimeOut[0] = false;
                            super.onComplete();
                        }

                        @Override
                        public void onNext(Frame frame) {
                            if (messageIndex[0] > 30 || errorMessageIndex[0] > 30) {
                                // 超过了直接调用 onComplete
                                super.onComplete();
                            }
                            log.info("getExecuteResult#onNext be invoke");
                            StreamType streamType = frame.getStreamType();
                            if (StreamType.STDERR.equals(streamType)) {
                                errorMessage[errorMessageIndex[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
                            } else {
                                message[messageIndex[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
                            }
                            super.onNext(frame);
                        }
                    })
                    .awaitCompletion(TIME_OUT, TimeUnit.SECONDS);
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




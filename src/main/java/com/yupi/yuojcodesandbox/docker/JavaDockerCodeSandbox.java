package com.yupi.yuojcodesandbox.docker;

import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.yupi.yuojcodesandbox.event.ContainerDeleteEvent;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import com.yupi.yuojcodesandbox.model.ExitValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.swing.event.DocumentEvent;
import java.io.Closeable;
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
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        // todo 修改 参考 Solution 文档
        String inputArgsArray = "'" + String.join("' '", inputList) + "'";
        // 合并命令为一个单一的命令字符串
        String combinedCommand = String.join(" && ",
                "cd /app ",
                "javac -encoding utf-8 /app/Solution.java",
                "javac -encoding utf-8 /app/MainSolution.java",
                "java -Dfile.encoding=UTF-8 -cp /app MainSolution " + inputArgsArray
        );
        String[] commands = {"sh", "-c", combinedCommand};
        log.info("commands: {}", String.join(" ", commands));
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(commands)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        log.info("创建执行命令: {}", execCreateCmdResponse);
        ExecuteMessage executeResult = getExecuteResult(containerId, execCreateCmdResponse);
        log.info("ExecuteMessageResult: {}", executeResult);
        executeMessageList.add(executeResult);
        applicationEventPublisher.publishEvent(new ContainerDeleteEvent(this, containerId));
        return executeMessageList;
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

    private ExecuteMessage getExecuteResult(String containerId, ExecCreateCmdResponse execCreateCmdResponse) {
        StopWatch stopWatch = new StopWatch();
        final String[] message = new String[30];
        final String[] errorMessage = new String[30];
        final int[] messageIndex = {0};
        final int[] errorMessageIndex = {0};
        long time = 0L;
        // 判断是否超时
        final boolean[] isTimeOut = {true};
        String execId = execCreateCmdResponse.getId();

        final long[] maxMemory = {0L};

        // 获取占用的内存
        try (StatsCmd statsCmd = getRunStatistics(containerId, maxMemory)) {
            stopWatch.start();
            dockerClient.logContainerCmd(execId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTail(30)
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
                            log.info("getExecuteResult#onNext be invoke");
                            StreamType streamType = frame.getStreamType();
                            if (StreamType.STDERR.equals(streamType)) {
                                errorMessage[errorMessageIndex[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                log.error("输出错误结果: {}", errorMessage[0]);
                            } else {
                                message[messageIndex[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
                                log.info("输出结果: {}", message[0]);
                            }
                            super.onNext(frame);
                        }
                    })
                    .awaitCompletion(TIME_OUT, TimeUnit.SECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
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

    private StatsCmd getRunStatistics(String containerId, long[] maxMemory) {
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

            @Override
            public void onNext(Statistics statistics) {
//                log.info("内存占用 {}", statistics.getMemoryStats().getUsage());
                maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
            }

            @Override
            public void close() throws IOException {

            }

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });
        statsCmd.exec(statisticsResultCallback);
        return statsCmd;
    }

}




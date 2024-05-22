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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="https://www.github.com/lieeew">leikooo</a>
 */
@Slf4j
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    public static final String IMAGE_NAME = "openjdk:8-alpine";

    private static final long TIME_OUT = 5000L;

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
        String inputArgsArray = "\"" + String.join("\" \"", inputList) + "\"";
//        String[] javacSolution = ArrayUtil.append(new String[]{"javac", "-encoding", "utf-8", "/app/Solution.java"});
//        String[] javacRunMain = ArrayUtil.append(new String[]{"javac", "-encoding", "utf-8", "/app/MainSolution.java"});
        // "java", "-Dfile.encoding=UTF-8", "-cp", "/app", "Solution", "twoSum" , "1:[1,2,3,4]" ,"10:3"]
        // 合并命令为一个单一的命令字符串
        String combinedCommand = String.join(" && ",
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
        log.info("执行结果: {}", executeResult);
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
        final String[] message = {null};
        final String[] errorMessage = {null};
        long time = 0L;
        // 判断是否超时
        final boolean[] isTimeOut = {true};
        String execId = execCreateCmdResponse.getId();

        final long[] maxMemory = {0L};

        // 获取占用的内存
        StatsCmd statsCmd = getRunStatistics(containerId, maxMemory);
        try {
            stopWatch.start();
            dockerClient.execStartCmd(execId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onComplete() {
                            // 如果执行完成，则表示没超时
                            isTimeOut[0] = false;
                            super.onComplete();
                        }

                        @Override
                        public void onNext(Frame frame) {
                            StreamType streamType = frame.getStreamType();
                            if (StreamType.STDERR.equals(streamType)) {
                                errorMessage[0] = new String(frame.getPayload());
                                log.error("输出错误结果: {}", errorMessage[0]);
                            } else {
                                message[0] = new String(frame.getPayload());
                                log.error("输出结果: {}", message[0]);
                            }
                            super.onNext(frame);
                        }
                    })
                    .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
            statsCmd.close();
        } catch (InterruptedException e) {
            log.error("程序执行异常: {}", ExceptionUtils.getStackTrace(e));
        }
        ExecuteMessage executeMessage = new ExecuteMessage();
        executeMessage.setMessage(message[0]);
        executeMessage.setErrorMessage(errorMessage[0]);
        executeMessage.setTime(time);
        executeMessage.setMemory(maxMemory[0]);
        return executeMessage;
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




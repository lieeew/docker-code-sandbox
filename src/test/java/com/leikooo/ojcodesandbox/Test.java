package com.leikooo.ojcodesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.InvocationBuilder;
import com.leikooo.ojcodesandbox.docker.JavaDockerCodeSandbox;
import com.leikooo.ojcodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/24
 * @description
 */
@Slf4j
@SpringBootTest
public class Test {

    @Resource
    private DockerClient dockerClient;

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @org.junit.jupiter.api.Test
    void test() {
        StopWatch stopWatch = new StopWatch();
        final String[] message = new String[30];
        final String[] errorMessage = new String[30];
        long time = 0L;
        // 判断是否超时
        final boolean[] isTimeOut = {true};

        final long[] maxMemory = {0L};

        final int[] i = {0};
        // 获取占用的内存
        try {
            stopWatch.start();
            dockerClient.logContainerCmd("0eb0898d8be9")
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
                                errorMessage[i[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
//                                log.error("输出错误结果: {}", errorMessage[0]);
                            } else {
                                message[i[0]++] = new String(frame.getPayload(), StandardCharsets.UTF_8);
//                                log.info("输出结果: {}", message[0]);
                            }
                            super.onNext(frame);
                        }
                    })
                    .awaitCompletion(10, TimeUnit.SECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
        } catch (InterruptedException e) {
            log.error("程序执行异常: {}", ExceptionUtils.getStackTrace(e));
        }
        ExecuteMessage executeMessage = new ExecuteMessage();
        executeMessage.setMessage(Arrays.stream(message).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        executeMessage.setErrorMessage(Arrays.stream(errorMessage).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        executeMessage.setTime(time);
        executeMessage.setMemory(maxMemory[0]);
        log.info("getExecuteResult result {}", executeMessage);
    }

//    @org.junit.jupiter.api.Test
//    public void test2() {
//        ExecuteMessage executeResult = javaDockerCodeSandbox.getExecuteResult("d7c3698c50eb");
//
//    }

    @org.junit.jupiter.api.Test
    public void test3() {
        StatsCmd statsCmd = dockerClient.statsCmd("8467ee5ad9a0");
        statsCmd.exec(new ResultCallback.Adapter<Statistics>() {
            @Override
            public void onNext(Statistics statistics) {
                Long memoryUsage = statistics.getMemoryStats().getUsage();
                Long memoryLimit = statistics.getMemoryStats().getLimit();
                System.out.println("Memory Usage: " + memoryUsage + " bytes");
                System.out.println("Memory Limit: " + memoryLimit + " bytes");
            }

            @Override
            public void close() throws IOException {

            }

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {
                log.error("getRunStatistics {}", ExceptionUtils.getRootCauseMessage(throwable));
            }

            @Override
            public void onComplete() {

            }
        }).onComplete();
    }

    @org.junit.jupiter.api.Test
    public void test4() {
        InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
        dockerClient.statsCmd("8467ee5ad9a0").exec(callback);
        Statistics stats;
        try {
            stats = callback.awaitResult();
            callback.close();
        } catch (RuntimeException | IOException e) {
            // you may want to throw an exception here
        }
    }

    @org.junit.jupiter.api.Test
    public void test5() {
        String input = "groupAnagrams 18:[eat,tea,tan,ate,nat,bat]";
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

    }

    @org.junit.jupiter.api.Test
    public void test6() {
        File files = new File("E:\\yuoj-code-sandbox-master\\src\\main\\resources\\testCode");
        // 删除下级内容
        try {
            Files.walk(files.toPath()) // Traverse the file tree in depth-first order
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            System.out.println("Deleting: " + path);
                            Files.delete(path);  //delete each file or directory
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}



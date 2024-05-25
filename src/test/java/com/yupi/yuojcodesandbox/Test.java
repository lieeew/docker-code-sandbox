package com.yupi.yuojcodesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.yupi.yuojcodesandbox.config.DockerClientConfig;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
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

}

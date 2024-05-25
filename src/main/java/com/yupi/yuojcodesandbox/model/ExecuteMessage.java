package com.yupi.yuojcodesandbox.model;

import lombok.*;

/**
 * 进程执行信息
 * @author <a href="https://www.github.com/lieeew">leikooo</a>
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteMessage {
    /**
     * @see ExitValue
     */
    private Integer exitValue;

    private String message;

    private String errorMessage;

    /**
     * 消耗时间
     */
    private Long time;

    /**
     * 占用内存
     */
    private Long memory;
}

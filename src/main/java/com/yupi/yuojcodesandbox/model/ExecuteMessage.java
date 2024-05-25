package com.yupi.yuojcodesandbox.model;

import lombok.*;

/**
 * 进程执行信息
 * @author <a href="https://www.github.com/lieeew">leikooo</a>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExecuteMessage {
    /**
     * @see ExitValue
     */
    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}

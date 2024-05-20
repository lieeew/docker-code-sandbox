package com.yupi.yuojcodesandbox.model;

import lombok.Getter;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/20
 * @description
 */
@Getter
public enum ExitValue {
    NORMAL(0, "SUCCESS"),
    ABNORMAL(1, "ABNORMAL"),
    ;

    private final Integer code;

    private final String des;

    ExitValue(Integer code, String des) {
        this.code = code;
        this.des = des;
    }
}

package com.leikooo.ojcodesandbox.docker;


import com.leikooo.ojcodesandbox.model.ExecuteCodeRequest;
import com.leikooo.ojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) ;
}

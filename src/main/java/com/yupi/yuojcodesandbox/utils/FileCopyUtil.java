package com.yupi.yuojcodesandbox.utils;

import cn.hutool.core.util.ClassUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/19
 * @description
 */
@Slf4j
public class FileCopyUtil {

    public static void copyFromResourcePathToFile(String resourcePath, File file) {
        try {
            try (InputStream inputStream = ClassUtil.getClassLoader().getResourceAsStream(resourcePath);
                 FileOutputStream outputStream = new FileOutputStream(file)) {
                FileCopyUtils.copy(Objects.requireNonNull(inputStream), outputStream);
            }
        } catch (IOException e) {
            log.error("FileCopyUtil#copyFromResourcePathToFile {}" , ExceptionUtils.getRootCauseMessage(e));
        }
    }
}

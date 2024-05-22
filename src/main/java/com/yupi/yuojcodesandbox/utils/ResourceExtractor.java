package com.yupi.yuojcodesandbox.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author <a href="https://www.github.com/lieeew">leikooo</a>
 */
public class ResourceExtractor {
    private static File tempDir = null;

    static {
        try {
            tempDir = Files.createTempDirectory("ca").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * todo 后期优化
     *
     * @param resourcePath
     * @return
     * @throws IOException
     */
    public static File extractResource(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        File tempFile = Files.createTempFile("temp", resource.getFilename()).toFile();
        try (InputStream inputStream = resource.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            FileCopyUtils.copy(inputStream, outputStream);
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

    public static File extractResourceDirectory(String resourceDirectory) {
        tempDir.deleteOnExit();
        // 获取资源目录下的文件列表
        String[] resourceFiles = {"ca.pem", "cert.pem", "key.pem"}; // 手动列出文件
        for (String resourceFile : resourceFiles) {
            File targetFile = new File(tempDir, resourceFile);
            FileCopyUtil.copyFromResourcePathToFile(resourceDirectory + "/" + resourceFile, targetFile);
        }
        return tempDir;
    }
}
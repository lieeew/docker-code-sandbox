package com.leikooo.ojcodesandbox.event.lisener;

import com.leikooo.ojcodesandbox.event.FileDeleteEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Objects;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/5/22
 * @description 运行之后删除 container
 */
@Slf4j
@Component
public class FileDeleteListener {
    @EventListener(classes = FileDeleteEvent.class)
    public void deleteFile(FileDeleteEvent fileDeleteEvent) {
        // /app/tempCode/uuid random dir
        File paths = fileDeleteEvent.getPath();
        if (Objects.isNull(paths)) {
            log.error("FileDeleteListener#deleteFile path is null");
            return;
        }
        deleteFileContents(paths);
    }

    private void deleteFileContents(File folder) {
        File[] files = folder.listFiles();
        if (Objects.isNull(files)) {
            log.error("FileDeleteListener#deleteFile path content is null");
            return;
        }
        for (File file : files) {
            deleteFolder(file);
        }
        folder.delete();
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { // some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}

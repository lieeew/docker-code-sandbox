package com.leikooo.ojcodesandbox.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.io.File;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Getter
public class FileDeleteEvent extends ApplicationEvent {

    private final File path;

    public FileDeleteEvent(Object source, File path) {
        super(source);
        this.path = path;
    }
}

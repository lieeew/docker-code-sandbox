package com.yupi.yuojcodesandbox.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Getter
public class ContainerDeleteEvent extends ApplicationEvent {

    private final String containerId;

    public ContainerDeleteEvent(Object source, String containerId) {
        super(source);
       this.containerId = containerId;
    }
}

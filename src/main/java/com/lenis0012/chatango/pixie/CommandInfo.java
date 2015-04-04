package com.lenis0012.chatango.pixie;

import com.lenis0012.chatango.pixie.entities.CommandModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@Getter
@RequiredArgsConstructor
public class CommandInfo {
    private final Object instance;
    private final Method method;
    private final CommandModel model;
    private long allowed = 0L;

    public void delay(int time) {
        this.allowed = System.currentTimeMillis() + time * 1000L;
    }
}

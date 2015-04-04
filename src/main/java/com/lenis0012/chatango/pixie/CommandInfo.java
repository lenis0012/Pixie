package com.lenis0012.chatango.pixie;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;

@Getter
@RequiredArgsConstructor
public class CommandInfo {
    private final Object instance;
    private final Method method;
}

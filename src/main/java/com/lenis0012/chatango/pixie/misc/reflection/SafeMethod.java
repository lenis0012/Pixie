/*
 * .  ____                 ____    ___
 *   / ___|___  _ __ ___  |___ \  / _ \
 *  | |   / _ \| '__/ _ \   __) || | | |
 *  | |__| (_) | | |  __/  / __/ | |_| |
 *   \____\___/|_|  \___| |_____(_)___/
 * This file is part of Core, a bukkit server framework for a minigame network.
 * Copyright (c) 2015. Lennart ten Wolde All rights reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 */

package com.lenis0012.chatango.pixie.misc.reflection;


import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SafeMethod {
    private Method method;

    public SafeMethod(Method method) {
        this.method = method;
    }

    public String getName() {
        return method.getName();
    }

    @SuppressWarnings("unchecked")
    public <T> T invoke(Object instance, Class<T> type, Object... args) {
        return (T) invoke(instance, args);
    }

    public Object invoke(Object instance, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch(Exception e) {
            Logger.getLogger("Minecraft").log(Level.WARNING, "Plugin tried to access unknown method", e);
            return null;
        }
    }

    public Method getHandle() {
        return this.method;
    }
}

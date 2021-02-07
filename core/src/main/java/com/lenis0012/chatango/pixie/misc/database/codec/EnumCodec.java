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

package com.lenis0012.chatango.pixie.misc.database.codec;

import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;

import java.lang.reflect.Method;

public class EnumCodec extends DBCodec<Object> {

    public EnumCodec(DatabaseEngine engine) {
        super(engine);
    }

    @Override
    public boolean canEncode(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public boolean canDecode(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public Object encode(Class<?> type, SafeField field, Object value) {
        return ((Enum) value).name();
    }

    @Override
    public Object decode(Class<?> type, SafeField field, Object object) {
        try {
            Method method = type.getMethod("valueOf", String.class);
            return method.invoke(null, object);
        } catch(Exception e) {
            return null;
        }
    }
}

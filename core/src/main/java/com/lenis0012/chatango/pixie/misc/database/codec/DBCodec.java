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

public abstract class DBCodec<T> {
    protected final DatabaseEngine engine;

    protected DBCodec(DatabaseEngine engine) {
        this.engine = engine;
    }

    public abstract boolean canEncode(Class<?> type);

    public abstract boolean canDecode(Class<?> type);

    public abstract Object encode(Class<?> type, SafeField field, T value);

    public abstract Object decode(Class<?> type, SafeField field, Object object);

    public Object encodeFrom(Class<?> type, SafeField field, Object object) {
        return encode(type, field, (T) object);
    }

    protected String parse(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}

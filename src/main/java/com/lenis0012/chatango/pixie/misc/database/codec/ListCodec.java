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

import com.google.common.collect.Lists;
import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.lenis0012.chatango.pixie.misc.database.Storable;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;
import com.mongodb.BasicDBList;

import java.util.List;

public class ListCodec extends DBCodec<List<?>> {

    public ListCodec(DatabaseEngine engine) {
        super(engine);
    }

    @Override
    public boolean canEncode(Class<?> type) {
        return List.class.isAssignableFrom(type);
    }

    @Override
    public boolean canDecode(Class<?> type) {
        return canEncode(type);
    }

    @Override
    public Object encode(Class<?> type, SafeField field, List<?> value) {
        BasicDBList list = new BasicDBList();
        for(Object entry : value) {
            list.add(engine.encode(field.getAnnotation(Storable.class).genericType(), field, entry));
        }

        return list;
    }

    @Override
    public Object decode(Class<?> type, SafeField field, Object object) {
        List<Object> list = Lists.newArrayList();
        BasicDBList dbList = (BasicDBList) object;
        for(Object entry : dbList) {
            list.add(engine.decode(field.getAnnotation(Storable.class).genericType(), field, entry));
        }

        return list;
    }
}

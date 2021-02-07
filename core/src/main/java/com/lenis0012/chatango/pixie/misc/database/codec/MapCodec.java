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
import com.lenis0012.chatango.pixie.misc.database.Storable;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;
import com.mongodb.BasicDBObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapCodec extends DBCodec<Map<String, ?>> {

    protected MapCodec(DatabaseEngine engine) {
        super(engine);
    }

    @Override
    public boolean canEncode(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    @Override
    public boolean canDecode(Class<?> type) {
        return canEncode(type);
    }

    @Override
    public Object encode(Class<?> type, SafeField field, Map<String, ?> value) {
        BasicDBObject object = new BasicDBObject();
        Class<?> genericType = field.getAnnotation(Storable.class).genericType();
        for(Entry<String, ?> entry : value.entrySet()) {
            object.append(entry.getKey(), engine.encode(genericType, field, entry.getValue()));
        }

        return object;
    }

    @Override
    public Object decode(Class<?> type, SafeField field, Object object) {
        Map<String, Object> map = new HashMap<>();
        BasicDBObject dbObject = (BasicDBObject) object;
        Class<?> genericType = field.getAnnotation(Storable.class).genericType();
        for(Entry<String, Object> entry : dbObject.entrySet()) {
            map.put(entry.getKey(), engine.decode(genericType, field, entry.getValue()));
        }

        return map;
    }
}

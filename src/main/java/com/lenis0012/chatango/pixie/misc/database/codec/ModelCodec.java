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

import com.lenis0012.chatango.pixie.misc.CommonUtil;
import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.lenis0012.chatango.pixie.misc.database.Model;
import com.lenis0012.chatango.pixie.misc.database.Storable;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;
import com.mongodb.BasicDBObject;

import java.util.Map;
import java.util.Map.Entry;

public class ModelCodec extends DBCodec<Object> {
    public ModelCodec(DatabaseEngine engine) {
        super(engine);
    }

    @Override
    public boolean canEncode(Class<?> type) {
        Class<?> clazz = type;
        while(clazz != null) {
            if(clazz.isAnnotationPresent(Model.class)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    @Override
    public boolean canDecode(Class<?> type) {
        return canEncode(type);
    }

    @Override
    public Object encode(Class<?> type, SafeField field, Object value) {
        BasicDBObject dbObject = new BasicDBObject();
        for(Entry<String, SafeField> entry : engine.getFields(type).entrySet()) {
            SafeField f = entry.getValue();
            if(f.getAnnotation(Storable.class) == null) {
                continue;
            }
            Object val = f.get(value);
            dbObject.append(parse(entry.getKey()), engine.encode(f.getType(), f, val));
        }

        return dbObject;
    }

    @Override
    public Object decode(Class<?> type, SafeField field, Object object) {
        BasicDBObject dbObject = (BasicDBObject) object;
        Map<String, SafeField> fields = engine.getFields(type);
        Object instance = CommonUtil.instance(type);
        for(Entry<String, Object> entry : dbObject.entrySet()) {
            String name = entry.getKey();
            Object raw = entry.getValue();
            SafeField f = fields.get(name);
            if(f != null) {
                f.set(instance, engine.decode(f.getType(), f, raw));
            }
        }

        return instance;
    }
}

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

package com.lenis0012.chatango.pixie.misc.database;

import com.lenis0012.chatango.pixie.misc.CaseInsensitiveMap;
import com.lenis0012.chatango.pixie.misc.CommonUtil;
import com.lenis0012.chatango.pixie.misc.database.codec.*;
import com.lenis0012.chatango.pixie.misc.reflection.SafeClass;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DatabaseEngine {
    private static DatabaseEngine instance;

    private void setInstance(DatabaseEngine instance) {
        DatabaseEngine.instance = instance;
    }

    public static DatabaseEngine getInstance() {
        return instance;
    }

    private final LoadingCache<Class<?>, Map<String, SafeField>> fieldCache;
    private final Set<DBCodec<?>> codecs = Sets.newConcurrentHashSet();
    private final MongoClient client;

    public DatabaseEngine(String address, int port) {
        setInstance(this);
        try {
            this.client = new MongoClient(address, port);
        } catch(UnknownHostException e) {
            throw new IllegalArgumentException("Unkown database host: " + address + ":" + port);
        }

        // Register default codecs
        registerCodec(EnumCodec.class);
        registerCodec(ListCodec.class);
        registerCodec(ModelCodec.class);
        registerCodec(UUIDCodec.class);
        registerCodec(MapCodec.class);

        // Create field cache
        this.fieldCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).maximumSize(32L).build(new CacheLoader<Class<?>, Map<String, SafeField>>() {
            @Override
            public Map<String, SafeField> load(Class<?> aClass) throws Exception {
                Map<String, SafeField> map = new CaseInsensitiveMap<>();
                SafeClass safeClass = new SafeClass(aClass);
                for(SafeField field : safeClass.getFields()) {
                    map.put(field.getName(), field);
                }
                return map;
            }
        });
    }

    public Database getDatabase(String name) {
        DB db = client.getDB(name);
        return new Database(this, db);
    }

    /**
     * Register a custom codec
     *
     * @param codecClass Class of the codec
     */
    public void registerCodec(Class<? extends DBCodec<?>> codecClass) {
        DBCodec<?> codec = (DBCodec<?>) CommonUtil.instance(codecClass, this);
        codecs.add(codec);
    }

    public Object encode(Class<?> type, SafeField field, Object value) {
        for(DBCodec<?> codec : codecs) {
            if(codec.canEncode(type)) {
                return codec.encodeFrom(type, field, value);
            }
        }

        return value;
    }

    public Object decode(Class<?> type, SafeField field, Object dbObject) {
        for(DBCodec<?> codec : codecs) {
            if(codec.canDecode(type)) {
                return codec.decode(type, field, dbObject);
            }
        }

        return dbObject;
    }

    public Map<String, SafeField> getFields(Class<?> clazz) {
        try {
            return fieldCache.get(clazz);
        } catch(ExecutionException e) {
            return null;
        }
    }
}

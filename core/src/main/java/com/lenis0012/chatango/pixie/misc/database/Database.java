/*
 * .  ____                 ____    ___
 *   / ___|___  _ __ ___  |___ \  / _ \
 *  | |   / _ \| '__/ _ \   __) || | | |
 *  | |__| (_) | | |  __/  / __/ | |_| |
 *   \____\___/|_|  \___| |_____(_)___/
 * This file is part of Core, a bukkit server framework for a minigame network.
 * Copyright (c) 2014. Lennart ten Wolde All rights reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 */

package com.lenis0012.chatango.pixie.misc.database;

import com.lenis0012.chatango.pixie.misc.CommonUtil;
import com.lenis0012.chatango.pixie.misc.reflection.SafeField;
import com.mongodb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Database {
    private final DatabaseEngine engine;
    private final DB database;

    protected Database(DatabaseEngine engine, DB handle) {
        this.engine = engine;
        this.database = handle;
    }

    /**
     * Get a raw mongo database location by it's name.
     *
     * @param name Name of collection
     * @return The mongo DBCollection
     */
    public DBCollection getCollection(String name) {
        return database.getCollection(name);
    }

    public <T> List<T> findModels(Class<T> type, BasicDBObject query) {
        // Find the collection name
        Model model = getAnnotation(type);

        List<T> list = new ArrayList<>();
        DBCollection collection = getCollection(model.name());
        DBCursor cursor = query == null ? collection.find() : collection.find(query);
        while(cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            if(dbObject instanceof BasicDBObject) {
                Object instance = CommonUtil.instance(type);
                parseDBToModel(instance, (BasicDBObject) dbObject);
                list.add(type.cast(instance));
            }
        }

        return list;
    }

    public <T> T findModel(Class<T> type, BasicDBObject object) {
        return findModels(type, object).get(0);
    }

    /**
     * Load a model from the database if exists.
     *
     * @param instance Instance of the model
     * @param query    Query to find data in the database
     */
    public void loadModel(Object instance, BasicDBObject query) {
        // Find the collection name
        Model model = getAnnotation(instance.getClass());

        // Obtain the data
        DBCollection collection = getCollection(model.name());
        DBObject result = collection.findOne(query);
        if(result != null && result instanceof BasicDBObject) {
            // Parse the database info
            parseDBToModel(instance, (BasicDBObject) result);
        }
    }

    private void parseDBToModel(Object model, BasicDBObject data) {
        Map<String, SafeField> fields = engine.getFields(model.getClass());
        for(Entry<String, Object> entry : data.entrySet()) {
            String name = entry.getKey();
            SafeField field = fields.get(name);
            if(field != null) {
                field.set(model, engine.decode(field.getType(), field, entry.getValue()));
            }
        }
    }

    /**
     * Save a model to the database.
     *
     * @param instance Instance of model
     * @param query    The query to find and update the existing entry (null force create new)
     */
    public void saveModel(Object instance, BasicDBObject query) {
        // Find the collection name
        Model model = getAnnotation(instance.getClass());

        // Generate a new database object
        BasicDBObject object = new BasicDBObject();
        Map<String, SafeField> fields = engine.getFields(instance.getClass());
        for(Entry<String, SafeField> entry : fields.entrySet()) {
            String name = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            SafeField field = entry.getValue();
            if(field.getAnnotation(Storable.class) != null) {
                Object value = field.get(instance);
                object.append(name, engine.encode(field.getType(), field, value));
            }
        }

        // Save the object
        DBCollection collection = getCollection(model.name());
        if(query != null) {
            collection.update(query, object, true, false);
        } else {
            collection.save(object); // This can cause problems when loading a single model and the query returns 2 or more entries, thus only use this if you expect multiple results.
        }
    }

    private Model getAnnotation(Class<?> clazz) {
        while(clazz != null) {
            if(clazz.isAnnotationPresent(Model.class)) {
                return clazz.getAnnotation(Model.class);
            }
            clazz = clazz.getSuperclass();
        }

        throw new IllegalArgumentException("Model does not have @Model annotation!");
    }
}

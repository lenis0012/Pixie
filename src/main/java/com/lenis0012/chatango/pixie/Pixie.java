package com.lenis0012.chatango.pixie;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.lenis0012.chatango.bot.api.Message;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Engine;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.JsonConfig;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.lenis0012.chatango.pixie.misc.database.Database;
import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.mongodb.BasicDBObject;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Pixie {
    public static final String MASHAPE_KEY = "jY0bEhHCBpmsh8j1mpA5p11tCJGyp1tok3Zjsn4ubbvNNp5Jt3";

    private final Set<CommandInfo> commands;
    private final Engine engine;
    private final Random random;
    private final JsonConfig config;
    private final DatabaseEngine databaseEngine;
    private List<String> truths;
    private List<String> dares;
    private Map<String, List<String>> replacements = Maps.newConcurrentMap();

    public Pixie(Engine engine, Set<CommandInfo> commands) {
        this.commands = commands;
        this.engine = engine;
        this.random = new Random();
        this.config = new JsonConfig("config.json");
        this.truths = Utils.convertList(config.getList("truths", new JsonArray()), String.class);
        this.dares = Utils.convertList(config.getList("dares", new JsonArray()), String.class);
        this.databaseEngine = new DatabaseEngine(
                config.get("database.host", new JsonPrimitive("localhost")).getAsString(),
                config.get("database.port", new JsonPrimitive(27017)).getAsInt());

        for(String key : config.keys("replacements")) {
            replacements.put(key, Utils.convertList(config.getList("replacements." + key, new JsonArray()), String.class));
        }
        config.save();
    }

    public String replaceWords(String text) {
        for(Map.Entry<String, List<String>> entry : replacements.entrySet()) {
            while(text.contains(entry.getKey())) {
                text = text.replaceFirst(entry.getKey(), entry.getValue().get(random.nextInt(entry.getValue().size())));
            }
        }
        return text;
    }

    public String pickUsers(Room room, String msg) {
        // Pick random user
        List<User> users = room.getUserList();
        users.remove(room.findUser(engine.getCredentials().getUsername().toLowerCase()));
        while(msg.contains("USER")) {
            User picked = users.get(random.nextInt(users.size()));
            msg = msg.replaceFirst("USER", picked.getName());
            users.remove(picked);
        }

        return msg;
    }

    public List<String> getTruths() {
        return truths;
    }

    public List<String> getDares() {
        return dares;
    }

    public Random getRandom() {
        return random;
    }

    public Set<CommandInfo> getCommands() {
        return commands;
    }

    public Database database() {
        return databaseEngine.getDatabase("chatango");
    }

    public UserModel getUser(String name) {
        UserModel model = new UserModel(name);
        database().loadModel(model, new BasicDBObject("Name", name));
        return model;
    }

    public void msg(final Room room, final String msg, long time, TimeUnit unit) {
        final long ms = unit.toMillis(time);
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(ms);
                } catch(InterruptedException e) {}
                msg(room, msg);
            }
        }.start();
    }

    public void msgTo(Room room, User user, String msg) {
        msg(room, "@" + user.getName() + " " + msg);
    }

    public void msg(Room room, String msg) {
        room.message(new Message(msg));
    }
}

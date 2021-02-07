package com.lenis0012.chatango.pixie;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.lenis0012.chatango.bot.api.Message;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Engine;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.JsonConfig;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.lenis0012.chatango.pixie.misc.SimSimi;
import com.lenis0012.chatango.pixie.misc.database.Database;
import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.mongodb.BasicDBObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Pixie {
    private final LoadingCache<String, UserModel> userCache = Caffeine.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(s -> {
        UserModel model = new UserModel(s);
        database().loadModel(model, new BasicDBObject("Name", model.getName()));
        return model;
    });
    private final Set<CommandInfo> commands;
    private final Engine engine;
    private final Random random;
    private final JsonConfig config;
    private final DatabaseEngine databaseEngine;
    private final String ytApiKey;
    private final String imgurApiKey;
    private List<String> truths;
    private List<String> dares;
    private Map<String, List<String>> replacements = new ConcurrentHashMap<>();
    private final SimSimi simSimi;
    private boolean muted;
    private long pingTime;

    public Pixie(Engine engine, Set<CommandInfo> commands) {
        this.commands = commands;
        this.engine = engine;
        this.random = new Random();
        this.config = new JsonConfig("config.json");
        this.truths = Utils.convertList(config.getList("truths", new JsonArray()), String.class);
        this.dares = Utils.convertList(config.getList("dares", new JsonArray()), String.class);
        config.get("mashape-key", new JsonPrimitive(""));
        config.get("pastebin.api-key", new JsonPrimitive(""));
        this.ytApiKey = config.get("youtube.api-key", new JsonPrimitive("")).getAsString();
        this.imgurApiKey = config.get("imgur.api-key", new JsonPrimitive("")).getAsString();
        this.databaseEngine = new DatabaseEngine(
                config.get("database.host", new JsonPrimitive("localhost")).getAsString(),
                config.get("database.port", new JsonPrimitive(27017)).getAsInt());

        for(String key : config.keys("replacements")) {
            replacements.put(key, Utils.convertList(config.getList("replacements." + key, new JsonArray()), String.class));
        }

        this.simSimi = new SimSimi(this);
        config.save();
    }

    public void clearCache() {
        userCache.asMap().clear();
    }

    public String getImgurApiKey() {
        return imgurApiKey;
    }

    public long getPingTime() {
        return pingTime;
    }

    public void setPingTime(long pingTime) {
        this.pingTime = pingTime;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public String getYtApiKey() {
        return ytApiKey;
    }

    public SimSimi getSimSimi() {
        return simSimi;
    }

    public Engine getEngine() {
        return engine;
    }

    public String getCommandsURL() throws IOException {
        String url = config.get("pastebin.url", new JsonPrimitive("")).getAsString();
        long expiry = config.get("pastebin.expiry", new JsonPrimitive(0L)).getAsLong();
        if(expiry <= System.currentTimeMillis()) {
            // Generate new url
            StringBuilder builder = new StringBuilder("--- User Commands ---\n");
            List<CommandInfo> cmds = new ArrayList<>(commands);
            Collections.sort(cmds, (cmd1, cmd2) -> cmd1.getModel().getName().compareTo(cmd2.getModel().getName()));
            for(CommandInfo info : cmds) {
                if(info.getMethod().getAnnotation(Command.class).adminOnly()) {
                    continue;
                }
                builder.append(info.getModel().getName());
                builder.append("\n");
            }
            builder.append("--- Admin commands ---\n");
            for(CommandInfo info : cmds) {
                if(!info.getMethod().getAnnotation(Command.class).adminOnly()) {
                    continue;
                }
                builder.append(info.getModel().getName());
                builder.append("\n");
            }
            String apiUrl = "http://pastebin.com/api/api_post.php";
            String apiParams = "api_option=paste&" +
                    "api_user_key=%s&" +
                    "api_paste_private=%s&" +
                    "api_paste_name=%s&" +
                    "api_paste_expire_date=%s&" +
                    "api_paste_format=%s&" +
                    "api_dev_key=%s&" +
                    "api_paste_code=%s";
            String params = Utils.formatUrl(apiParams, "", "0", "Pixie Command List", "1W", "text",
                    config.get("pastebin.api-key", new JsonPrimitive("")).getAsString(), builder.toString());
            System.out.println(config.get("pastebin.api-key", new JsonPrimitive("")).getAsString());
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.writeBytes(params);
            output.flush();
            output.close();

            int responseCode = connection.getResponseCode();
            if(responseCode != 200) {
                throw new IOException("Invalid response code: " + connection.getResponseCode());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = reader.readLine();
            reader.close();
            config.set("pastebin.url", result);
            config.set("pastebin.expiry", System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7); // 7 days
            config.save();
        }

        return config.get("pastebin.url").getAsString();
    }

    public JsonConfig getConfig() {
        return config;
    }

    public String getMashapeKey() {
        return config.get("mashape-key").getAsString();
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
        return userCache.get(name.toLowerCase());
    }

    public void msg(final Room room, final String msg, long time, TimeUnit unit) {
        final long ms = unit.toMillis(time);
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(ms);
                } catch(InterruptedException e) {
                }
                msg(room, msg);
            }
        }.start();
    }

    public void msgTo(Room room, User user, String msg) {
        msg(room, "@" + user.getName() + " " + msg);
    }

    public void msg(Room room, String msg) {
        if(muted) {
            return;
        }
        room.message(new Message(msg));
    }
}

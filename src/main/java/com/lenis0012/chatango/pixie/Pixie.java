package com.lenis0012.chatango.pixie;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import com.lenis0012.chatango.pixie.misc.SimSimi;
import com.lenis0012.chatango.pixie.misc.database.Database;
import com.lenis0012.chatango.pixie.misc.database.DatabaseEngine;
import com.mongodb.BasicDBObject;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Pixie {
    private final LoadingCache<String, UserModel> userCache = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<String, UserModel>() {
        @Override
        public UserModel load(String s) throws Exception {
            UserModel model = new UserModel(s);
            database().loadModel(model, new BasicDBObject("Name", model.getName()));
            return model;
        }
    });
    private final Set<CommandInfo> commands;
    private final Engine engine;
    private final Random random;
    private final JsonConfig config;
    private final DatabaseEngine databaseEngine;
    private List<String> truths;
    private List<String> dares;
    private Map<String, List<String>> replacements = Maps.newConcurrentMap();
    private final SimSimi simSimi;

    public Pixie(Engine engine, Set<CommandInfo> commands) {
        this.commands = commands;
        this.engine = engine;
        this.random = new Random();
        this.config = new JsonConfig("config.json");
        this.truths = Utils.convertList(config.getList("truths", new JsonArray()), String.class);
        this.dares = Utils.convertList(config.getList("dares", new JsonArray()), String.class);
        config.get("mashape-key", new JsonPrimitive(""));
        config.get("pastebin.api-key", new JsonPrimitive(""));
        this.databaseEngine = new DatabaseEngine(
                config.get("database.host", new JsonPrimitive("localhost")).getAsString(),
                config.get("database.port", new JsonPrimitive(27017)).getAsInt());

        for(String key : config.keys("replacements")) {
            replacements.put(key, Utils.convertList(config.getList("replacements." + key, new JsonArray()), String.class));
        }

        this.simSimi = new SimSimi(this);
        config.save();
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
            StringBuilder builder = new StringBuilder();
            for(CommandInfo info : commands) {
                builder.append(info.getModel().getName());
                if(info.getMethod().getAnnotation(Command.class).adminOnly()) {
                    builder.append(" [Admin Only]");
                }
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
        return userCache.getUnchecked(name.toLowerCase());
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

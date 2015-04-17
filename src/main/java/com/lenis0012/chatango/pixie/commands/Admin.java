package com.lenis0012.chatango.pixie.commands;

import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;
import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.ChatangoAPI;
import com.lenis0012.chatango.bot.api.Badge;
import com.lenis0012.chatango.bot.api.Channel;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.RegistrationException;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.CommandInfo;
import com.lenis0012.chatango.pixie.Pixie;
import com.lenis0012.chatango.pixie.entities.Definition;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Admin {
    private static final String URBAN_DIR_URL = "https://mashape-community-urban-dictionary.p.mashape.com/define";
    private final JsonParser jsonParser = new JsonParser();
    private final Pixie pixie;
    private final ScriptEngine scriptTank;
    private final ChatterBotFactory chatterBotFactory;
    private boolean botRunning = false;

    public Admin(Pixie pixie) {
        this.pixie = pixie;
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptTank = manager.getEngineByName("js");
        this.chatterBotFactory = new ChatterBotFactory();
    }

    @Command(adminOnly = true)
    @SneakyThrows(UnirestException.class)
    public void define(Room room, User user, String[] args) {
        String name = Joiner.on(' ').join(args);
        String url = Utils.urlEncode(URBAN_DIR_URL, "term", name);
        HttpResponse<String> response = Unirest.get(url).header("X-Mashape-Key", pixie.getMashapeKey())
                .header("Accept", "text/plain").asString();
        System.out.println(response.getBody());
        JsonObject object = (JsonObject) jsonParser.parse(response.getBody());
        Definition definition = null;
        if(object.has("list")) {
            JsonArray list = object.getAsJsonArray("list");
            if(list.size() > 0) {
                JsonObject entry = list.get(0).getAsJsonObject();
                String meaning = entry.get("definition").getAsString();
                String example = entry.has("example") ? entry.get("example").getAsString() : "";
                definition = new Definition(name.toLowerCase(), meaning, entry.get("author").getAsString(), example);
            }
        } if(definition == null) {
            definition = new Definition(name.toLowerCase(), "", "", "");
        }

        pixie.database().loadModel(definition, new BasicDBObject("Name", definition.getName()));
        if(!definition.getMeaning().isEmpty()) {
            pixie.msg(room, "Definition by " + definition.getDefinedBy() + ": " + definition.getMeaning());
            if (!definition.getExample().isEmpty()) {
                pixie.msg(room, "Example: " + definition.getExample(), 500L, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Command(adminOnly = true)
    public void delay(Room room, User user, String[] args) {
        if(args.length < 2) {
            pixie.msgTo(room, user, "Usage: delay [seconds] [command]");
            return;
        }
        for(CommandInfo info : pixie.getCommands()) {
            Command command = info.getMethod().getAnnotation(Command.class);
            List<String> aliases = command.aliases().length > 0 ? Arrays.asList(command.aliases()) : Arrays.asList(info.getMethod().getName().toLowerCase());
            if(aliases.contains(args[1].toLowerCase())) {
                info.getModel().setDelay(Integer.parseInt(args[0]));
                info.delay(info.getModel().getDelay());
                pixie.database().saveModel(info.getModel(), new BasicDBObject("Name", info.getModel().getName()));
                pixie.msgTo(room, user, "Set delay for command "+ args[1] + " to " + args[0]);
                return;
            }
        }
    }

    @Command(adminOnly = true)
    public void promote(Room room, User user, String[] args) {
        UserModel model = pixie.getUser(args[0].toLowerCase().replace("@", ""));
        model.setAdmin(true);
        pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
        pixie.msgTo(room, user, "Promoted user " + model.getName() + " to admin!");
    }

    @Command(adminOnly = true)
    public void demote(Room room, User user, String[] args) {
        UserModel model = pixie.getUser(args[0].toLowerCase().replace("@", ""));
        model.setAdmin(false);
        pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
        pixie.msgTo(room, user, "Demoted user " + model.getName() + " to admin!");
    }

    @Command
    public void admins(Room room, User user, String[] args) {
        List<UserModel> models = pixie.database().findModels(UserModel.class, new BasicDBObject("Admin", true));
        StringBuilder builder = new StringBuilder("List of admins: ");
        models.forEach(m -> builder.append(m.getName()).append(", "));
        builder.setLength(builder.length() - 2);
        pixie.msgTo(room, user, builder.toString());
    }

    @Command(adminOnly = true)
    public void channel(Room room, User user, String[] args) {
        Channel channel = Channel.valueOf(args[0].toUpperCase());
        room.setChannel(channel);
        pixie.msgTo(room, user, "Joined channel: " + channel.toString());
    }

    @Command(adminOnly = true)
    public void badge(Room room, User user, String[] args) {
        Badge badge = Badge.valueOf(args[0].toUpperCase());
        room.setBadge(badge);
        pixie.msgTo(room, user, "Set badge to: " + badge.toString());
    }

    @Command(adminOnly = true)
    public void color(Room room, User user, String[] args) {
        room.getDefaultFont().getColor().setRaw(args[0]);
        pixie.msgTo(room, user, "Set color to " + args[0] + "!");
    }

    @Command(adminOnly = true)
    public void alts(Room room, User user, String[] args) {
        String name = args[0].replace("@", "").toLowerCase();
        UserModel model = pixie.getUser(name);
        BasicDBList list = new BasicDBList();
        list.addAll(model.getIpAddresses());
        List<String> names = pixie.database().findModels(UserModel.class, new BasicDBObject("IpAddresses", new BasicDBObject("$in", list))).stream().map(UserModel::getName).collect(Collectors.toList());
        names.remove(name);
        if(names.size() > 0) {
            StringBuilder builder = new StringBuilder("List of alts: ");
            names.forEach(s -> builder.append(s).append(", "));
            builder.setLength(builder.length() - 2);
            pixie.msgTo(room, user, builder.toString());
        } else {
            pixie.msgTo(room, user, "User has no known alts!");
        }
    }

    @Command(adminOnly = true)
    public void ban(Room room, User user, String[] args) {
        String name = args[0].replace("@", "");
        UserModel model = pixie.getUser(name);
        model.setBanned(true);
        pixie.database().saveModel(model, new BasicDBObject("Name", name.toLowerCase()));
        pixie.msgTo(room, user, name + " is banned from using pixie commands!");
    }

    @Command(adminOnly = true)
    public void unban(Room room, User user, String[] args) {
        String name = args[0].replace("@", "");
        UserModel model = pixie.getUser(name);
        if(model.isBanned()) {
            model.setBanned(false);
            pixie.database().saveModel(model, new BasicDBObject("Name", name.toLowerCase()));
            pixie.msgTo(room, user, name + " is unbanned and can now use pixie commands again!");
        } else {
            pixie.msgTo(room, user, name + " is not banned right now!");
        }
    }

    @Command
    public void banlist(Room room, User user, String[] args) {
        StringBuilder builder = new StringBuilder("Banned users: ");
        List<UserModel> users = pixie.database().findModels(UserModel.class, new BasicDBObject("Banned", true));
        if(users.size() > 0) {
            users.forEach(u -> builder.append(u.getName()).append(", "));
            pixie.msgTo(room, user, builder.toString());
        } else {
            pixie.msgTo(room, user, "There are no banned users!");
        }
    }

    @Command(adminOnly = true)
    public void clearbans(Room room, User user, String[] args) {
        pixie.database().getCollection("users").findAndRemove(new BasicDBObject("Banned", true));
        pixie.msg(room, "Clear banlist!");
    }

    @Command(aliases = {"adddare"}, adminOnly = true)
     public void addDare(Room room, User user, String[] args) {
        pixie.getDares().add(Joiner.on(' ').join(args));
        pixie.getConfig().save();
        pixie.msgTo(room, user, "Added message to dare list!");
    }

    @Command(aliases = {"addtruth"}, adminOnly = true)
    public void addTruth(Room room, User user, String[] args) {
        pixie.getTruths().add(Joiner.on(' ').join(args));
        pixie.getConfig().save();
        pixie.msgTo(room, user, "Added message to truth list!");
    }

    @Command(adminOnly = true)
    public void dares(Room room, User user, String[] args) {
        pixie.msgTo(room, user, Joiner.on("\n").join(pixie.getDares()));
    }

    @Command(adminOnly = true)
    public void truths(Room room, User user, String[] args) {
        pixie.msgTo(room, user, Joiner.on("\n").join(pixie.getTruths()));
    }

    @Command(adminOnly = true)
    public void records(Room room, User user, String[] args) {
        pixie.msgTo(room, user, "Amount of user records: " + pixie.database().findModels(UserModel.class, null).size());
    }

    @Command(adminOnly = true)
    public void eval(Room room, User user, String[] args) {
        String cmd = Joiner.on(' ').join(args).replace("\";", "\"");
        try {
            Object response = scriptTank.eval(cmd);
            if(response != null) {
                pixie.msg(room, response.toString());
            }
        } catch (ScriptException e) {
            pixie.msgTo(room, user, e.getMessage());
        }
    }

    @SneakyThrows
    @Command(adminOnly = true)
    public void rant(final Room room, final User user, String[] args) {
        final String topic = Joiner.on(' ').join(args);
        final ChatterBotSession ses1 = chatterBotFactory.create(ChatterBotType.CLEVERBOT).createSession();
        final ChatterBotSession ses2 = chatterBotFactory.create(ChatterBotType.JABBERWACKY).createSession();
        this.botRunning = !botRunning; // Toggle
        pixie.msgTo(room, user, botRunning ? "Starting rant!" : "Stopping rant!");
        if(botRunning) {
            new Thread() {
                @Override
                public void run() {
                    boolean jw = false;
                    String response = topic;
                    while(botRunning) {
                        try {
                            response = jw ? ses2.think(response) : ses1.think(response);
                            pixie.msg(room, (jw ? "JW: " : "CB: ") + response);
                            jw = !jw;
                        } catch (Exception e) {
                            botRunning = false;
                        }
                        try {
                            Thread.sleep(3000L);
                        } catch (InterruptedException e) {}
                    }
                }
            }.start();
        }
    }

    @Command(adminOnly = true)
    public void regacc(final Room room, final User user, String[] args) {
        if(args.length < 3) {
            pixie.msgTo(room, user, "Usage: pixie regacc email user pass");
            return;
        }

        try {
            ChatangoAPI.createAccount(args[0], args[1], args[2], room.getRoomName());
            pixie.msgTo(room, user, "If this function actually worked, the account is created.");
        } catch (RegistrationException e) {
            pixie.msgTo(room, user, "Error: " + e.getMessage());
        }
    }

    @Command(adminOnly = true)
    public void login(final Room room, final User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: pixie login USER PASS");
        }

        room.sendCommand("blogout");
        if(args.length > 1) {
            room.sendCommand("blogin", args[0], args[1]);
        } else {
            room.sendCommand("blogin", args[0]);
        }
    }

    @Command(adminOnly = true)
    public void relog(final Room room, final User user, String[] args) {
        login(room, user, new String[]{pixie.getEngine().getCredentials().getUsername(), pixie.getEngine().getCredentials().getPassword()});
    }

    @Command(adminOnly = true)
    public void def(final Room room, final User user, String[] args) {
        String cmd = Joiner.on(' ').join(args);
        if(!cmd.contains(" as ")) {
            pixie.msgTo(room, user, "Usage: pixiedefine [word] as [definition]");
        }

        String word = cmd.split(Pattern.quote(" as "))[0];
        String msg = cmd.split(Pattern.quote(" as "), 2)[1];
        Definition definition = new Definition(word.toLowerCase(), msg, user.getName(), "");
        pixie.database().saveModel(definition, new BasicDBObject("Name", definition.getName()));
        pixie.msgTo(room, user, word + " is now defined as " + msg);
    }

    @Command(adminOnly = true)
    public void sexuality(final Room room, final User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: pixie sexuality 0-100%");
        }
        int amount = Integer.parseInt(args[0].replaceAll("[^0-9]", ""));
        if(amount >= 0 && amount <= 100) {
            pixie.getSimSimi().setSexuality(amount);
            pixie.msgTo(room, user, "Set sexuality to " + amount + "%");
        } else {
            pixie.msgTo(room, user, "Amount must be between 0-100%");
        }
    }
}

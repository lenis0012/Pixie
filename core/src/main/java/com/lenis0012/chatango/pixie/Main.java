package com.lenis0012.chatango.pixie;

import com.google.gson.JsonPrimitive;
import com.lenis0012.chatango.bot.ChatangoAPI;
import com.lenis0012.chatango.bot.api.*;
import com.lenis0012.chatango.bot.engine.Engine;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.events.ConnectEvent;
import com.lenis0012.chatango.bot.events.EventHandler;
import com.lenis0012.chatango.bot.events.MessageReceiveEvent;
import com.lenis0012.chatango.bot.utils.AuthException;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.commands.Admin;
import com.lenis0012.chatango.pixie.commands.Fun;
import com.lenis0012.chatango.pixie.commands.General;
import com.lenis0012.chatango.pixie.entities.CommandModel;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.mongodb.BasicDBObject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Main implements EventListener {

    public static void main(String[] args) throws Exception {
        if(args.length < 3) {
            throw new RuntimeException("Missing arguments! [userame] [password] [rooms]");
        }
        String username = args[0];
        String password = args[1];
        List<String> rooms = Arrays.asList(args[2].split(","));
        new Main(username, password, rooms);
    }

    private Set<CommandInfo> commands = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Engine engine;
    private final Pixie pixie;
    private long nextSimSimi = 0L;

    public Main(String username, String password, List<String> rooms) throws AuthException {
        this.engine = ChatangoAPI.createBot(username, password);
        this.pixie = new Pixie(engine, commands);
        engine.init(rooms);
        engine.getRooms().forEach(r -> r.getEventManager().addListener(this));
        engine.start();

        // Scan commands
        try {
            for(Class<?> clazz : Arrays.asList(Admin.class, Fun.class, General.class)) {
                Object instance = clazz.getConstructor(Pixie.class).newInstance(pixie);
                for(Method method : clazz.getMethods()) {
                    if(method.isAnnotationPresent(Command.class)) {
                        CommandModel model = new CommandModel(method.getName());
                        pixie.database().loadModel(model, new BasicDBObject("Name", method.getName()));
                        commands.add(new CommandInfo(instance, method, model));
                    }
                }
            }
        } catch(Exception e) {
            ChatangoAPI.getLogger().log(Level.SEVERE, "Failed to scan command classes!", e);
        }

        // PM Server
        engine.getPmManager().onMessage(e -> {
            ChatangoAPI.getLogger().log(Level.INFO, "[PM] {0}: {1}", new Object[]{e.getUser(), e.getMessage()});
            if(e.getMessage().startsWith("warnme ")) {
                String target = e.getMessage().split(" ")[1].toLowerCase();
                UserModel model = pixie.getUser(target);
                if(!model.getPuids().isEmpty()) {
                    model.addStalker(e.getUser());
                    pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
                    engine.getPmManager().message(e.getUser(), "I will warn you when " + target + " sends a message!");
                } else {
                    engine.getPmManager().message(e.getUser(), "Unknown user!");
                }
            }
        });
        engine.getPmManager().connect();
    }

    @EventHandler
    public void onMessageReceive(MessageReceiveEvent event) {
        if(pixie == null) return; // Wait until fully started
        final Room room = event.getRoom();
        final Message message = event.getMessage();
        final User user = message.getUser();

        List<String> enabled = Utils.convertList(pixie.getConfig().getList("enabled-rooms"), String.class);
        ChatangoAPI.getLogger().log(Level.INFO, "{0}: {1}", new Object[]{message.getUser().getName(), message.getText()});
        UserModel model = pixie.getUser(user.getName());
        String[] params = message.getText().split(" ");
        if(params.length > 1 && enabled.contains(room.getRoomName())) {
            String name = params[0];
            String cmd = params[1];
            if(name.equalsIgnoreCase("pixie") && !model.isBanned()) {
                String[] args = new String[params.length - 2];
                System.arraycopy(params, 2, args, 0, args.length);
                boolean cmdFound = false;
                for(CommandInfo info : commands) {
                    Command command = info.getMethod().getAnnotation(Command.class);
                    List<String> aliases = command.aliases().length > 0 ? Arrays.asList(command.aliases()) : Arrays.asList(info.getMethod().getName().toLowerCase());
                    if(aliases.contains(cmd.toLowerCase())) {
                        cmdFound = true;
                    }
                    if(System.currentTimeMillis() < info.getAllowed()) {
                        continue;
                    }
                    if(command.adminOnly() && !pixie.getUser(user.getName()).isAdmin()) {
                        continue;
                    }
                    if(aliases.contains(cmd.toLowerCase())) {
                        try {
                            info.getMethod().invoke(info.getInstance(), room, user, args);
                            info.delay(info.getModel().getDelay());
                        } catch(Exception e) {
                            ChatangoAPI.getLogger().log(Level.WARNING, "Failed to execute command!", e);
                            pixie.msgTo(room, message.getUser(), "An error occurred while running command, please report to the harem king!");
                        }
                    }
                }
                if(!cmdFound && System.currentTimeMillis() > nextSimSimi && !model.isBanned()) {
                    try {
                        String response = pixie.getSimSimi().think(message.getText().substring("pixie ".length()));
                        pixie.msgTo(room, user, response);
                    } catch(IOException e) {
                        pixie.msgTo(room, user, "Error: " + e.getMessage());
                    }

                    this.nextSimSimi = System.currentTimeMillis() + 10000L;
                }

                return; // Is command.
            }
        }

        // Truth/Dare
        if((params[0].equalsIgnoreCase("t") || params[0].equalsIgnoreCase("d")) && user.hasTag("t/d")) {
            user.removetag("t/d");
            List<String> options = params[0].equalsIgnoreCase("t") ? pixie.getTruths() : pixie.getDares();
            String msg = pixie.replaceWords(options.get(pixie.getRandom().nextInt(options.size())));
            pixie.msgTo(room, user, pixie.pickUsers(room, msg));
        }

        // Store ip
        if(!message.getIpAddress().isEmpty()) {
            if(model.addIpAddress(message.getIpAddress())) {
                pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
            }
        }

        // Store UID
        if(model.addUid(user.getUid())) {
            pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
        }

        // Warn stalkers
        List<String> stalkers = model.getStalkers();
        if(!stalkers.isEmpty()) {
            stalkers.forEach(s -> engine.getPmManager().message(s, model.getName() + " has just sent a message in " + event.getRoom().getRoomName() + "!"));
            model.clearStalkers();
            pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
        }
    }

    @EventHandler
    public void onConnect(ConnectEvent event) {
        Room room = event.getRoom();

        // Basic settings (BG, name, badge and channel)
        room.setBgEnabled(pixie.getConfig().get("style.bgEnabled", new JsonPrimitive(true)).getAsBoolean());
        room.setNameColor(new RGBColor(pixie.getConfig().get("style.nameColor", new JsonPrimitive("000")).getAsString()));
        room.setBadge(Badge.valueOf(pixie.getConfig().get("style.badge", new JsonPrimitive("NONE")).getAsString().toUpperCase()));
        room.setChannel(Channel.valueOf(pixie.getConfig().get("style.channel", new JsonPrimitive("DEFAULT")).getAsString().toUpperCase()));

        // Font
        Font.FontType fType = Font.FontType.valueOf(pixie.getConfig().get("style.font.type", new JsonPrimitive("ARIAL")).getAsString().toUpperCase());
        int fSize = pixie.getConfig().get("style.font.size", new JsonPrimitive(12)).getAsInt();
        RGBColor fColor = new RGBColor(pixie.getConfig().get("style.textColor", new JsonPrimitive("000")).getAsString());
        Font font = new Font(fType, fSize, fColor);
        font.setBold(pixie.getConfig().get("style.font.bold", new JsonPrimitive(false)).getAsBoolean());
        font.setUnderlined(pixie.getConfig().get("style.font.underlined", new JsonPrimitive(false)).getAsBoolean());
        font.setItalic(pixie.getConfig().get("style.font.italic", new JsonPrimitive(false)).getAsBoolean());

        // Apply style
        room.setDefaultFont(font);
        pixie.getConfig().save();
    }
}

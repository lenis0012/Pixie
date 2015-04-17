package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Main;
import com.lenis0012.chatango.pixie.Pixie;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Properties;

public class General {
    private static final String PROFILE_IMG_URL = "http://fp.chatango.com/profileimg/%s/%s/%s/full.jpg";
    private final Pixie pixie;
    private final Properties properties;

    @SneakyThrows(IOException.class)
    public General(Pixie pixie) {
        this.pixie = pixie;
        this.properties = new Properties();
        properties.load(Main.class.getResourceAsStream("/info.properties"));
    }


    @Command(adminOnly = true)
    public void say(Room room, User user, String[] args) {
        pixie.msg(room, Joiner.on(" ").join(args));
    }

    @Command(aliases = {"pic", "upic"})
    public void pic(Room room, User user, String[] args) {
        String name = args[0].replace("@", "").toLowerCase();
        pixie.msg(room, String.format(PROFILE_IMG_URL, name.substring(0, 1), name.substring(1, 2), name));
    }

    @Command
    public void info(Room room, User user, String[] args) {
        pixie.msg(room, "Version: " + properties.getProperty("version") + "\n" +
                "API Version: " + properties.getProperty("apiversion"));
    }

    @Command
    public void commands(Room room, User user, String[] args) {
        try {
            pixie.msgTo(room, user, pixie.getCommandsURL());
        } catch (IOException e) {
            pixie.msgTo(room, user, "Error occurred: " + e.getMessage());
        }
    }
}

package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Pixie;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class Fun {
    private static final String WATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    private final JsonParser jsonParser = new JsonParser();
    private final Pixie pixie;

    @Command(aliases = {"t/d"})
    public void truthOrDare(Room room, User user, String[] args) {
        pixie.msgTo(room, user, "T/D?");
        user.addTag("t/d");
    }

    @Command
    public void stb(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "The bottle has spun and points towards @USER"));
    }

    @Command
    public void stb2(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "The bottle has spun, @USER and @USER have to kiss!"));
    }

    @Command
    public void threesome(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "@USER @USER and @USER should have a threesome!"));
    }

    @Command(aliases = {"fuckingweather", "weather"})
    public void weather(Room room, User user, String[] args) {
        String url = Utils.urlEncode(WATHER_URL, "units", "imperial", "q", Joiner.on(' ').join(args));
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            JsonObject json = (JsonObject) jsonParser.parse(builder.toString().trim());
            if(json.has("cod") && json.get("cod").getAsInt() == 200) {
                double temp = Math.round(json.getAsJsonObject("main").get("temp").getAsDouble() * 10.0) / 10.0;
                double metric = Math.round((temp - 32) / 1.8 * 10.0) / 10.0;
                if(metric < 0) {
                    pixie.msg(room, "ITS FUCKING FREEZING!");
                } else if(metric < 15) {
                    pixie.msg(room, "ITS FUCKING COLD!");
                } else if(metric < 24) {
                    pixie.msg(room, "ITS FUCKING NICE!");
                } else {
                    pixie.msg(room, "ITS FUCKING HOT!");
                }

                pixie.msg(room, "THE FUCKING WEATHER IN " + Joiner.on(' ').join(args).toUpperCase() + " IS " + temp + "F | " + metric + "C", 500L, TimeUnit.MILLISECONDS);
            } else {
                pixie.msgTo(room, user, "I CANT GET THE FUCKING WEATHER!");
            }
        } catch(IOException e) {
            pixie.msgTo(room, user, "THE FUCKING WEATHER MODULE FAILED FUCK!");
        }
    }
}

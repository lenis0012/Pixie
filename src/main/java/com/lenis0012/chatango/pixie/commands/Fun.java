package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Pixie;
import com.lenis0012.chatango.pixie.misc.CommonUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Fun {
    private static final String YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch";
    private static final String WATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String YODA_URL = "https://yoda.p.mashape.com/yoda";
    private final JsonParser jsonParser = new JsonParser();
    private final Pixie pixie;

    @Command
    public void ship(Room room, User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: ship [user1] [optinal:user2]");
            return;
        }
        String u1 = args[0].replace("@", "");
        String u2 = null;
        if(args.length > 1) {
            u2 = args[1].replace("@", "");
        } else {
            User u = room.findUser(u1.toLowerCase());
            if(u != null) {
                String gender = u.getGender().equalsIgnoreCase("f") ? "m" : u.getGender().equalsIgnoreCase("m") ? "f" : null;
                if(gender != null) {
                    List<String> options = room.getUserList().stream().filter(us -> us.getGender().equalsIgnoreCase(gender)).map(User::getName).collect(Collectors.toList());
                    u2 = options.isEmpty() ? null : options.get(new Random().nextInt(options.size()));
                }
            }
        }

        if(u2 != null) {
            pixie.msgTo(room, user, "I ship @" + u1 + " x @" + u2);
        } else {
            pixie.msgTo(room, user, "I was unable to achieve " + u1 + " 's gender!");
        }
    }

    @Command(aliases = {"t/d"})
    public void truthOrDare(Room room, User user, String[] args) {
        pixie.msgTo(room, user, "T/D?");
        user.addTag("t/d");
    }

    @Command(adminOnly = true)
    public void stb(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "The bottle has spun and points towards @USER"));
    }

    @Command(adminOnly = true)
    public void stb2(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "The bottle has spun, @USER and @USER have to kiss!"));
    }

    @Command
    public void threesome(Room room, User user, String[] args) {
        pixie.msg(room, pixie.pickUsers(room, "@USER @USER and @USER should have a threesome!"));
    }

    @Command
    @SneakyThrows(UnirestException.class)
    public void yoda(Room room, User user, String[] args) {
        String url = Utils.urlEncode(YODA_URL, "sentence", Joiner.on(' ').join(args));
        HttpResponse<String> response = Unirest.get(url).header("X-Mashape-Key", pixie.getMashapeKey())
                .header("Accept", "text/plain").asString();
        pixie.msg(room, response.getBody());
    }

    @Command(aliases = {"whoshouldidate", "whoshouldifuck", "bestmatch"})
    public void whoshouldidate(final Room room, final User user, final String[] args) {
        String name = args.length > 0 ? args[0].replace("@", "") : user.getName();
        name = name.equalsIgnoreCase("random") ? pixie.pickUsers(room, "USER") : name;
        final String fname = name;
        pixie.msgTo(room, user, "Looking for best matches for " + name + " in this room (may take some time).");
        new Thread() {
            @Override
            @SneakyThrows
            public void run() {
                User best = null;
                int max = 0;
                for(User u : room.getUserList()) {
                    if(user.equals(u)) {
                        continue;
                    }
                    sleep(1000L);
                    String url = Utils.urlEncode("https://love-calculator.p.mashape.com/getPercentage", "fname", fname, "sname", u.getName());
                    HttpResponse<String> response = Unirest.get(url).header("X-Mashape-Key", pixie.getMashapeKey()).header("Accept", "application/json").asString();
                    JsonObject json = jsonParser.parse(response.getBody()).getAsJsonObject();
                    int percentage = Integer.parseInt(json.get("percentage").getAsString());
                    if(percentage > max) {
                        best = u;
                        max = percentage;
                    }
                }

                if(best != null) {
                    pixie.msgTo(room, user, "Best match for @" + fname + " : @" + best.getName() + " (" + max + "% match)");
                }
            }
        }.start();
    }

    @Command(aliases = {"match", "lovecheck"})
    public void love(Room room, User user, String[] args) {
        String fname = args[0].replace("@", "");
        String sname = args[1].replace("@", "");
        if(sname.equalsIgnoreCase("random")) {
            sname = pixie.pickUsers(room, "USER");
        }
        User u0 = room.findUser(fname);
        User u1 = room.findUser(sname);
        if(fname.equalsIgnoreCase("LennyHaremKing") && u1 != null && u1.getGender().equalsIgnoreCase("f")) {
            pixie.msg(room, String.format("%s and %s match for %s. %s", fname, sname, "101%", "Best match in da world."));
        } else if(sname.equalsIgnoreCase("LennyHaremKing") && u0 != null && u0.getGender().equalsIgnoreCase("f")) {
            pixie.msg(room, String.format("%s and %s match for %s. %s", fname, sname, "101%", "Best match in da world."));
        } else {
            try {
                String url = Utils.urlEncode("https://love-calculator.p.mashape.com/getPercentage", "fname", fname, "sname", sname);
                HttpResponse<String> response = Unirest.get(url).header("X-Mashape-Key", pixie.getMashapeKey()).header("Accept", "application/json").asString();
                JsonObject json = jsonParser.parse(response.getBody()).getAsJsonObject();
                String percentage = json.get("percentage").getAsString();
                String message = json.get("result").getAsString();
                pixie.msg(room, String.format("%s and %s match for %s. %s", fname, sname, percentage + "%", message));
            } catch(Exception e) {
                pixie.msgTo(room, user, String.format("@%s and @%s give an error, they probably don't match.", fname, sname));
            }
        }
    }

    //@Command
    public void match(Room room, User user, String[] args) {
        if(args.length > 1) {
            String fname = args[0].replace("@", "");
            String sname = args[1].replace("@", "");
            int result = CommonUtil.getLovePercentage(fname.toUpperCase(), sname.toUpperCase());
            String message = result >= 80 ? "Good match, this might work out!" : result >= 66 ? "Could work work out." : result >= 33 ? "You'll have to try a little harder." : "This probably won't work out.";
            pixie.msg(room, String.format("@%s and %s match for %s, %s", fname, sname, result + "%", message));
        }
    }

    @Command(aliases = {"youtube", "yt"})
    public void youtube(Room room, User user, String[] args) {
        String url = Utils.urlEncode("https://www.googleapis.com/youtube/v3/search", "order", "relevance", "part", "snippet", "type", "video", "maxResults", "1", "q", Joiner.on(' ').join(args), "key", pixie.getYtApiKey());
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();

            JsonObject json = jsonParser.parse(builder.toString()).getAsJsonObject();
            JsonArray items = json.getAsJsonArray("items");
            if(items.size() > 0) {
                JsonObject item = items.get(0).getAsJsonObject();
                String title = item.getAsJsonObject("snippet").get("title").getAsString();
                pixie.msg(room, title + "\n" + Utils.urlEncode(YOUTUBE_VIDEO_URL, "v", item.getAsJsonObject("id").get("videoId").getAsString()));
            } else {
                pixie.msgTo(room, user, "Got no results!");
            }
        } catch(IOException e) {
            pixie.msgTo(room, user, "HOLY SHIT IS YOUTUBE DOWN?");
        }
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
            e.printStackTrace();
        }
    }
}

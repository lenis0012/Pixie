package com.lenis0012.chatango.pixie.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Pixie;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.lenis0012.chatango.pixie.misc.CommonUtil;
import com.mongodb.BasicDBObject;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Fun {
    private static final String YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch";
    private static final String WATHER_URL = "http://api.openweathermap.org/data/2.5/weather";
    private static final String YODA_URL = "http://yoda-api.appspot.com/api/v1/yodish";
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
        String url = Utils.urlEncode(YODA_URL, "text", String.join(" ", args));
        HttpResponse<JsonNode> response = Unirest.get(url).header("Accept", "text/plain").asJson();
        pixie.msg(room, response.getBody().getObject().getString("yodish"));
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
                    String url = Utils.urlEncode("https://love-calculator.p.rapidapi.com/getPercentage", "fname", fname, "sname", u.getName());
                    HttpResponse<String> response = Unirest.get(url)
                            .header("X-RapidAPI-Key", pixie.getMashapeKey())
                            .header("X-RapidAPI-Host", "love-calculator.p.rapidapi.com")
                            .header("Accept", "application/json")
                            .asString();
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
                String url = Utils.urlEncode("https://love-calculator.p.rapidapi.com/getPercentage", "fname", fname, "sname", sname);
                HttpResponse<String> response = Unirest.get(url)
                        .header("X-RapidAPI-Key", pixie.getMashapeKey())
                        .header("X-RapidAPI-Host", "love-calculator.p.rapidapi.com")
                        .header("Accept", "application/json")
                        .asString();
                JsonObject json = jsonParser.parse(response.getBody()).getAsJsonObject();
                String percentage = json.get("percentage").getAsString();
                String message = json.get("result").getAsString();
                pixie.msg(room, String.format("%s and %s match for %s. %s", fname, sname, percentage + "%", message));
            } catch(Exception e) {
                pixie.msgTo(room, user, String.format("@%s and @%s give an error, they probably don't match.", fname, sname));
            }
        }
    }

    //@Command (Note: Replaced with love command)
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
        String url = Utils.urlEncode("https://www.googleapis.com/youtube/v3/search", "order", "relevance", "part", "snippet", "type", "video", "maxResults", "1", "q", String.join(" ", args), "key", pixie.getYtApiKey());
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
            e.printStackTrace();
            pixie.msgTo(room, user, "HOLY SHIT IS YOUTUBE DOWN?");
        }
    }

    @Command(aliases = {"fuckingweather", "weather"})
    public void weather(Room room, User user, String[] args) {
        String url = Utils.urlEncode(WATHER_URL, "units", "imperial", "q", String.join(" ", args), "APPID", "912bc4af2af961e57782c38596520a55");
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

                pixie.msg(room, "THE FUCKING WEATHER IN " + String.join(" ", args).toUpperCase() + " IS " + temp + "F | " + metric + "C", 500L, TimeUnit.MILLISECONDS);
            } else {
                pixie.msgTo(room, user, "I CANT GET THE FUCKING WEATHER!");
            }
        } catch(IOException e) {
            pixie.msgTo(room, user, "THE FUCKING WEATHER MODULE FAILED FUCK!");
            e.printStackTrace();
        }
    }

    @Command
    public void claim(Room room, User user, String[] args) {
        if(args.length == 0) {
            pixie.msgTo(room, user, "Pleace specify a user!");
            return;
        }

        // Validate: Exists
        String name = args[0].replace("@", "").toLowerCase();
        UserModel target = pixie.getUser(name);
        if(target.getPuids().isEmpty()) {
            pixie.msgTo(room, user, "That user does not exist in this room!");
            return;
        }

        // Validate: Available
        List<UserModel> models = pixie.database().findModels(UserModel.class, new BasicDBObject("Claimed", name));
        if(!models.isEmpty()) {
            UserModel by = models.get(0);
            pixie.msgTo(room, user, "That person was already claimed by @" + by.getName());
            return;
        }

        // Claim
        UserModel me = pixie.getUser(user.getName());

        if(me.getClaimed().size() >= 3) {
            pixie.msgTo(room, user, "You have already claimed the maximum of 3 people, to unlock more slots, donate to Lenny's charity foundation!");
            return;
        }

        me.addClaimed(name);
        pixie.database().saveModel(me, new BasicDBObject("Name", me.getName()));
        pixie.msgTo(room, user, "Successfully claimed " + args[0] + "!");
    }

    @Command(aliases = {"viewclaims", "myclaims"})
    public void viewclaims(Room room, User user, String[] args) {
        UserModel me = pixie.getUser(args.length > 0 ? args[0].replace("@", "").toLowerCase() : user.getName());
        if(me.getPuids().isEmpty()) {
            pixie.msgTo(room, user, "That user does not exist!");
        }
        StringBuilder builder = new StringBuilder();
        for(String owned : me.getClaimed()) {
            builder.append(owned).append(", ");
        }
        if(builder.length() > 1) {
            builder.setLength(builder.length() - 2);
        } else {
            pixie.msgTo(room, user, (args.length == 0 ? "you" : me.getName()) + " haven't claimed anyone");
            return;
        }
        pixie.msgTo(room, user, (args.length == 0 ? "you" : me.getName()) + " have claimed: " + builder.toString());
    }

    @Command(adminOnly = true)
    public void unclaimall(Room room, User user, String[] args) {
        List<UserModel> models = pixie.database().findModels(UserModel.class, null);
        for(UserModel model : models) {
            new ArrayList<>(model.getClaimed()).forEach(u -> model.removeClaimed(u));
            pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
        }
        pixie.msgTo(room, user, "Removed all user claims.");
        pixie.clearCache();
    }

    @Command
    public void unclaim(Room room, User user, String[] args) {
        if(args.length > 0) {
            String target = args[0].replace("@", "").toLowerCase();
            UserModel me = pixie.getUser(user.getName());
            if(!me.getClaimed().contains(target)) {
                pixie.msgTo(room, user, "You don't own " + args[0]);
                return;
            }

            me.removeClaimed(target);
            pixie.database().saveModel(me, new BasicDBObject("Name", me.getName()));
            pixie.msgTo(room, user, "You released @" + target + " from your claim!");
        } else {
            List<UserModel> models = pixie.database().findModels(UserModel.class, new BasicDBObject("Claimed", user.getName()));
            if(models.isEmpty()) {
                pixie.msgTo(room, user, "You aren't claimed by anyone.");
                return;
            }

            UserModel model = pixie.getUser(models.get(0).getName());
            model.removeClaimed(user.getName());
            pixie.database().saveModel(model, new BasicDBObject("Name", model.getName()));
            pixie.msgTo(room, user, "You have released yourself from @" + model.getName());
        }
    }
}

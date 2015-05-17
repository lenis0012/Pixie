package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.Main;
import com.lenis0012.chatango.pixie.Pixie;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class General {
    private static final String PROFILE_IMG_URL = "http://fp.chatango.com/profileimg/%s/%s/%s/full.jpg";
    private final Pixie pixie;
    private final Properties properties;
    private final JsonParser jsonParser = new JsonParser();

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
        } catch(IOException e) {
            pixie.msgTo(room, user, "Error occurred: " + e.getMessage());
        }
    }

    @Command
    public void vote(Room room, User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: vote [user]");
            return;
        }
        UserModel u0 = pixie.getUser(user.getName());
        UserModel u1 = pixie.database().findModel(UserModel.class, new BasicDBObject("Name", args[0].toLowerCase().replace("@", "")));
        if(u1 != null) {
            u1 = pixie.getUser(u1.getName());
            Calendar calendar = Calendar.getInstance();
            if(u0.getLastVote() == calendar.get(Calendar.DAY_OF_YEAR)) {
                pixie.msgTo(room, user, "You have already voted today!");
            } else {
                u1.setVotes(u1.getVotes() + 1);
                u0.setLastVote(calendar.get(Calendar.DAY_OF_YEAR));
                pixie.database().saveModel(u0, new BasicDBObject("Name", u0.getName()));
                pixie.database().saveModel(u1, new BasicDBObject("Name", u1.getName()));
                pixie.msgTo(room, user, "Voted for waifu competition user: @" + u1.getName() + " (" + u1.getVotes() + " votes)");
            }
        } else {
            pixie.msgTo(room, user, "Couldn't find user!");
        }
    }

    @Command
    public void topwaifu(Room room, User user, String[] args) {
        StringBuilder builder = new StringBuilder("Top 5 waifu's:\n");
        DBCollection collection = pixie.database().getCollection("users");
        DBCursor cursor = collection.find().sort(new BasicDBObject("Votes", -1)).limit(5);
        int i = 1;
        while(cursor.hasNext()) {
            BasicDBObject obj = (BasicDBObject) cursor.next();
            builder.append(i).append(". ").append(obj.getString("Name")).append(" (").append(obj.getInt("Votes", 0)).append(" votes)").append("\n");
            i += 1;
        }
        pixie.msg(room, builder.toString());
    }

    @Command
    public void lookup(Room room, User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: lookup [user]");
            return;
        }
        String name = args[0].replace("@", "").toLowerCase();
        User u = room.findUser(name);
        if(u != null) {
            String age = u.getBirth() == null ? "?" : String.valueOf(ageByBirth(u.getBirth()));
            pixie.msg(room, "Info about @" + name + "\n" +
                    "Gender: " + u.getGender() + "\n" +
                    "Age: " + age + "\n" +
                    "Location: " + u.getCountry());
        } else {
            pixie.msgTo(room, user, "That user isn't online!");
        }
    }

    @Command
    public void kitten(Room room, User user, String[] args) {
        int width = pixie.getRandom().nextInt(520) + 200;
        int height = 300;// pixie.getRandom().nextInt(520) + 200;
        pixie.msg(room, String.format("http://placekitten.com/g/%s/%s?lol=.png", width, height)); // Add ?lol=.png to bypass chatango image filter
    }

    @Command
    public void track(final Room room, final User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: track [user]");
            return;
        }
        pixie.getEngine().getPmManager().track(args[0].toLowerCase().replace("@", ""), e -> {
            if(e.isOnline()) {
                pixie.msgTo(room, user, String.format("%s is online (%s min idle)", e.getUser(), e.getIdle()));
            } else {
                pixie.msgTo(room, user, e.getUser() + " is offline.");
            }
        });
    }

    @Command
    @SneakyThrows(UnirestException.class)
    public void facedetect(Room room, User user, String[] args) {
        if(args.length < 1) {
            pixie.msgTo(room, user, "Usage: facedetect [image url]");
            return;
        }
        if(!args[0].startsWith("http://") && !args[0].startsWith("https://")) {
            pixie.msgTo(room, user, "That doesn't look like a valid URL (http/https)");
            return;
        }
        if(!args[0].endsWith(".png") && !args[0].endsWith(".jpg")) {
            pixie.msgTo(room, user, "Invalid image type (png/jpg)");
            return;
        }
        String url = Utils.urlEncode("https://apicloud-facerect.p.mashape.com/process-url.json", "features", "false", "url", args[0]);
        HttpResponse<String> response = Unirest.get(url)
                .header("X-Mashape-Key", pixie.getMashapeKey())
                .header("Accept", "application/json").asString();
        if(response.getCode() == 200) {
            JsonObject json = jsonParser.parse(response.getBody()).getAsJsonObject();
            JsonArray faces = json.getAsJsonArray("faces");
            if(faces.size() > 0) {
                pixie.msgTo(room, user, "Found " + faces.size() + " faces in image, now processing...");
                try {
                    BufferedImage image = ImageIO.read(new URL(args[0]));
                    BufferedImage trollface = ImageIO.read(Main.class.getResourceAsStream("/trollface.png"));
                    Graphics2D g = image.createGraphics();
                    for(int i = 0; i < faces.size(); i++) {
                        JsonObject face = faces.get(i).getAsJsonObject();
                        int x = face.get("x").getAsInt();
                        int y = face.get("y").getAsInt();
                        int width = (int) (face.get("width").getAsInt());
                        int height = (int) (face.get("height").getAsInt());
                        x -= (int) (width * 0.25);
                        y -= (int) (height * 0.25);
                        width *= 1.5;
                        height *= 1.5;
                        g.drawImage(trollface, x, y, width, height, null);
                    }

                    // Encode image to Base64
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", baos);
                    byte[] bytes = baos.toByteArray();
//                    String encoded = Base64.encode(bytes);
                    String encoded = new String(Base64.encodeBase64(bytes));
                    String postParams = URLEncoder.encode("image", "UTF-8") + "=" +
                            URLEncoder.encode(encoded, "UTF-8") + "&" +
                            URLEncoder.encode("type", "UTF-8") + "=" +
                            URLEncoder.encode("base64", "UTF-8");

                    // Upload to imgur
                    HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.imgur.com/3/image").openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Client-ID " + pixie.getImgurApiKey());
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);

                    DataOutputStream output = new DataOutputStream(connection.getOutputStream());
                    output.writeBytes(postParams);
                    output.flush();
                    output.close();

                    String line;
                    StringBuilder builder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    reader.close();
                    json = jsonParser.parse(builder.toString()).getAsJsonObject();
                    int cod = json.get("status").getAsInt();
                    if(cod == 200) {
                        JsonObject data = json.getAsJsonObject("data");
                        String link = data.get("link").getAsString();
                        pixie.msg(room, link);
                    } else {
                        pixie.msgTo(room, user, "Failed to upload image, invalid response code: " + cod);
                    }
                } catch(Exception e) {
                    pixie.msg(room, "Failed to process image: " + e.getMessage(), 1L, TimeUnit.SECONDS);
                }
            } else {
                pixie.msgTo(room, user, "Couldn't find any faces!");
            }
        } else {
            pixie.msgTo(room, user, "Invalid response code: " + response.getCode());
        }
    }

    public int ageByBirth(Date birth) {
        Calendar dob = Calendar.getInstance();
        dob.setTime(birth);
        Calendar now = Calendar.getInstance();
        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if(now.get(Calendar.MONTH) < dob.get(Calendar.MONTH)) {
            age--;
        } else if(now.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && now.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH)) {
            age--;
        }

        return age;
    }
}

package com.lenis0012.chatango.pixie.commands;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lenis0012.chatango.bot.api.User;
import com.lenis0012.chatango.bot.engine.Room;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Command;
import com.lenis0012.chatango.pixie.CommandInfo;
import com.lenis0012.chatango.pixie.Pixie;
import com.lenis0012.chatango.pixie.entities.UserModel;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.BasicDBObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class Admin {
    private static final String URBAN_DIR_URL = "https://mashape-community-urban-dictionary.p.mashape.com/define";
    private final JsonParser jsonParser = new JsonParser();
    private final Pixie pixie;

    @Command(adminOnly = true)
    @SneakyThrows(UnirestException.class)
    public void define(Room room, User user, String[] args) {
        String url = Utils.urlEncode(URBAN_DIR_URL, "term", Joiner.on(' ').join(args));
        HttpResponse<String> response = Unirest.get(url).header("X-Mashape-Key", Pixie.MASHAPE_KEY)
                .header("Accept", "text/plain").asString();
        JsonObject object = (JsonObject) jsonParser.parse(response.getBody());
        if(object.has("list")) {
            JsonArray list = object.getAsJsonArray("list");
            if(list.size() > 0) {
                JsonObject entry = list.get(0).getAsJsonObject();
                String definition = entry.get("definition").getAsString();
                pixie.msg(room, "Definition: " + definition);
                if(entry.has("example")) {
                    pixie.msg(room, "Example: " + entry.get("example").getAsString(), 500L, TimeUnit.MILLISECONDS);
                }
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

    @Command
    public void admins(Room room, User user, String[] args) {
        List<UserModel> models = pixie.database().findModels(UserModel.class, new BasicDBObject("Admin", true));
        StringBuilder builder = new StringBuilder("List of admins: ");
        models.forEach(m -> builder.append(m.getName()).append(", "));
        builder.setLength(builder.length() - 2);
        pixie.msgTo(room, user, builder.toString());
    }
}

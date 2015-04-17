package com.lenis0012.chatango.pixie.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.lenis0012.chatango.bot.utils.Utils;
import com.lenis0012.chatango.pixie.Pixie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SimSimi {
    private final String API_URL = "http://api.simsimi.com/request.p?key=%s&lc=en&ft=%s&text=%s";
    private final Pixie pixie;
    private final String apiKey;
    private final JsonParser jsonParser = new JsonParser();
    private double sexuality;

    public SimSimi(Pixie pixie) {
        this.pixie = pixie;
        this.apiKey = pixie.getConfig().get("simsimi.api-key", new JsonPrimitive("")).getAsString();
        this.sexuality = pixie.getConfig().get("simsimi.sexuality", new JsonPrimitive(1.0)).getAsDouble();
    }

    public void setSexuality(int sexuality) {
        double val = sexuality / 100.0; // Percentage to double
        this.sexuality = val;
        pixie.getConfig().set("simsimi.sexuality", val);
    }

    public String think(String question) throws IOException {
        URL url = new URL(Utils.formatUrl(API_URL, apiKey, String.valueOf(sexuality), question));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();

        JsonObject response = jsonParser.parse(builder.toString()).getAsJsonObject();
        int cod = response.get("result").getAsInt();
        switch(cod) {
            case 100:
                return response.get("response").getAsString();
            case 400:
                throw new IOException("Bad request.");
            case 401:
                throw new IOException("Invalid API-KEY.");
            case 404:
                throw new IOException("Page not found.");
            case 500:
                throw new IOException("Internal server error.");
            default:
                throw new IOException("Unkown response code: " + cod);
        }
    }
}

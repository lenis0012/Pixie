package com.lenis0012.chatango.pixie.misc;

import com.google.gson.JsonObject;
import com.lenis0012.chatango.bot.utils.Utils;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Lenny on 4/4/2015.
 */
public class CommonUtil {
    public static Object instance(Class<?> clazz, Object... args) {
        try {
            for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if(constructor.getParameterTypes().length == args.length) {
                    boolean typeMismatch = false;
                    for(int i = 0; i < args.length; i++) {
                        if(!constructor.getParameterTypes()[i].isInstance(args[i])) {
                            typeMismatch = false;
                        }
                    }

                    if(!typeMismatch) {
                        if(!constructor.isAccessible()) {
                            constructor.setAccessible(true);
                        }
                        try {
                            return constructor.newInstance(args);
                        } catch(Exception e) {
                            try {
                                return clazz.newInstance();
                            } catch(Exception e1) {
                                throw new IllegalArgumentException("Invalid amount of parameters when constructing class!", e1);
                            }
                        }
                    }
                }
            }

            //Attempt to use 0 args
            try {
                return clazz.newInstance();
            } catch(Exception e) {
                throw new IllegalArgumentException("Invalid amount of parameters when constructing class!", e);
            }
        } catch(Exception e) {
            throw new IllegalArgumentException("Class constructing failed", e);
        }
    }

    public static JsonObject getBestMatch(String user, List<String> options) {
        int[] charMap0 = new int[Character.MAX_VALUE];
        int[] charMap1 = new int[Character.MAX_VALUE];
        for(int i = 0; i < user.length(); i++) {
            charMap0[user.charAt(i)]++;
        }

        String best = null;
        int max = -1;
        for(String otherUser : options) {
            Arrays.fill(charMap1, 0);
            for(int i = 0; i < otherUser.length(); i++) {
                charMap1[otherUser.charAt(i)]++;
            }
            int L = charMap0['L'] + charMap1['L'];
            int O = charMap0['O'] + charMap1['O'];
            int V = charMap0['V'] + charMap1['V'];
            int E = charMap0['E'] + charMap1['E'];
            int percentage = ((L + O) * (L + V) * (L + E) * (O + V) * (O + E) * (V + E)) % 100;
            if(percentage > max || (percentage == max && otherUser.compareTo(best) < 0)) {
                best = otherUser;
                max = percentage;
            }
        }

        // Create JSON result
        JsonObject result = new JsonObject();
        result.addProperty("user", best);
        result.addProperty("percentage", max);
        return result;
    }

    public static int getLovePercentage(String user, String otherUser) {
        int[] charMap0 = new int[Character.MAX_VALUE];
        int[] charMap1 = new int[Character.MAX_VALUE];
        for(int i = 0; i < user.length(); i++) {
            charMap0[user.charAt(i)]++;
        }
        for(int i = 0; i < otherUser.length(); i++) {
            charMap1[otherUser.charAt(i)]++;
        }
        int L = charMap0['L'] + charMap1['L'];
        int O = charMap0['O'] + charMap1['O'];
        int V = charMap0['V'] + charMap1['V'];
        int E = charMap0['E'] + charMap1['E'];
        return ((L + O) * (L + V) * (L + E) * (O + V) * (O + E) * (V + E)) % 100;
    }

    @SneakyThrows
    public static JSONObject getBGInfo(String username) {
        URL url = new URL(Utils.formatUrl("http://fp.chatango.com/profileimg/%s/%s/%s/msgbg.xml", username.substring(0, 1), username.substring(1, 2), username));
        URLConnection connection = url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return XML.toJSONObject(builder.toString());
    }
}

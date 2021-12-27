package com.example.spotter;

import org.json.JSONException;
import org.json.JSONObject;

public class ComData {
    private static String id;
    private static String token;
    private static String login;
    private static String avatar;

    public ComData(JSONObject object) throws JSONException {
        id = object.getString("id");
        token = object.getString("token");
        login = object.getString("login");
        avatar = object.getString("avatar");
    }

    public static String getId() {
        return id;
    }

    public static String getToken() {
        return token;
    }

    public static String getLogin() {
        return login;
    }

    public static String getAvatar() {
        return avatar;
    }

    public static void setAvatar(String avatar) {
        ComData.avatar = avatar;
    }
}

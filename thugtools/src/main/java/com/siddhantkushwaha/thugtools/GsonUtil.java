package com.siddhantkushwaha.thugtools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;

public class GsonUtil {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static String toGson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromGson(String json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromGson(JsonElement json, Class<T> type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromGson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromGson(JsonElement json, Type type) {
        return gson.fromJson(json, type);
    }
}
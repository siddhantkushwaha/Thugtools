package com.siddhantkushwaha.thugtools

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import java.lang.reflect.Type


object GsonUtil {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun toGson(obj: Any?): String {
        return gson.toJson(obj)
    }

    fun <T> fromGson(json: String?, type: Class<T>?): T {
        return gson.fromJson(json, type)
    }

    fun <T> fromGson(json: JsonElement?, type: Class<T>?): T {
        return gson.fromJson(json, type)
    }

    fun <T> fromGson(json: String?, type: Type?): T {
        return gson.fromJson(json, type)
    }

    fun <T> fromGson(json: JsonElement?, type: Type?): T {
        return gson.fromJson(json, type)
    }
}
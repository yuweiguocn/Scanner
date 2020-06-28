package io.github.yuweiguocn.classscanner.plugin.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder


class GsonUtils {
    private GsonUtils(){}

    static Gson getGson(){
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

}
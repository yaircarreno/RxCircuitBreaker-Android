package com.yaircarreno.rxcircuitbreaker.utils;

import android.content.SharedPreferences;
import com.yaircarreno.rxcircuitbreaker.storage.LocalPersistence;

public class SharedPreferencesStorage implements LocalPersistence {

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesStorage(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public Long getTime(String key) {
        return sharedPreferences.getLong(key, 1);
    }

    @Override
    public void saveTime(String key, Long time) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, time);
        editor.apply();
    }
}

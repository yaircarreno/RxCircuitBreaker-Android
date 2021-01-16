package com.yaircarreno.rxcircuitbreaker.storage;

public interface LocalPersistence {

    Long getTime(String key);
    void saveTime(String key, Long time);
}

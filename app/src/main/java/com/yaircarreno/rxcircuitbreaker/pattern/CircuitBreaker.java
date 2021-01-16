package com.yaircarreno.rxcircuitbreaker.pattern;

import android.util.Log;

import com.yaircarreno.rxcircuitbreaker.storage.LocalPersistence;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import io.reactivex.rxjava3.core.Single;

public class CircuitBreaker {

    private static final String TAG = "CircuitBreaker";

    private final String name;
    private final LocalPersistence localPersistence;
    private CircuitBreakerState status;
    private final long resetTimeout;
    private final long callTimeout;
    private final int attempt;

    private CircuitBreaker(Builder builder) {
        name = builder.name;
        localPersistence = builder.localPersistence;
        status = builder.status;
        resetTimeout = builder.resetTimeout;
        callTimeout = builder.callTimeout;
        attempt = builder.attempt;
    }

    public <T> Single<T> callService(Single<T> service) {
        switch (this.status) {
            case OPEN:
                return openActions();
            case HALF_OPEN:
                return halfOpenActions(service);
            case CLOSED:
                return closeActions(service);
            default:
                return service;
        }
    }

    private <T> Single<T> openActions() {
        if (shouldBeAttemptReset()) {
            attemptReset();
        } else {
            callFast();
        }
        return Single.error(new Throwable("Sorry!, CircuitBreaker is open"));
    }

    private <T> Single<T> halfOpenActions(Single<T> observable) {
        return observable
                .timeout(callTimeout, TimeUnit.SECONDS)
                .retry(attempt)
                .onErrorResumeNext(Single::error)
                .doOnError(throwable -> trip())
                .doOnSuccess(user -> reset());
    }

    private <T> Single<T> closeActions(Single<T> observable) {
        return observable
                .timeout(callTimeout, TimeUnit.SECONDS)
                .retry(attempt)
                .onErrorResumeNext(Single::error)
                .doOnError(throwable -> trip())
                .doOnSuccess(ignore -> success());
    }

    private void trip() {
        Log.d(TAG, "CircuitBreaker is now open, and will not closed for " + resetTimeout + "seconds");
        saveTimeWhenOpenOccurs();
        this.status = status.fail();
    }

    private void reset() {
        Log.d(TAG, "CircuitBreaker will be reset, now it is going to be closed");
        this.status = status.success();
    }

    private void success() {
        Log.d(TAG, "CircuitBreaker still closed");
        this.status = status.success();
    }

    private void attemptReset() {
        Log.d(TAG, "CircuitBreaker is going to be half open, this is attempt reset");
        this.status = status.success();
    }

    private void callFast() {
        Log.d(TAG, "CircuitBreaker still is open, this is a calling fast");
        this.status = status.fail();
    }

    private boolean shouldBeAttemptReset() {
        Long timeWhenOpenOccurs = localPersistence.getTime(name);
        long timeElapsed = (new Date().getTime() - timeWhenOpenOccurs) / 1000;
        Log.i(TAG, "ShouldBeAttemptReset? TimeElapsed: " + timeElapsed);
        return timeElapsed > resetTimeout;
    }

    private void saveTimeWhenOpenOccurs() {
        localPersistence.saveTime(name, new Date().getTime());
    }

    //Builder Pattern
    public static class Builder {

        private final String name;
        private final LocalPersistence localPersistence;

        private CircuitBreakerState status = CircuitBreakerState.CLOSED;
        private long resetTimeout = 10; // 10 Seconds to be OPEN state
        private long callTimeout = 2;   // 2 Seconds as timeout in each call
        private int attempt = 3;        // 3 attempts to retry the call

        public Builder(String name, LocalPersistence localPersistence) {
            this.name = name;
            this.localPersistence = localPersistence;
        }

        public Builder resetTimeout(long val) {
            resetTimeout = val;
            return this;
        }

        public Builder callTimeout(long val) {
            callTimeout = val;
            return this;
        }

        public Builder attempt(int val) {
            attempt = val;
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(this);
        }
    }

    public CircuitBreakerState getStatus() {
        return status;
    }
}

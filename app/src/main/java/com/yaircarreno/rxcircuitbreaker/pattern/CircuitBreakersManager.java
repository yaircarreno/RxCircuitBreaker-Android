package com.yaircarreno.rxcircuitbreaker.pattern;

import android.util.Log;
import com.yaircarreno.rxcircuitbreaker.storage.LocalPersistence;
import java.util.HashMap;
import java.util.Map;
import io.reactivex.rxjava3.core.Single;

public class CircuitBreakersManager {

    private static final String TAG = "CircuitBreakersManager";
    private Map<String, CircuitBreaker> circuitBreakerDirectory = new HashMap<>();

    public CircuitBreakersManager() {
        if (circuitBreakerDirectory == null) {
            circuitBreakerDirectory = new HashMap<>();
        }
    }

    public static  <T> Single<T> callWithCircuitBreaker(Single<T> service,
                                                        CircuitBreaker circuitBreaker) {
        return circuitBreaker.callService(service);
    }

    public CircuitBreaker getCircuitBreaker(String nameCircuitBreaker, LocalPersistence localPersistence) {
        if (circuitBreakerDirectory.get(nameCircuitBreaker) == null) {
            CircuitBreaker circuitBreaker = new CircuitBreaker.Builder(nameCircuitBreaker, localPersistence).build();
            circuitBreakerDirectory.put(nameCircuitBreaker, circuitBreaker);
            Log.d(TAG, "Saved a new Circuit Breaker in the directory");
        }
        return circuitBreakerDirectory.get(nameCircuitBreaker);
    }
}
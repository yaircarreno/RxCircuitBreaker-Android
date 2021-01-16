# RxCircuitBreaker in Android

This repository contains an example of the Circuit Breaker pattern implementation in Android applications.
You can also find the iOS implementation at [RxCircuitBreaker-iOS](https://github.com/yaircarreno/RxCircuitBreaker-iOS)

## Articles

- [Circuit Breaker Pattern Implemented with Rx in Mobile or Web Applications](https://www.yaircarreno.com/2021/01/circuit-breaker-pattern-implemented.html)


## States Diagram

![Circuit Breaker Pattern](https://github.com/yaircarreno/RxCircuitBreaker-Android/blob/main/screenshots/circuit-breaker-diagram.png)

## Implementation

We have *CircuitBreaker* as a main component in the pattern, like this:

```java
public class CircuitBreaker {

    ...

    private CircuitBreaker(Builder builder) {
        ...
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
    ...
}
```

For demonstration purposes, the events (reset - success) are separated in the half-open and closed states. However, the reader may notice that they are events that can be unified.

You also have the Rx wrapper for the service:

```java
	private Single<String> service(String typeSimulation) {
        return Single.create(emitter ->
                functions
                        .getHttpsCallable("simulateResponses?typeSimulation=" + typeSimulation)
                        .call()
                        .continueWith(task -> (String) Objects.requireNonNull(task.getResult()).getData())
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                emitter.onSuccess(Objects.requireNonNull(task.getResult()));
                            } else {
                                emitter.onError(Objects.requireNonNull(task.getException()));
                            }
                        }));
    }
```

Calling from the client, you could use something like this:

```java
	private void callServiceWithCircuitBreaker(String typeSimulation) {
        ...
        CircuitBreaker circuitBreaker = circuitBreakerManager.getCircuitBreaker("circuit-breaker-9", localPersistence);
        compositeDisposable.add(
                CircuitBreakersManager.callWithCircuitBreaker(this.service(typeSimulation), circuitBreaker)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                                    logs(response, circuitBreaker.getStatus().toString(), true);
                                    ...
                                },
                                throwable -> {
                                    logs(throwable.getMessage(), circuitBreaker.getStatus().toString(), false);
                                    ...
                                }));

    }
```

## Demo

![Circuit Breaker Pattern](https://github.com/yaircarreno/RxCircuitBreaker-Android/blob/main/screenshots/demo-circuit-breaker-android.gif)


## Versions of IDEs and technologies used.

- Android Studio 4.1.1 - Java 8
- RxJava 3 - RxAndroid 3
- Activities - Layouts



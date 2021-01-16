package com.yaircarreno.rxcircuitbreaker;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.functions.FirebaseFunctions;
import com.yaircarreno.rxcircuitbreaker.databinding.ActivityMainBinding;
import com.yaircarreno.rxcircuitbreaker.pattern.CircuitBreaker;
import com.yaircarreno.rxcircuitbreaker.pattern.CircuitBreakersManager;
import com.yaircarreno.rxcircuitbreaker.storage.LocalPersistence;
import com.yaircarreno.rxcircuitbreaker.utils.SharedPreferencesStorage;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private CompositeDisposable compositeDisposable;
    private FirebaseFunctions functions;
    private LocalPersistence localPersistence;
    private CircuitBreakersManager circuitBreakerManager;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        localPersistence = new SharedPreferencesStorage(this.getPreferences(Context.MODE_PRIVATE));
        initializeFunctionsClient();
        initializeCircuitBreakersManager();
        initializeCompositeDisposable();
        setUpUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    private void callServiceWithCircuitBreaker(String typeSimulation) {
        showLoading(true);
        CircuitBreaker circuitBreaker = circuitBreakerManager
                .getCircuitBreaker("circuit-breaker-9", localPersistence);
        compositeDisposable.add(
                CircuitBreakersManager.callWithCircuitBreaker(this.service(typeSimulation), circuitBreaker)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(response -> {
                                    logs(response, circuitBreaker.getStatus().toString(), true);
                                    showMessages(response, circuitBreaker.getStatus().toString());
                                    showLoading(false);
                                },
                                throwable -> {
                                    logs(throwable.getMessage(), circuitBreaker.getStatus().toString(), false);
                                    showMessages("Event called Error: " + throwable.getMessage(),
                                            circuitBreaker.getStatus().toString());
                                    showLoading(false);
                                }));

    }

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

    private void setUpUI() {
        binding.button1.setOnClickListener(view1 -> callServiceWithCircuitBreaker("Success"));
        binding.button2.setOnClickListener(view1 -> callServiceWithCircuitBreaker("Error"));
        binding.button3.setOnClickListener(view1 -> callServiceWithCircuitBreaker("Latency"));
    }

    private void initializeFunctionsClient() {
        if (functions != null) {
            return;
        }
        functions = FirebaseFunctions.getInstance();
    }

    private void initializeCircuitBreakersManager() {
        if (circuitBreakerManager != null) {
            return;
        }
        circuitBreakerManager = new CircuitBreakersManager();
    }

    private void initializeCompositeDisposable() {
        if (compositeDisposable != null) {
            return;
        }
        compositeDisposable = new CompositeDisposable();
    }

    @SuppressLint("SetTextI18n")
    private void showMessages(String messageFromAPI, String circuitBreakerState) {
        binding.messageFromApi.setText(messageFromAPI);
        binding.circuitBreakerStatus.setText("Circuit Breaker State: " + circuitBreakerState);
    }

    private void logs(String messageFromAPI, String circuitBreakerState, boolean success) {
        Log.i(TAG, success ? messageFromAPI : "Event called Error: " + messageFromAPI);
        Log.i(TAG, circuitBreakerState);
    }

    private void showLoading(boolean show) {
        binding.progressBar1.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
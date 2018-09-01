package net.androidcart.rxbusretrofit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.androidcart.rxbusretrofit.api.GitHubServiceCallback;
import net.androidcart.rxbusretrofit.api.GitHubServiceCallbackMapper;
import net.androidcart.rxbusretrofit.api.GitHubServicePublisher;

import javax.inject.Inject;

public abstract class BaseActivity extends AppCompatActivity {

    @Inject GitHubServicePublisher api;

    protected GitHubServiceCallbackMapper apiMapper;
    protected GitHubServiceCallback getApiCallback(){return null;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApp.getComponent().inject(this);

        if (apiMapper == null){
            GitHubServiceCallback callbacks = getApiCallback();
            if (callbacks != null){
                apiMapper = new GitHubServiceCallbackMapper(callbacks);
                api.subscribe(apiMapper);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (apiMapper != null){
            api.unregister(apiMapper);
        }
    }
}

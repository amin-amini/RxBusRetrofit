package net.androidcart.rxbusretrofit;

import android.app.Application;
import android.util.Log;

import net.androidcart.rxbusretrofit.api.GitHubServiceCallback;
import net.androidcart.rxbusretrofit.api.GitHubServiceCallbackMapper;
import net.androidcart.rxbusretrofit.api.GitHubServicePublisher;
import net.androidcart.rxbusretrofit.di.components.AppComponent;
import net.androidcart.rxbusretrofit.di.components.DaggerAppComponent;
import net.androidcart.rxbusretrofit.di.modules.AppModule;
import net.androidcart.rxbusretrofit.model.Repo;
import net.androidcart.rxbusretrofit.model.User;

import java.util.List;

import javax.inject.Inject;

import okhttp3.Request;
import retrofit2.Response;

/**
 * Created by Amin Amini on 8/23/18.
 */

public class MyApp extends Application {

    public static final String LOG = "RxBusRetrofit";

    private static AppComponent component;
    public static AppComponent getComponent() {
        return component;
    }

    @Inject GitHubServicePublisher api;

    private GitHubServiceCallbackMapper apiMapper;

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        component.inject(this);

        apiMapper = new GitHubServiceCallbackMapper(getApiCallback());
        api.subscribe(apiMapper);
    }

    @Override
    public void onTerminate() {
        api.unregister(apiMapper);
        super.onTerminate();
    }

    protected GitHubServiceCallback getApiCallback() {
        return new GitHubServiceCallback(){
            @Override
            public void listReposStart(Object object, Request request) {
                Log.d(MyApp.LOG, "Application: listReposStart"+
                        "\n\tobject : " + object +
                        "\n\trequest: " + request  );
            }
            @Override
            public void listReposResult(List<Repo> result, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "Application: listReposResult"+
                        "\n\t  result   : " + result +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
            @Override
            public void listReposFailure(Object error, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "Application: listReposFailure"+
                        "\n\t   error   : " + error +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }

            @Override
            public void listFollowersStart(Object object, Request request) {
                Log.d(MyApp.LOG, "Application: listFollowersStart"+
                        "\n\tobject : " + object +
                        "\n\trequest: " + request  );
            }
            @Override
            public void listFollowersResult(List<User> result, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "Application: listFollowersResult"+
                        "\n\t  result   : " + result +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
            @Override
            public void listFollowersFailure(Object error, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "Application: listFollowersFailure"+
                        "\n\t   error   : " + error +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
        };
    }
}

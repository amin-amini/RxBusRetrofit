package net.androidcart.rxbusretrofit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import net.androidcart.rxbusretrofit.api.GitHubService;
import net.androidcart.rxbusretrofit.api.GitHubServiceCallback;
import net.androidcart.rxbusretrofit.api.GitHubServicePublisher;
import net.androidcart.rxbusretrofit.model.Repo;
import net.androidcart.rxbusretrofit.model.User;

import java.util.List;

import javax.inject.Inject;

import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api.listFollowers("amin-amini");
        api.listRepos("amin-amini", "testStartObject");

        api.listRepos(apiMapper, "amin-amini", "testStartObjectLocal");

    }

    @Override
    protected GitHubServiceCallback getApiCallback() {
        return new GitHubServiceCallback(){
            @Override
            public void listReposStart(Object object, Request request) {
                Log.d(MyApp.LOG, "MainAct: listReposStart"+
                        "\n\tobject : " + object +
                        "\n\trequest: " + request  );
            }
            @Override
            public void listReposResult(List<Repo> result, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "MainAct: listReposResult"+
                        "\n\t  result   : " + result +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
            @Override
            public void listReposFailure(Object error, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "MainAct: listReposFailure"+
                        "\n\t   error   : " + error +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }

            @Override
            public void listFollowersStart(Object object, Request request) {
                Log.d(MyApp.LOG, "MainAct: listFollowersStart"+
                        "\n\tobject : " + object +
                        "\n\trequest: " + request  );
            }
            @Override
            public void listFollowersResult(List<User> result, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "MainAct: listFollowersResult"+
                        "\n\t  result   : " + result +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
            @Override
            public void listFollowersFailure(Object error, Request request, Object startObject, Response response) {
                Log.d(MyApp.LOG, "MainAct: listFollowersFailure"+
                        "\n\t   error   : " + error +
                        "\n\t  request  : " + request  +
                        "\n\tstartObject: " + startObject  );
            }
        };
    }
}

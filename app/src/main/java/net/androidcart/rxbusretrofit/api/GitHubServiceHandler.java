package net.androidcart.rxbusretrofit.api;

import android.util.Log;

import net.androidcart.rxbusretrofit.MyApp;
import net.androidcart.rxbusretrofit.api.GitHubServiceApiHandler;
import net.androidcart.rxbusretrofit.api.GitHubServiceApiType;

import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Amin Amini on 8/23/18.
 */

public class GitHubServiceHandler extends GitHubServiceApiHandler {


    GitHubServiceHandler(GitHubServicePublisher publisher) {
        super(publisher);
    }

    @Override
    void onApiStart(GitHubServiceCallbackMapper mapper, Call call, GitHubServiceApiType type, Request request, Object startObject) {
        Log.d(MyApp.LOG, "Handler: onApiStart"+
                "\n\t    type   : " + type +
                "\n\t  request  : " + request +
                "\n\tstartObject: " + startObject );
    }

    @Override
    void onApiSuccess(GitHubServiceCallbackMapper mapper, Call call, GitHubServiceApiType type, Object body, Response response, Request request, Object startObject) {
        Log.d(MyApp.LOG, "Handler: onApiSuccess"+
                "\n\t    type   : " + type +
                "\n\t    body   : " + body +
                "\n\t response  : " + response +
                "\n\t  request  : " + request +
                "\n\tstartObject: " + startObject );
    }

    @Override
    void onApiFailure(GitHubServiceCallbackMapper mapper, Call call, GitHubServiceApiType type, ResponseBody errorBody, Response response, Request request, Object startObject) {
        Log.d(MyApp.LOG, "Handler: onApiSuccess"+
                "\n\t    type   : " + type +
                "\n\t errorBody : " + errorBody +
                "\n\t response  : " + response +
                "\n\t  request  : " + request +
                "\n\tstartObject: " + startObject );
        retry(mapper, call,type,startObject);
    }

    @Override
    void onGeneralFailure(GitHubServiceCallbackMapper mapper, Call call, GitHubServiceApiType type, Throwable throwable, Request request, Object startObject) {
        Log.d(MyApp.LOG, "Handler: onApiSuccess"+
                "\n\t    type   : " + type +
                "\n\t throwable : " + throwable +
                "\n\t  request  : " + request +
                "\n\tstartObject: " + startObject );
        retry(mapper, call,type,startObject);
    }
}

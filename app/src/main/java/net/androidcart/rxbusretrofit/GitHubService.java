package net.androidcart.rxbusretrofit;

import net.androidcart.rxbusretrofit.model.Repo;
import net.androidcart.rxbusretrofitschema.RxBusRetrofitSchema;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Amin Amini on 8/23/18.
 */

@RxBusRetrofitSchema(handler = GitHubServiceHandler.class)
public interface GitHubService {
    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos(@Path("user") String user);

    @GET("users/{user}/repos")
    Call<List<Repo>> listRepos2(@Path("user") String user);
}

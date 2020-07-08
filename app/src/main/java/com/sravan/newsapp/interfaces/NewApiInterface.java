package com.sravan.newsapp.interfaces;

import com.sravan.newsapp.model.NewsModel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface NewApiInterface {

    @GET("top-headlines")
    Call<NewsModel> getNews(@Query("country") String country, @Query("apiKey") String apiKey);
}

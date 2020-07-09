package com.sravan.newsapp.ui.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;
import com.sravan.newsapp.ui.adapters.NewsAdapter;
import com.sravan.newsapp.R;
import com.sravan.newsapp.interfaces.NewApiInterface;
import com.sravan.newsapp.model.Article;
import com.sravan.newsapp.model.NewsModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private NewsAdapter newsAdapter;
    private List<Article> newsModelList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<Object> mDataSet;

    // The number of native ads to load.
    public static final int NUMBER_OF_ADS = 5;

    // The AdLoader used to load ads.
    private AdLoader adLoader;

    // List of native ads that have been successfully loaded.
    private List<UnifiedNativeAd> mNativeAds = new ArrayList<>();

    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean adMobEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        //progressBar.setVisibility(View.GONE);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mDataSet = new ArrayList<>();

        checkPermission();

        MobileAds.initialize(this);

        String uniqueID = UUID.randomUUID().toString();

        List<String> testDeviceIds = Arrays.asList(uniqueID);
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);

        FirebaseApp.initializeApp(getApplicationContext());

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(MainActivity.this);

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        //mFirebaseRemoteConfig.setConfigSettingsAsync(configBuilder.build());
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        FirebaseRemoteConfigValue value = mFirebaseRemoteConfig.getValue("Enable_Admob");
        System.out.println("LOG:: Admob value:: " + value.asBoolean());

        if(value.asBoolean())
            adMobEnabled = true;
        else
            adMobEnabled = false;


        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean res = task.getResult();

                            checkPermission();

                        } else {
                            Toast.makeText(MainActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                            checkPermission();
                        }
                    }
                });

        OneSignal.startInit(this).setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
            @Override
            public void notificationOpened(OSNotificationOpenResult result) {
                String data = result.notification.payload.launchURL;

                Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
                intent.putExtra("Url", data);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }).inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification).init();
    }

    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            progressBar.setVisibility(View.VISIBLE);
            parseJson();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        progressBar.setVisibility(View.VISIBLE);
                        parseJson();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }
                return;
            }
        }

    }

    private void parseJson() {

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String country = telephonyManager.getNetworkCountryIso();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://newsapi.org/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NewApiInterface newApiInterface = retrofit.create(NewApiInterface.class);
        Call<NewsModel> listCall = newApiInterface.getNews(country, "fc3b342f3c984a7791f695654122859b");
        listCall.enqueue(new Callback<NewsModel>() {
            @Override
            public void onResponse(Call<NewsModel> call, Response<NewsModel> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    newsModelList = new ArrayList<>(response.body().getArticles());
                    loadData(newsModelList);
                }
            }

            @Override
            public void onFailure(Call<NewsModel> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                System.out.println("LOG:: Error:: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Oops!!! Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
        Method to load data in mDataSet - the list that will contain data items and Ad Units
    */

    public void loadData(List<Article> articles) {

        System.out.println("LOG:: Dataset size before articles: "+mDataSet.size());
        mDataSet.addAll(articles);
        System.out.println("LOG:: Dataset size after articles: "+mDataSet.size());
        if (adMobEnabled) {
            progressBar.setVisibility(View.VISIBLE);
            loadNativeAds();
        }
        System.out.println("LOG:: Dataset size after loading ads: "+mDataSet.size());
        newsAdapter = new NewsAdapter(MainActivity.this, mDataSet, newsModelList, adMobEnabled);
        recyclerView.setAdapter(newsAdapter);
    }

    private void insertAdsInMenuItems() {
        if (mNativeAds.size() <= 0) {
            return;
        }

        int offset = (mDataSet.size() / mNativeAds.size()) + 1;
        int index = 0;
        for (UnifiedNativeAd ad : mNativeAds) {
            mDataSet.add(index, ad);
            index = index + offset;
        }
    }

    private void loadNativeAds() {

        AdLoader.Builder builder = new AdLoader.Builder(this, getString(R.string.ad_unit_id));
        adLoader = builder.forUnifiedNativeAd(
                new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        // A native ad loaded successfully, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        mNativeAds.add(unifiedNativeAd);
                        System.out.println("LOG:: Native Ads size: "+mNativeAds.size());
                        if (!adLoader.isLoading()) {
                            progressBar.setVisibility(View.GONE);
                            insertAdsInMenuItems();
                        }
                    }
                }).withAdListener(
                new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // A native ad failed to load, check if the ad loader has finished loading
                        // and if so, insert the ads into the list.
                        Log.e("MainActivity", "The previous native ad failed to load. Attempting to"
                                + " load another.");
                        if (!adLoader.isLoading()) {
                            progressBar.setVisibility(View.GONE);
                            insertAdsInMenuItems();
                        }
                    }
                }).build();

        // Load the Native ads.
        adLoader.loadAds(new AdRequest.Builder().build(), NUMBER_OF_ADS);
    }
}
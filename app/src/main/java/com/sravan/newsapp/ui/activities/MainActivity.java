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
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.RequestConfiguration;
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

    public final static int spaceBetweenAds = 10;

    private FirebaseAnalytics mFirebaseAnalytics;
    private boolean adMobEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mDataSet = new ArrayList<>();

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

        //String locale = this.getResources().getConfiguration().locale.getCountry();
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

        mDataSet.addAll(articles);
        if (adMobEnabled)
            addNativeExpressAds();
        newsAdapter = new NewsAdapter(MainActivity.this, mDataSet, spaceBetweenAds, newsModelList, adMobEnabled);
        recyclerView.setAdapter(newsAdapter);
    }

    /*
        Method to add Native Express Ads to our Original Dataset
    */
    private void addNativeExpressAds() {
        for (int i = spaceBetweenAds; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
            NativeExpressAdView adView = new NativeExpressAdView(this);
            //adView.setAdUnitId("ca-app-pub-2264431215397445/7991876442");
            adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
            mDataSet.add(i, adView);
        }
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                float scale = MainActivity.this.getResources().getDisplayMetrics().density;
                int adWidth = (int) (recyclerView.getWidth() - (2 * MainActivity.this.getResources().getDimension(R.dimen.activity_horizontal_margin)));
                AdSize adSize = new AdSize((int) (adWidth / scale), 150);
                for (int i = spaceBetweenAds; i <= mDataSet.size(); i += (spaceBetweenAds + 1)) {
                    NativeExpressAdView adViewToSize = (NativeExpressAdView) mDataSet.get(i);
                    adViewToSize.setAdSize(adSize);
                }
                loadNativeExpressAd(spaceBetweenAds);
            }
        });

    }

    private void loadNativeExpressAd(final int index) {

        if (index >= mDataSet.size()) {
            return;
        }

        Object item = mDataSet.get(index);
        if (!(item instanceof NativeExpressAdView)) {
            throw new ClassCastException("Expected item at index " + index + " to be a Native"
                    + " Express ad.");
        }

        final NativeExpressAdView adView = (NativeExpressAdView) item;

        // Set an AdListener on the NativeExpressAdView to wait for the previous Native Express ad
        // to finish loading before loading the next ad in the items list.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                // The previous Native Express ad loaded successfully, call this method again to
                // load the next ad in the items list.
                loadNativeExpressAd(index + spaceBetweenAds + 1);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // The previous Native Express ad failed to load. Call this method again to load
                // the next ad in the items list.
                Log.e("AdmobMainActivity", "The previous Native Express ad failed to load. Attempting to"
                        + " load the next Native Express ad in the items list.");
                loadNativeExpressAd(index + spaceBetweenAds + 1);
            }
        });

        // Load the Native Express ad.
        //We also registering our device as Test Device with addTestDevic("ID") method
        adView.loadAd(new AdRequest.Builder().addTestDevice("YOUR_TEST_DEVICE_ID").build());
    }
}

//mFirebaseRemoteConfig.fetchAndActivate()
//        .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
//@Override
//public void onComplete(@NonNull Task<Boolean> task) {
//        if (task.isSuccessful()) {
//        boolean updated = task.getResult();
//
//        Toast.makeText(MainActivity.this, "Fetch and activate succeeded",
//        Toast.LENGTH_SHORT).show();
//
//        } else {
//        Toast.makeText(MainActivity.this, "Fetch failed",
//        Toast.LENGTH_SHORT).show();
//        }
//        //displayWelcomeMessage();
//        }
//        });
//
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

//        Bundle bundle = new Bundle();
//        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
//        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "sravan");
//        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
//        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
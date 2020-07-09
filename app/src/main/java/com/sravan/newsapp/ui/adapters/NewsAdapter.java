package com.sravan.newsapp.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.NativeExpressAdView;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.sravan.newsapp.R;
import com.sravan.newsapp.model.Article;
import com.sravan.newsapp.ui.UnifiedNativeAdViewHolder;
import com.sravan.newsapp.ui.activities.MainActivity;
import com.sravan.newsapp.ui.activities.WebViewActivity;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int DATA_VIEW_TYPE = 0;
    private static final int NATIVE_EXPRESS_AD_VIEW_TYPE = 1;

    private List<Object> mDataset;

    private List<Article> newsModels;
    private Context context;

    private boolean adMobEnabled;

    public NewsAdapter(MainActivity mainActivity, List<Object> dataSet, List<Article> newsModelList, boolean adMobEnabled) {
        this.newsModels = newsModelList;
        this.context = mainActivity.getApplicationContext();
        this.mDataset = dataSet;
        this.adMobEnabled = adMobEnabled;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Switch Case for creating ViewHolder based on viewType
        switch (viewType) {
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                View unifiedNativeLayoutView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.ad_unified,
                        parent, false);
                return new UnifiedNativeAdViewHolder(unifiedNativeLayoutView);
            case DATA_VIEW_TYPE:


            default:
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
                return new DataViewHolder(view);

        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {



        int viewType = getItemViewType(position);

        // Binding data based on View Type
        switch (viewType) {

            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                // fall through
                System.out.println("LOG: Data set Count:: "+mDataset.size());
                UnifiedNativeAd nativeAd = (UnifiedNativeAd) mDataset.get(position);
                populateNativeAdView(nativeAd,((UnifiedNativeAdViewHolder) holder).getAdView());
                break;
            case DATA_VIEW_TYPE:

            default:
                DataViewHolder viewHolder = (DataViewHolder) holder;

                Article article = (Article) mDataset.get(position);
                viewHolder.tvName.setText(article.getTitle());
                viewHolder.tvDesc.setText(article.getDescription());
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * Determines the view type for the given position.
     */
    @Override
    public int getItemViewType(int position) {

        if(adMobEnabled) {
            Object recyclerViewItem = mDataset.get(position);
            if (recyclerViewItem instanceof UnifiedNativeAd) {
                return NATIVE_EXPRESS_AD_VIEW_TYPE;
            }
            return DATA_VIEW_TYPE;
        } else
            return DATA_VIEW_TYPE;
    }


    public class DataViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName, tvDesc;
        private CardView cardView_ad;

        public DataViewHolder(@NonNull final View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.news_name);
            tvDesc = itemView.findViewById(R.id.news_desc);

            //cardView_ad = itemView.findViewById(R.id.ad_card_view);

            for(Object object: mDataset){
                System.out.println("LOG:: Dataset item: "+object);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(itemView.getContext(), WebViewActivity.class);
                    i.putExtra("Url", newsModels.get(getAdapterPosition()).getUrl());
                    itemView.getContext().startActivity(i);
                }
            });
        }
    }

    private void populateNativeAdView(UnifiedNativeAd nativeAd,
                                      UnifiedNativeAdView adView) {
        // Some assets are guaranteed to be in every UnifiedNativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        NativeAd.Image icon = nativeAd.getIcon();

        if (icon == null) {
            adView.getIconView().setVisibility(View.INVISIBLE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(icon.getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // Assign native ad object to the native view.
        adView.setNativeAd(nativeAd);
    }
}

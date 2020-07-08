package com.sravan.newsapp.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.NativeExpressAdView;
import com.sravan.newsapp.R;
import com.sravan.newsapp.model.Article;
import com.sravan.newsapp.ui.activities.MainActivity;
import com.sravan.newsapp.ui.activities.WebViewActivity;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int DATA_VIEW_TYPE = 0;
    private static final int NATIVE_EXPRESS_AD_VIEW_TYPE = 1;

    private List<Object> mDataset = new ArrayList<>();
    private int spaceBetweenAds;

    private List<Article> newsModels;
    private Context context;

    private boolean adMobEnabled;

    public NewsAdapter(List<Article> newsModelList, MainActivity mainActivity) {
        newsModels = newsModelList;
        context = mainActivity.getApplicationContext();

    }

    public NewsAdapter(MainActivity mainActivity, List<Object> dataSet, int spaceBetweenAds, List<Article> newsModelList, boolean adMobEnabled) {
        this.newsModels = newsModelList;
        this.context = mainActivity.getApplicationContext();
        this.spaceBetweenAds = spaceBetweenAds;
        this.mDataset.addAll(dataSet);
        this.adMobEnabled = adMobEnabled;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Switch Case for creating ViewHolder based on viewType
        switch (viewType) {
            case DATA_VIEW_TYPE:
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
                return new DataViewHolder(view);
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                // fall through
            default:
                View nativeExpressLayoutView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.item_layout_ad,
                        parent, false);
                return new NativeExpressAdViewHolder(nativeExpressLayoutView);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {



        int viewType = getItemViewType(position);

        // Binding data based on View Type
        switch (viewType) {
            case DATA_VIEW_TYPE:
                DataViewHolder viewHolder = (DataViewHolder) holder;

                Article article = (Article) mDataset.get(position);
                viewHolder.tvName.setText(article.getTitle());
                viewHolder.tvDesc.setText(article.getDescription());

                break;
            case NATIVE_EXPRESS_AD_VIEW_TYPE:
                // fall through
            default:
                NativeExpressAdViewHolder nativeExpressHolder = (NativeExpressAdViewHolder) holder;
                NativeExpressAdView adView = (NativeExpressAdView) mDataset.get(position);
                ViewGroup adCardView = (ViewGroup) nativeExpressHolder.itemView;

                if (adCardView.getChildCount() > 0) {
                    adCardView.removeAllViews();
                }
                if (adView.getParent() != null) {
                    ((ViewGroup) adView.getParent()).removeView(adView);
                    nativeExpressHolder.view.setVideoOptions(adView.getVideoOptions());
                }
                adCardView.addView(adView);

        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Logic for returning view type based on spaceBetweenAds variable
        // Here if remainder after dividing the position with (spaceBetweenAds + 1) comes equal to spaceBetweenAds,
        // then return NATIVE_EXPRESS_AD_VIEW_TYPE otherwise DATA_VIEW_TYPE
        // By the logic defined below, an ad unit will be showed after every spaceBetweenAds numbers of data items
        if(adMobEnabled)
            return (position % (spaceBetweenAds + 1) == spaceBetweenAds) ? NATIVE_EXPRESS_AD_VIEW_TYPE: DATA_VIEW_TYPE;
        else
            return DATA_VIEW_TYPE;
    }


    public class DataViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName, tvDesc;
        private CardView cardView_ad;

        public DataViewHolder(@NonNull final View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.news_name);
            tvDesc = itemView.findViewById(R.id.news_desc);

            cardView_ad = itemView.findViewById(R.id.ad_card_view);

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

    // View Holder for Admob Native Express Ad Unit
    public static class NativeExpressAdViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView_ad;
        private NativeExpressAdView view;
        NativeExpressAdViewHolder(View view) {
            super(view);
            cardView_ad = itemView.findViewById(R.id.ad_card_view);
            view = itemView.findViewById(R.id.view);
        }
    }
}

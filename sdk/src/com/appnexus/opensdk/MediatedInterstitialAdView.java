package com.appnexus.opensdk;

import com.appnexus.opensdk.MediatedInterstitialAdViewController;

import android.app.Activity;
import android.view.View;

public interface MediatedInterstitialAdView extends MediatedAdView{
    public void requestAd(MediatedInterstitialAdViewController mIC, Activity activity, String parameter, String uid);

    public void show();
}
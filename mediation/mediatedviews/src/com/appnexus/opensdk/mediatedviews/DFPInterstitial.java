/*
 *    Copyright 2013 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.appnexus.opensdk.mediatedviews;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import com.appnexus.opensdk.MediatedAdViewController;
import com.appnexus.opensdk.MediatedInterstitialAdView;
import com.appnexus.opensdk.MediatedInterstitialAdViewController;
import com.appnexus.opensdk.TargetingParameters;
import com.appnexus.opensdk.utils.Clog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.google.android.gms.plus.model.people.Person;

/**
 * This class is the Google DFP interstitial adaptor it provides the functionality needed to allow
 * an application using the App Nexus SDK to load a banner ad through the Google DFP SDK. The instantiation
 * of this class is done in response from the AppNexus server for a banner placement that is configured
 * to use AdMob to serve it. This class is never instantiated by the developer.
 *
 * This class also serves as an example of how to write a Mediation adaptor for the AppNexus
 * SDK.
 *
 */
public class DFPInterstitial implements MediatedInterstitialAdView {

    private final AdListener adListener = new AdListener(){
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            Clog.d(Clog.mediationLogTag, "DFPInterstitial - onAdClosed");
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            super.onAdFailedToLoad(errorCode);
            Clog.d(Clog.mediationLogTag, String.format("DFPInterstitial - onAdFailedToLoad: with errorCode: %s", errorCode));

            MediatedAdViewController.RESULT code = MediatedAdViewController.RESULT.INTERNAL_ERROR;

            switch (errorCode) {
                case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                    code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                    break;
                case AdRequest.ERROR_CODE_INVALID_REQUEST:
                    code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                    break;
                case AdRequest.ERROR_CODE_NETWORK_ERROR:
                    code = MediatedAdViewController.RESULT.NETWORK_ERROR;
                    break;
                case AdRequest.ERROR_CODE_NO_FILL:
                    code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                    break;
                default:
                    break;
            }

            if (mMediatedInterstitialAdViewController != null) {
                mMediatedInterstitialAdViewController.onAdFailed(code);
            }
        }

        @Override
        public void onAdLeftApplication() {
            super.onAdLeftApplication();
            Clog.d(Clog.mediationLogTag, "DFPInterstitial - onAdLeftApplication");
            if (mMediatedInterstitialAdViewController != null) {
                mMediatedInterstitialAdViewController.onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {
            super.onAdOpened();
            Clog.d(Clog.mediationLogTag, "DFPInterstitial - onAdOpened");
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            mMediatedInterstitialAdViewController.onAdLoaded();
        }
    };

    private InterstitialAd iad;
    private MediatedInterstitialAdViewController mMediatedInterstitialAdViewController;

    public DFPInterstitial() {
        super();
    }

    @Override
    public void requestAd(MediatedInterstitialAdViewController mIC, Activity activity, String parameter, String uid, TargetingParameters targetingParameters) {
        Clog.d(Clog.mediationLogTag, String.format("DFPInterstitial - requesting an ad: [%s, %s]", parameter, uid));

        iad = new InterstitialAd(activity);
        iad.setAdUnitId(uid);

        AdRequest.Builder builder = new AdRequest.Builder();

        switch(targetingParameters.getGender()){
            case UNKNOWN:
                break;
            case FEMALE:
                builder.setGender(Person.Gender.FEMALE);
                break;
            case MALE:
                builder.setGender(Person.Gender.MALE);
                break;
        }


        final Bundle bundle = new Bundle();
        if(targetingParameters.getAge()!=null){
            bundle.putString("Age", targetingParameters.getAge());
        }
        if(targetingParameters.getLocation()!=null){
            builder.setLocation(targetingParameters.getLocation());
        }
        for(Pair<String, String> p : targetingParameters.getCustomKeywords()){
            bundle.putString(p.first, p.second);
        }

        builder.addNetworkExtras(new AdMobExtras(bundle));

        iad.setAdListener(adListener);

        mMediatedInterstitialAdViewController = mIC;
        iad.loadAd(builder.build());
    }

    @Override
    public void show() {
        Clog.d(Clog.mediationLogTag, "DFPInterstitial - show called");
        if (iad == null) {
            Clog.e(Clog.mediationLogTag, "DFPInterstitial - show called while interstitial ad view was null");
            return;
        }
        if (!iad.isLoaded()) {
            Clog.e(Clog.mediationLogTag, "DFPInterstitial - show called while interstitial ad was not ready");
            return;
        }

        iad.show();
        Clog.d(Clog.mediationLogTag, "DFPInterstitial - interstitial ad shown");
    }

    @Override
    public boolean isReady() {
        return (iad != null) && (iad.isLoaded());
    }

}

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
import android.view.View;

import com.appnexus.opensdk.MediatedAdViewController;
import com.appnexus.opensdk.MediatedBannerAdView;
import com.appnexus.opensdk.MediatedBannerAdViewController;
import com.appnexus.opensdk.TargetingParameters;
import com.appnexus.opensdk.utils.Clog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.google.android.gms.plus.model.people.Person;

/**
 * This class is the Google AdMob banner adaptor it provides the functionality needed to allow
 * an application using the App Nexus SDK to load a banner ad through the Google SDK. The instantiation
 * of this class is done in response from the AppNexus server for a banner placement that is configured
 * to use AdMob to serve it. This class is never instantiated by the developer.
 * <p>
 * This class also serves as an example of how to write a Mediation adaptor for the AppNexus
 * SDK.
 *
 */
public class AdMobBanner implements MediatedBannerAdView {

    private final AdListener adListener = new AdListener() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            Clog.d(Clog.mediationLogTag, "AdMobBanner - onAdClosed");
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            super.onAdFailedToLoad(errorCode);
            Clog.d(Clog.mediationLogTag, String.format("AdMobBanner - onAdFailedToLoad: errorCode: %s", errorCode));

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

            if (mMediatedBannerAdViewController != null) {
                mMediatedBannerAdViewController.onAdFailed(code);
            }
        }

        @Override
        public void onAdLeftApplication() {
            super.onAdLeftApplication();
            Clog.d(Clog.mediationLogTag, "AdMobBanner - onAdLeftApplication");
            if (mMediatedBannerAdViewController != null) {
                mMediatedBannerAdViewController.onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {
            super.onAdOpened();
            Clog.d(Clog.mediationLogTag, "AdMobBanner - onAdOpened");
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();
            Clog.d(Clog.mediationLogTag, "AdMobBanner - onAdLoaded");
            if (mMediatedBannerAdViewController != null) {
                mMediatedBannerAdViewController.onAdLoaded();
            }
        }
    };

    private MediatedBannerAdViewController mMediatedBannerAdViewController;

    public AdMobBanner() {
        super();
    }

    /**
     * Interface called by the AN SDK to request an ad from the mediating SDK.
     *
     * @param mBC the object which will be called with events from the 3d party SDK
     * @param activity the activity from which this is launched
     * @param parameter String parameter received from the server for instantiation of this object
     * @param adUnitID The 3rd party placement , in adMob this is the adUnitID
     * @param width Width of the ad
     * @param height Height of the ad
     */
    @Override
    public View requestAd(MediatedBannerAdViewController mBC, Activity activity, String parameter, String adUnitID,
                          int width, int height, TargetingParameters targetingParameters) {
        Clog.d(Clog.mediationLogTag, String.format("AdMobBanner - requesting an ad: [%s, %s, %dx%d]", parameter, adUnitID, width, height));

        AdView admobAV = new AdView(activity);
        admobAV.setAdSize(new AdSize(width, height));
        admobAV.setAdUnitId(adUnitID);

        admobAV.setAdListener(adListener);
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

        for(Pair<String, String> p : targetingParameters.getCustomKeywords()){
            bundle.putString(p.first, p.second);
        }

        builder.addNetworkExtras(new AdMobExtras(bundle));
        if(targetingParameters.getLocation()!=null){
            builder.setLocation(targetingParameters.getLocation());
        }
        mMediatedBannerAdViewController = mBC;

        admobAV.loadAd(builder.build());
        return admobAV;
    }

}

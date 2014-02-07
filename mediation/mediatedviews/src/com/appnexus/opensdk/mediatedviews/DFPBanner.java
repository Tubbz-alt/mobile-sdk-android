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
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.doubleclick.PublisherAdView;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is the Google DFP banner adaptor it provides the functionality needed to allow
 * an application using the App Nexus SDK to load a banner ad through the Google/DFP SDK. The instantiation
 * of this class is done in response from the AppNexus server for a banner placement that is configured
 * to use DFP to serve it. This class is never instantiated by the developer.
 * <p>
 * This class also serves as an example of how to write a Mediation adaptor for the AppNexus
 * SDK.
 *
 */
public class DFPBanner implements MediatedBannerAdView {

    private final AdListener adListener = new AdListener() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            Clog.d(Clog.mediationLogTag, "DFPBanner - onAdClosed");
        }

        @Override
        public void onAdFailedToLoad(int errorCode) {
            super.onAdFailedToLoad(errorCode);
            Clog.d(Clog.mediationLogTag, String.format("DFPBanner - onAdFailedToLoad: with errorCode: %s", errorCode));

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

            Clog.d(Clog.mediationLogTag, "DFPBanner - onAdLeftApplication");
            if (mMediatedBannerAdViewController != null) {
                mMediatedBannerAdViewController.onAdClicked();
            }
        }

        @Override
        public void onAdOpened() {
            super.onAdOpened();
            Clog.d(Clog.mediationLogTag, "DFPBanner - onAdOpened");
        }

        @Override
        public void onAdLoaded() {
            super.onAdLoaded();

            Clog.d(Clog.mediationLogTag, "DFPBanner - onAdLoaded");
            if (mMediatedBannerAdViewController != null) {
                mMediatedBannerAdViewController.onAdLoaded();
            }
        }
    };

    private MediatedBannerAdViewController mMediatedBannerAdViewController;

    public DFPBanner() {
        super();
    }



    /**
     * Interface called by the AN SDK to request an ad from the mediating SDK.
     *
     * @param mBC the object which will be called with events from the 3d party SDK
     * @param activity the activity from which this is launched
     * @param parameter String parameter received from the server for instantiation of this object
     * @param adUnitID The 3rd party placement , in DFP this is the adUnitID
     * @param width Width of the ad
     * @param height Height of the ad
     */
    @Override
    public View requestAd(MediatedBannerAdViewController mBC, Activity activity, String parameter, String adUnitID,
                          int width, int height, TargetingParameters targetingParameters) {

        Clog.d(Clog.mediationLogTag, String.format("DFPBanner - requesting an ad: [%s, %s, %dx%d]", parameter, adUnitID, width, height));

        DFBBannerSSParameters ssparm = new DFBBannerSSParameters(parameter);

        AdSize adSize = ssparm.isSmartBanner ? AdSize.SMART_BANNER : new AdSize(width,height);

        final PublisherAdView v;

        if (ssparm.isSwipeable) {
            v = new PublisherAdView(activity);//, adSize, adUnitID);
        } else {
            v = new PublisherAdView(activity);//, adSize, adUnitID);
        }

        v.setAdUnitId(adUnitID);
        v.setAdSizes(adSize);
        v.setAdListener(adListener);

        PublisherAdRequest.Builder builder = new PublisherAdRequest.Builder();

        if (ssparm.test_device != null && ssparm.test_device.length() > 0) {
            Clog.d(Clog.mediationLogTag, "DFPBanner - requestAd called with test device " + ssparm.test_device);
            builder.addTestDevice(ssparm.test_device);
        }

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

        Bundle bundle = new Bundle();
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

        mMediatedBannerAdViewController = mBC;

        v.loadAd(builder.build());

        return v;
    }

    /**
     * Class to extract optional server side parameters from passed in json string.
     * Supports
     * {
     *  "swipeable" : 1,
     *  "smartbanner" : 1
     *  }
     *
     */
    class DFBBannerSSParameters {

        public DFBBannerSSParameters(String parameter)
        {
            final String SWIPEABLE = "swipeable";
            final String SMARTBANNER = "smartbanner";

            do {
                JSONObject req = null;
                if (parameter == null || parameter.length() == 0) {
                    break;
                }
                try {
                    req = new JSONObject(parameter);
                } catch (JSONException e) {
                    // This is optional
                }
                finally {
                    if (req == null) {
                        break;
                    }
                }

                try {
                    isSwipeable = req.getBoolean(SWIPEABLE);
                }   catch (JSONException e) {}
                try {
                    isSmartBanner = req.getBoolean(SMARTBANNER);
                }   catch (JSONException e) {}

            } while (false);
        }


        public boolean isSwipeable;
        public String test_device;
        public boolean isSmartBanner;


    }
}

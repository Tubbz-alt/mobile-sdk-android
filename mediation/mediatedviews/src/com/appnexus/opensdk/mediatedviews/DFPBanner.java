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
import android.util.Pair;
import android.view.View;
import com.appnexus.opensdk.*;
import com.appnexus.opensdk.utils.Clog;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.doubleclick.DfpAdView;
import com.google.ads.doubleclick.DfpExtras;
import com.google.ads.doubleclick.SwipeableDfpAdView;

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
public class DFPBanner implements MediatedBannerAdView, AdListener {
    private MediatedBannerAdViewController mMediatedBannerAdViewController;

    public DFPBanner() {
        super();
    }

    /**
     * Interface called by the AN SDK to request an ad from the mediating SDK.
     *
     * @param mBC the object which will be called with events from the 3d party SDK
     * @param Activity the activity from which this is launched
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

        DfpAdView v ;
        if (ssparm.isSwipeable) {
            v = new SwipeableDfpAdView(activity, adSize, adUnitID);
        } else {
            v = new DfpAdView(activity, adSize, adUnitID);
        }

        v.setAdListener(this);
        AdRequest ar = new AdRequest();

        if (ssparm.test_device != null && ssparm.test_device.length() > 0) {
            Clog.d(Clog.mediationLogTag, "DFPBanner - requestAd called with test device " + ssparm.test_device);
            ar.addTestDevice(ssparm.test_device);
        }

        switch(targetingParameters.getGender()){
            case UNKNOWN:
                break;
            case FEMALE:
                ar.setGender(AdRequest.Gender.FEMALE);
                break;
            case MALE:
                ar.setGender(AdRequest.Gender.MALE);
                break;
        }
        DfpExtras extras = new DfpExtras();
        if(targetingParameters.getAge()!=null){
            extras.addExtra("Age", targetingParameters.getAge());
        }
        if(targetingParameters.getLocation()!=null){
            ar.setLocation(targetingParameters.getLocation());
        }
        for(Pair<String, String> p : targetingParameters.getCustomKeywords()){
            extras.addExtra(p.first, p.second);
        }
        ar.setNetworkExtras(extras);

        mMediatedBannerAdViewController = mBC;

        v.loadAd(ar);

        return v;
    }

    @Override
    public void onReceiveAd(Ad ad) {
        Clog.d(Clog.mediationLogTag, "DFPBanner - onReceiveAd: " + ad);
        if (mMediatedBannerAdViewController != null) {
            mMediatedBannerAdViewController.onAdLoaded();
        }
    }

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
        Clog.d(Clog.mediationLogTag, String.format("DFPBanner - onFailedToReceiveAd: %s with error: %s", ad, errorCode));

        MediatedAdViewController.RESULT code = MediatedAdViewController.RESULT.INTERNAL_ERROR;

        switch (errorCode) {
            case INTERNAL_ERROR:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
            case INVALID_REQUEST:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case NETWORK_ERROR:
                code = MediatedAdViewController.RESULT.NETWORK_ERROR;
                break;
            case NO_FILL:
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
    public void onPresentScreen(Ad ad) {
        Clog.d(Clog.mediationLogTag, "DFPBanner - onPresentScreen: " + ad);
    }

    @Override
    public void onDismissScreen(Ad ad) {
        Clog.d(Clog.mediationLogTag, "DFPBanner - onDismissScreen: " + ad);
    }

    @Override
    public void onLeaveApplication(Ad ad) {
        Clog.d(Clog.mediationLogTag, "DFPBanner - onLeaveApplication: " + ad);
        if (mMediatedBannerAdViewController != null) {
            mMediatedBannerAdViewController.onAdClicked();
        }
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

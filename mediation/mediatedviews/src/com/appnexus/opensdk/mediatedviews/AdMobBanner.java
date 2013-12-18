package com.appnexus.opensdk.mediatedviews;

import android.app.Activity;
import android.view.View;

import com.appnexus.opensdk.MediatedAdViewController;
import com.appnexus.opensdk.MediatedBannerAdView;
import com.appnexus.opensdk.MediatedBannerAdViewController;
import com.appnexus.opensdk.utils.Clog;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;

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
public class AdMobBanner implements MediatedBannerAdView, AdListener {
    private MediatedBannerAdViewController mMediatedBannerAdViewController;

    public AdMobBanner() {
        super();
    }

    /**
     * Interface called by the AN SDK to request an ad from the mediating SDK. 
     * 
     * @param mBC the object which will be called with events from the 3d party SDK
     * @param Activity the activity from which this is launched
     * @param parameter String parameter received from the server for instantiation of this object 
     * @param adUnitID The 3rd party placement , in adMob this is the adUnitID
     * @param width Width of
     * @param height
     */
    @Override
    public View requestAd(MediatedBannerAdViewController mBC, Activity activity, String parameter, String adUnitID,
                          int width, int height) {
        if (mBC == null) {
            Clog.e(Clog.mediationLogTag, "AdMobBanner - requestAd called with null controller");
            return null;
        }

        if (activity == null) {
            Clog.e(Clog.mediationLogTag, "AdMobBanner - requestAd called with null activity");
            return null;
        }
        Clog.d(Clog.mediationLogTag, String.format("AdMobBanner - requesting an ad: [%s, %s, %dx%d]", parameter, adUnitID, width, height));

        AdView admobAV = new AdView(activity, new AdSize(width, height), adUnitID);
        admobAV.setAdListener(this);
        AdRequest ar = new AdRequest();

        mMediatedBannerAdViewController = mBC;

        admobAV.loadAd(ar);
        return admobAV;
    }

    @Override
    public void onDismissScreen(Ad arg0) {
        Clog.d(Clog.mediationLogTag, "AdMobBanner - onDismissScreen: " + arg0);
    }

    @Override
    public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
        Clog.d(Clog.mediationLogTag, String.format("AdMobBanner - onFailedToReceiveAd: %s with error: %s", arg0, arg1));

        MediatedAdViewController.RESULT code = MediatedAdViewController.RESULT.INTERNAL_ERROR;

        switch (arg1) {
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
    public void onLeaveApplication(Ad arg0) {
        Clog.d(Clog.mediationLogTag, "AdMobBanner - onLeaveApplication: " + arg0);
        if (mMediatedBannerAdViewController != null) {
            mMediatedBannerAdViewController.onAdClicked();
        }

    }

    @Override
    public void onPresentScreen(Ad arg0) {
        Clog.d(Clog.mediationLogTag, "AdMobBanner - onPresentScreen: " + arg0);
    }

    @Override
    public void onReceiveAd(Ad arg0) {
        Clog.d(Clog.mediationLogTag, "AdMobBanner - onReceiveAd: " + arg0);
        if (mMediatedBannerAdViewController != null) {
            mMediatedBannerAdViewController.onAdLoaded();
        }
    }

}

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

import com.appnexus.opensdk.MediatedAdViewController;
import com.appnexus.opensdk.MediatedBannerAdViewController;
import com.appnexus.opensdk.ResultCode;
import com.appnexus.opensdk.utils.Clog;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;

public class AdMobAdListener implements AdListener {
    MediatedAdViewController mediatedAdViewController;
    String className;

    public AdMobAdListener(MediatedAdViewController mediatedAdViewController, String className) {
        this.mediatedAdViewController = mediatedAdViewController;
        this.className = className;
    }

    @Override
    public void onReceiveAd(Ad ad) {
        printToClog("onReceiveAd: " + ad);
        if (mediatedAdViewController != null) {
            mediatedAdViewController.onAdLoaded();
        }
    }

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
        printToClog("onFailedToReceiveAd: " + ad + " error code: " + errorCode);

        ResultCode code = ResultCode.INTERNAL_ERROR;

        switch (errorCode) {
            case INTERNAL_ERROR:
                code = ResultCode.INTERNAL_ERROR;
                break;
            case INVALID_REQUEST:
                code = ResultCode.INVALID_REQUEST;
                break;
            case NETWORK_ERROR:
                code = ResultCode.NETWORK_ERROR;
                break;
            case NO_FILL:
                code = ResultCode.UNABLE_TO_FILL;
                break;
            default:
                break;
        }

        if (mediatedAdViewController != null) {
            mediatedAdViewController.onAdFailed(code);
        }
    }

    @Override
    public void onPresentScreen(Ad ad) {
        printToClog("onPresentScreen: " + ad);
        // interstitials get this callback when show is called, so ignore that
        if ((mediatedAdViewController != null)
                && (mediatedAdViewController instanceof MediatedBannerAdViewController)) {
            mediatedAdViewController.onAdExpanded();
        }
    }

    @Override
    public void onDismissScreen(Ad ad) {
        printToClog("onDismissScreen: " + ad);
        // interstitials get this callback when the ad is closed, so ignore that
        if ((mediatedAdViewController != null)
                && (mediatedAdViewController instanceof MediatedBannerAdViewController)) {
            mediatedAdViewController.onAdCollapsed();
        }
    }

    @Override
    public void onLeaveApplication(Ad ad) {
        printToClog("onLeaveApplication: " + ad);
        if (mediatedAdViewController != null) {
            mediatedAdViewController.onAdClicked();
        }
    }

    void printToClog(String s) {
        Clog.d(Clog.mediationLogTag, className + " - " + s);
    }

    void printToClogError(String s) {
        Clog.e(Clog.mediationLogTag, className + " - " + s);
    }
}

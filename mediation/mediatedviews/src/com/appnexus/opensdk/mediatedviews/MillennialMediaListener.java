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
import com.appnexus.opensdk.MediatedInterstitialAdViewController;
import com.appnexus.opensdk.utils.Clog;
import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.RequestListener;

/**
 * This class provides the bridge for the Millennial Media's SDK events to the AppNexus SDK events. 
 * This class is used internally by the Millennial Media mediation adaptor.  
 *
 */
class MillennialMediaListener implements RequestListener {

    private final MediatedAdViewController mediatedAdViewController;
    private final String className;

    public MillennialMediaListener(MediatedAdViewController mediatedAdViewController, String className) {
        this.mediatedAdViewController = mediatedAdViewController;
        this.className = className;
        Clog.d(Clog.mediationLogTag, String.format("New MillennialMediaListener created for [%s %s]", className, mediatedAdViewController));
    }

    // occurs when ad is clicked and browser is launched.
    // or when an interstitial is launched
    @Override
    public void MMAdOverlayLaunched(MMAd mmAd) {
        Clog.d(Clog.mediationLogTag, String.format("%s - MMAdOverlayLaunched: %s", className, mmAd));
        if (mediatedAdViewController != null)
            mediatedAdViewController.onAdExpanded();
    }

    @Override
    public void MMAdOverlayClosed(MMAd mmAd) {
        Clog.d(Clog.mediationLogTag, String.format("%s - MMAdOverlayClosed: %s", className, mmAd));
        if (mediatedAdViewController != null)
            mediatedAdViewController.onAdCollapsed();
    }

    @Override
    public void MMAdRequestIsCaching(MMAd mmAd) {
        Clog.d(Clog.mediationLogTag, String.format("%s - MMAdRequestIsCaching: %s", className, mmAd));
    }

    @Override
    public void requestCompleted(MMAd mmAd) {
        Clog.d(Clog.mediationLogTag, String.format("%s - requestCompleted: %s", className, mmAd));
        if (mediatedAdViewController != null)
            mediatedAdViewController.onAdLoaded();
    }

    @Override
    public void requestFailed(MMAd mmAd, MMException e) {
        Clog.d(Clog.mediationLogTag, String.format("%s - requestFailed: %s with error %s", className, mmAd, e));
        MediatedAdViewController.RESULT code = MediatedInterstitialAdViewController.RESULT.INTERNAL_ERROR;

        boolean shouldCancelFailure = false;

        switch (e.getCode()) {
            case MMException.INVALID_PARAMETER:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case MMException.INNER_EXCEPTION:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
            case MMException.MAIN_THREAD_REQUIRED:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
            case MMException.REQUEST_TIMEOUT:
                code = MediatedAdViewController.RESULT.NETWORK_ERROR;
                break;
            case MMException.REQUEST_NO_NETWORK:
                code = MediatedAdViewController.RESULT.NETWORK_ERROR;
                break;
            case MMException.REQUEST_IN_PROGRESS:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case MMException.REQUEST_ALREADY_CACHING:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case MMException.REQUEST_NOT_FILLED:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.REQUEST_BAD_RESPONSE:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case MMException.REQUEST_NOT_PERMITTED:
                code = MediatedAdViewController.RESULT.INVALID_REQUEST;
                break;
            case MMException.CACHE_NOT_EMPTY:
                Clog.d(Clog.mediationLogTag, "%s - cache not empty, show it");
                if (mediatedAdViewController instanceof MediatedInterstitialAdViewController) {
                    shouldCancelFailure = true;
                    mediatedAdViewController.onAdLoaded();
                }
                break;
            case MMException.DISPLAY_AD_NOT_READY:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.DISPLAY_AD_EXPIRED:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.DISPLAY_AD_NOT_FOUND:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.DISPLAY_AD_ALREADY_DISPLAYED:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.DISPLAY_AD_NOT_PERMITTED:
                code = MediatedAdViewController.RESULT.UNABLE_TO_FILL;
                break;
            case MMException.AD_BROKEN_REFERENCE:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
            case MMException.AD_NO_ACTIVITY:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
            case MMException.UNKNOWN_ERROR:
                code = MediatedAdViewController.RESULT.INTERNAL_ERROR;
                break;
        }

        if (!shouldCancelFailure && (mediatedAdViewController != null))
            mediatedAdViewController.onAdFailed(code);
    }

    @Override
    public void onSingleTap(MMAd mmAd) {
        Clog.d(Clog.mediationLogTag, String.format("%s - onSingleTap: %s", className, mmAd));
        if (mediatedAdViewController != null)
            mediatedAdViewController.onAdClicked();
    }
}

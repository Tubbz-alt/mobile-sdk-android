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

package com.appnexus.opensdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.appnexus.opensdk.utils.ViewUtil;

@SuppressLint("ViewConstructor")
class MRAIDWebView extends AdWebView implements Displayable {
    private MRAIDImplementation implementation;
    final AdView owner;
    private int default_width;
    private int default_height;
    boolean isFullScreen = false;

    // for viewable event
    private boolean isOnscreen = false;
    private boolean isVisible = false;
    private Handler handler = new Handler();
    private boolean viewableCheckPaused = false;

    public MRAIDWebView(AdView owner) {
        super(owner);
        this.owner = owner;
        if (this.getVisibility()== VISIBLE) startCheckViewable();
    }

    void setImplementation(MRAIDImplementation imp) {
        this.implementation = imp;
        this.setWebViewClient(imp.getWebViewClient());
        this.setWebChromeClient(imp.getWebChromeClient());
    }

    MRAIDImplementation getImplementation() {
        return implementation;
    }

    public void loadAd(AdResponse ar) {
        String html = ar.getContent();

        if (ar.isMraid()) {
            setImplementation(new MRAIDImplementation(this));
        }

        if (implementation != null) {
            html = implementation.onPreLoadContent(this, html);
        }

        final float scale = owner.getContext().getResources()
                .getDisplayMetrics().density;
        int rheight = (int) (ar.getHeight() * scale + 0.5f);
        int rwidth = (int) (ar.getWidth() * scale + 0.5f);
        int rgravity = Gravity.CENTER;
        AdView.LayoutParams resize = new AdView.LayoutParams(rwidth, rheight,
                rgravity);
        this.setLayoutParams(resize);

        this.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    public void onVisibilityChanged(View view, int visibility) {
        if (visibility == VISIBLE) {
            isVisible = true;
            startCheckViewable();
        } else {
            isVisible = false;
            stopCheckViewable();
        }
        fireViewableChangeEvent();
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(0, 0);
    }

    // w,h in dips. this function converts to pixels
    void expand(int w, int h, boolean cust_close, final MRAIDImplementation caller,
                final boolean allowOrientationChange, final AdActivity.OrientationEnum forceOrientation) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                this.getLayoutParams());
        default_width = lp.width;
        default_height = lp.height;

        if ((h == -1) && (w == -1)) {
            if (owner != null) {
                isFullScreen = true;
            }
        }
        if (h != -1) {
            h = (int) (h * metrics.density + 0.5);
        }
        if (w != -1) {
            w = (int) (w * metrics.density + 0.5);
        }


        lp.height = h;
        lp.width = w;
        lp.gravity = Gravity.CENTER;

        MRAIDFullscreenListener mraidFullscreenListener = null;
        if (isFullScreen) {
            // if fullscreen, create a listener to lock the activity when it is created
            mraidFullscreenListener = new MRAIDFullscreenListener() {
                @Override
                public void onCreateCompleted() {
                    // lock orientation if necessary
                    if ((caller != null) && (caller.getFullscreenActivity() != null)) {
                        lockOrientationFromExpand(caller.getFullscreenActivity(),
                                allowOrientationChange, forceOrientation);
                        AdView.mraidFullscreenListener = null; // only listen once
                    }
                }
            };
        } else {
            // otherwise, lock the current activity
            lockOrientationFromExpand((Activity) this.getContext(),
                    allowOrientationChange, forceOrientation);
        }

        if (owner != null) {
            owner.expand(w, h, cust_close, caller, mraidFullscreenListener);
        }

        if (owner instanceof InterstitialAdView) {
            ((InterstitialAdView) owner).interacted();
        }

        this.setLayoutParams(lp);
    }

    private void lockOrientationFromExpand(Activity containerActivity,
                                           boolean allowOrientationChange,
                                           AdActivity.OrientationEnum forceOrientation) {
        if (forceOrientation != AdActivity.OrientationEnum.none) {
            AdActivity.lockToMRAIDOrientation(containerActivity, forceOrientation);
        }

        if (allowOrientationChange) {
            AdActivity.unlockOrientation(containerActivity);
        } else if (forceOrientation == AdActivity.OrientationEnum.none) {
            // if forceOrientation was not none, it would have locked the orientation already
            AdActivity.lockToCurrentOrientation(containerActivity);
        }
    }

    void hide() {
        if (owner != null) {
            owner.hide();
        }
    }

    void show() {
        if (owner != null) {
            owner.expand(default_width, default_height, true, null, null);
        }
    }

    void close() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                this.getLayoutParams());
        lp.height = default_height;
        lp.width = default_width;
        lp.gravity = Gravity.CENTER;

        if (owner != null) {
            owner.close(default_width, default_height, implementation);
        }


        this.setLayoutParams(lp);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean failed() {
        return false;
    }

    private void checkPosition() {
        if (!(this.getContext() instanceof Activity)) return;

        // check whether newly drawn view is onscreen or not,
        // fires a viewableChangeEvent with the result
        int viewLocation[] = new int[2];
        this.getLocationOnScreen(viewLocation);
        this.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        int left = viewLocation[0];
        int right = viewLocation[0] + this.getMeasuredWidth();
        int top = viewLocation[1];
        int bottom = viewLocation[1] + this.getMeasuredHeight();

        int[] screenSize = ViewUtil.getScreenSizeAsPixels((Activity) this.getContext());

        this.isOnscreen = (right > 0) && (left < screenSize[0])
                && (bottom > 0) && (top < screenSize[1]);
        this.fireViewableChangeEvent();

        // update current position
        if (implementation != null) {
            implementation.setCurrentPosition(left, top, this.getMeasuredWidth(), this.getMeasuredHeight());
        }
    }

    void fireViewableChangeEvent() {
        if (implementation != null) {
            implementation.onViewableChange(isOnscreen && isVisible);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        stopCheckViewable();
        if (implementation != null) {
            implementation.destroy();
        }
    }

    public void resize(int w, int h, int offset_x, int offset_y, MRAIDImplementation.CUSTOM_CLOSE_POSITION custom_close_position, boolean allow_offscrean) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                this.getLayoutParams());
        if(!implementation.resized){
            default_width = lp.width;
            default_height = lp.height;
        }


        if (h != -1) {
            h = (int) (h * metrics.density + 0.5);
        }
        if (w != -1) {
            w = (int) (w * metrics.density + 0.5);
        }


        lp.height = h;
        lp.width = w;
        lp.gravity = Gravity.CENTER;

        if (owner != null) {
            owner.resize(w, h, offset_x, offset_y, custom_close_position, allow_offscrean, implementation);
        }

        if (owner instanceof InterstitialAdView) {
            ((InterstitialAdView) owner).interacted();
        }

        this.setLayoutParams(lp);
    }

    interface MRAIDFullscreenListener {
        void onCreateCompleted();
    }

    // Viewable timer code

    private final Runnable checkViewableRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewableCheckPaused) return;
            checkPosition();
            handler.postDelayed(this, 1000);
        }
    };

    private void startCheckViewable() {
        viewableCheckPaused = false;
        handler.removeCallbacks(checkViewableRunnable);
        handler.post(checkViewableRunnable);
    }

    private void stopCheckViewable() {
        viewableCheckPaused = true;
        handler.removeCallbacks(checkViewableRunnable);
    }

}

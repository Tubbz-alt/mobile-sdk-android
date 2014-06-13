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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.utils.Settings;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class controls the loading and display of interstitial ads.
 * These ads are modal and take up the entire screen.  Each
 * interstitial ad is tied to a {@link AdActivity} which is launched
 * to show the ad.
 */
public class InterstitialAdView extends AdView {
    static final long MAX_AGE = 60000;
    private ArrayList<Size> allowedSizes;
    private int backgroundColor = Color.BLACK;
    private int closeButtonDelay = Settings.DEFAULT_INTERSTITIAL_CLOSE_BUTTON_DELAY;
    static InterstitialAdView INTERSTITIALADVIEW_TO_USE;
    private Queue<Pair<Long, Displayable>> adQueue = new LinkedList<Pair<Long, Displayable>>();

    //Intent Keys
    static final String INTENT_KEY_TIME = "TIME";
    static final String INTENT_KEY_CLOSE_BUTTON_DELAY = "CLOSE_BUTTON_DELAY";

    //To let the activity show the button.
    private AdActivity.AdActivityImplementation adImplementation = null;

    /**
     * Creates a new interstitial ad view in which to load and show
     * interstitial ads.
     *
     * @param context The context of the {@link ViewGroup} to which
     *                the interstitial ad view is being added.
     */
    public InterstitialAdView(Context context) {
        super(context);
    }

    /**
     * Creates a new interstitial ad view in which to load and show
     * interstitial ads.
     *
     * @param context The context of the {@link ViewGroup} to which
     *                the interstitial ad view is being added.
     * @param attrs   The {@link AttributeSet} to use when creating the
     *                interstitial ad view.
     */
    public InterstitialAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a new interstitial ad view in which to load and show
     * interstitial ads.
     *
     * @param context  The context of the {@link ViewGroup} to which
     *                 the interstitial ad view is being added.
     * @param attrs    The {@link AttributeSet} to use when creating the
     *                 interstitial ad view.
     * @param defStyle The default style to apply to this view. If 0,
     *                 no style will be applied (beyond what is
     *                 included in the theme). This may be either an
     *                 attribute resource, whose value will be
     *                 retrieved from the current theme, or an
     *                 explicit style resource.
     */
    public InterstitialAdView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void setup(Context context, AttributeSet attrs) {
        super.setup(context, attrs);
        mAdFetcher.setAutoRefresh(false);

        // Get the screen size
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(dm);
        measuredHeight = dm.heightPixels;
        measuredWidth = dm.widthPixels;
        int h_adjust = 0;

        Activity a = (Activity) context;
        if (a != null) {
            Rect r = new Rect();
            a.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
            h_adjust += a.getWindow().findViewById(Window.ID_ANDROID_CONTENT)
                    .getTop();
            measuredHeight -= h_adjust;
        }

        float scale = dm.density;
        measuredHeight = (int) (measuredHeight / scale + 0.5f);
        measuredWidth = (int) (measuredWidth / scale + 0.5f);

        allowedSizes = new ArrayList<Size>();


        if (new Size(300, 250).fitsIn(measuredWidth, measuredHeight))
            allowedSizes.add(new Size(300, 250));
        if (new Size(320, 480).fitsIn(measuredWidth, measuredHeight))
            allowedSizes.add(new Size(320, 480));
        if (new Size(900, 500).fitsIn(measuredWidth, measuredHeight))
            allowedSizes.add(new Size(900, 500));
        if (new Size(1024, 1024).fitsIn(measuredWidth, measuredHeight))
            allowedSizes.add(new Size(1024, 1024));

    }

    @Override
    protected void loadVariablesFromXML(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.InterstitialAdView);

        final int N = a.getIndexCount();
        Clog.v(Clog.xmlLogTag, Clog.getString(R.string.found_n_in_xml, N));
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.InterstitialAdView_placement_id) {
                setPlacementID(a.getString(attr));
                Clog.d(Clog.xmlLogTag, Clog.getString(R.string.placement_id,
                        a.getString(attr)));
            } else if (attr == R.styleable.InterstitialAdView_test) {
                Settings.getSettings().test_mode = a.getBoolean(attr, false);
                Clog.d(Clog.xmlLogTag,
                        Clog.getString(R.string.xml_set_test,
                                Settings.getSettings().test_mode));
            } else if (attr == R.styleable.InterstitialAdView_opens_native_browser) {
                Clog.d(Clog.xmlLogTag,
                        Clog.getString(R.string.xml_set_opens_native_browser));
                this.setOpensNativeBrowser(a.getBoolean(attr, false));
            }
        }
        a.recycle();
    }

    /**
     * Requests a new interstitial ad from the server and stores it in
     * a local queue.  Note that interstitials have a timeout of 60
     * seconds; you must show the interstitial (by calling
     * <code>show()</code>) within 60 seconds of getting a response;
     * otherwise, the ad will not show.
     *
     * @return <code>true</code> if the ad load was successfully
     * dispatched; <code>false</code> otherwise.
     */
    @Override
    public boolean loadAd() {
        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.load_ad_int));
        if (!isReadyToStart())
            return false;
        if (mAdFetcher != null) {
            // Load an interstitial ad
            mAdFetcher.stop();
            mAdFetcher.start();
            return true;
        }
        return false;
    }

    @Override
    protected void display(Displayable d) {
        // safety check: this should never evaluate to true
        if ((d == null) || d.failed()) {
            // The displayable has failed to be parsed or turned into a View.
            // We're already calling onAdLoaded, so don't call onAdFailed; just log
            Clog.e(Clog.baseLogTag, "Loaded an ad with an invalid displayable");
            return;
        }

        if (lastDisplayable != null) {
            lastDisplayable.destroy();
        }
        lastDisplayable = d;

        adQueue.add(new Pair<Long, Displayable>(System.currentTimeMillis(), d));
    }

    void interacted() {
        if (adImplementation != null) {
            adImplementation.interacted();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        // leave empty so that we don't call super
    }

    // removes stale ads and returns whether or not a valid ad exists
    private boolean removeStaleAds(long now) {
        boolean validAdExists = false;
        ArrayList<Pair<Long, Displayable>> staleAdsList = new ArrayList<Pair<Long, Displayable>>();
        for (Pair<Long, Displayable> p : adQueue) {
            if ((p == null) || (p.second == null)
                    || ((now - p.first) > InterstitialAdView.MAX_AGE)) {
                staleAdsList.add(p);
            } else {
                // We've reached a valid ad, so we can stop looking
                validAdExists = true;
                break;
            }
        }
        // Clear the queue of invalid ads
        for (Pair<Long, Displayable> p : staleAdsList) {
            adQueue.remove(p);
        }
        return validAdExists;
    }

    @Override
    boolean isBanner() {
        return false;
    }

    @Override
    boolean isInterstitial() {
        return true;
    }

    /**
     * Checks the queue to see if there is a valid (i.e., fresher than
     * 60 seconds) interstitial ad available.
     *
     * @return <code>true</code> if there is a valid ad available in
     * the queue, <code>false</code> otherwise.
     */
    public boolean isReady() {
        long now = System.currentTimeMillis();
        if (removeStaleAds(now)) {
            Pair<Long, Displayable> top = adQueue.peek();
            if (top != null && top.second instanceof MediatedDisplayable) {
                MediatedDisplayable mediatedDisplayable = (MediatedDisplayable) top.second;
                if (mediatedDisplayable.getMAVC() instanceof MediatedInterstitialAdViewController) {
                    MediatedInterstitialAdViewController mAVC = (MediatedInterstitialAdViewController) mediatedDisplayable.getMAVC();
                    return mAVC.isReady();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Pops interstitial ads from the queue until it finds one that
     * has not exceeded the 60 second timeout, and displays it in a
     * new activity.  All ads in the queue which have exceeded the
     * timeout are removed.
     *
     * @return The number of remaining ads in the queue that do not
     * exceed the timeout.
     */
    public int show() {
        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.show_int));
        // Make sure there is an ad to show
        long now = System.currentTimeMillis();
        boolean validAdExists = removeStaleAds(now);

        //If the head of the queue is interstitial mediation, show that instead of our adactivity
        Pair<Long, Displayable> top = adQueue.peek();
        if (top != null && top.second instanceof MediatedDisplayable) {
            MediatedDisplayable mediatedDisplayable = (MediatedDisplayable) top.second;
            if (mediatedDisplayable.getMAVC() instanceof MediatedInterstitialAdViewController) {
                MediatedInterstitialAdViewController mAVC = (MediatedInterstitialAdViewController) mediatedDisplayable.getMAVC();
                mAVC.show();

                //Pop the mediated view;
                adQueue.poll();
                return adQueue.size();
            }
        }

        // otherwise, launch our adActivity
        if (validAdExists) {
            Class<?> activity_clz = AdActivity.getActivityClass();
            Intent i = new Intent(getContext(), activity_clz);
            i.putExtra(AdActivity.INTENT_KEY_ACTIVITY_TYPE,
                    AdActivity.ACTIVITY_TYPE_INTERSTITIAL);
            i.putExtra(InterstitialAdView.INTENT_KEY_TIME, now);
            i.putExtra(InterstitialAdView.INTENT_KEY_CLOSE_BUTTON_DELAY, closeButtonDelay);

            INTERSTITIALADVIEW_TO_USE = this;
            try {
                getContext().startActivity(i);
            } catch (ActivityNotFoundException e) {
                INTERSTITIALADVIEW_TO_USE = null;
                Clog.e(Clog.baseLogTag, Clog.getString(R.string.adactivity_missing,activity_clz.getName()));
            }

            return adQueue.size() - 1; // Return the number of ads remaining, less the one we're about to show
        }
        Clog.w(Clog.baseLogTag, Clog.getString(R.string.empty_queue));
        return adQueue.size();
    }

    /**
     * Get a list of ad {@link Size}s which are allowed to be
     * displayed.
     *
     * @return The {@link ArrayList} of {@link Size}s which are
     * allowed to be displayed.
     */
    public ArrayList<Size> getAllowedSizes() {
        Clog.d(Clog.publicFunctionsLogTag,
                Clog.getString(R.string.get_allowed_sizes));
        return allowedSizes;
    }

    /**
     * Set the ad {@link Size}s which are allowed to be displayed.
     * This is a list of the platform ad sizes that may be inserted
     * into an interstitial ad view.  The default list is sufficient
     * for most implementations.  Custom sizes may also be added here.
     *
     * @param allowed_sizes The {@link ArrayList} of {@link Size}s
     *                      which are allowed to be displayed.
     */
    public void setAllowedSizes(ArrayList<Size> allowed_sizes) {
        Clog.d(Clog.publicFunctionsLogTag,
                Clog.getString(R.string.set_allowed_sizes));
        allowedSizes = allowed_sizes;
    }

    /**
     * Sets the background color to use behind the interstitial ad.
     * If left unspecified, the default is black.
     */
    public void setBackgroundColor(int color) {
        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.set_bg));
        backgroundColor = color;
    }

    /**
     * Get the current background color which will appear behind the
     * interstitial ad.
     *
     * @return The background color to use behind the interstitial ad.
     */
    public int getBackgroundColor() {
        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.get_bg));
        return backgroundColor;
    }

    /**
     * Destroy this InterstitialAdView object.
     */
    public void destroy() {
        Clog.d(Clog.publicFunctionsLogTag, Clog.getString(R.string.destroy_int));
        if (this.mAdFetcher != null)
            mAdFetcher.stop();
        adQueue.clear();
        InterstitialAdView.INTERSTITIALADVIEW_TO_USE = null;
    }

    /**
     * Get the delay between when an interstitial ad is displayed and
     * when the close button appears to the user.  10 seconds is the
     * default; 0 means that the close button will appear immediately.
     *
     * @return the time in milliseconds between when an interstitial
     * ad is displayed and when the close button appears.
     */
    public int getCloseButtonDelay() {
        return closeButtonDelay;
    }

    /**
     * Set the delay between when an interstitial ad is displayed and
     * when the close button appears to the user.  10 seconds is the
     * default; it is also the maximum.  Setting the value to 0 allows
     * the close button to appear immediately.
     *
     * @param closeButtonDelay The time in milliseconds before the
     *                         close button is displayed to the user.
     */
    public void setCloseButtonDelay(int closeButtonDelay) {
        this.closeButtonDelay = Math.min(closeButtonDelay, Settings.DEFAULT_INTERSTITIAL_CLOSE_BUTTON_DELAY);
    }

    void setAdImplementation(AdActivity.AdActivityImplementation adImplementation) {
        this.adImplementation = adImplementation;
    }

    Queue<Pair<Long, Displayable>> getAdQueue() {
        return adQueue;
    }

    /**
     * A convenience class which holds a width and height in integers.
     */
    public static class Size {
        private final int w;
        private final int h;

        public Size(int w, int h) {
            this.w = w;
            this.h = h;
        }
        
        

        /**
         * @return The width, in pixels.
         */
        public int width() {
            return w;
        }

        /**
         * @return The height, in pixels.
         */
        public int height() {
            return h;
        }

        /**
         * Determines whether this size object fits inside a rectangle of the
         * given width and height
         *
         * @param width  The width to check against.
         * @param height The height to check against.
         * @return True, if the size fits inside the described rectangle,
         * otherwise, false.
         */
        public boolean fitsIn(int width, int height) {
            return h < height && w < width;
        }
    }
}

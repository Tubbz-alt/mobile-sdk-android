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

package com.appnexus.opensdk.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class ViewUtil {
    public static ImageButton createCloseButton(Context context) {
        final ImageButton close = new ImageButton(context);
        close.setImageDrawable(context.getResources().getDrawable(
                android.R.drawable.ic_menu_close_clear_cancel));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.RIGHT
                | Gravity.TOP);
        close.setLayoutParams(blp);
        close.setBackgroundColor(Color.TRANSPARENT);
        return close;
    }

    public static ProgressBar createClickOverlay(Context context) {
        return createClickOverlay(context, -1);
    }

    public static ProgressBar createClickOverlay(Context context, int maxHeight) { // Max height in pixels
        ProgressBar bar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        bar.setBackgroundColor(0xCC4D4E53); // cool grey
        bar.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        int[] padding = {20,15,20,15}; // In dp
        convertFromDPToPixels((Activity)context, padding);

        if (maxHeight != -1 && maxHeight < (padding[1] * 3)) { // Ad too small for regular overlay
            for (int paddingIndex = 0; paddingIndex < padding.length; paddingIndex++) {
                padding[paddingIndex] = (int)(padding[paddingIndex] / 4.0);
            }
        }

        bar.setPadding(padding[0],padding[1],padding[2],padding[3]);

        return bar;
    }

    public static void removeChildFromParent(View view) {
        if ((view != null) && (view.getParent() != null)) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    public static Context getTopContext(View view) {
        if (view == null) {
            return null;
        }
        ViewParent parent = view.getParent();

        if ((parent == null) || !(parent instanceof View)) {
            return view.getContext();
        }

        //noinspection ConstantConditions
        while ((parent.getParent() != null)
                && (parent.getParent() instanceof View)) {
            parent = parent.getParent();
        }

        return ((View) parent).getContext();
    }

    // returns screen size as { width, height } in pixels
    @SuppressWarnings("deprecation")
    public static int[] getScreenSizeAsPixels(Activity activity) {
        int screenWidth;
        int screenHeight;
        Display d = activity.getWindowManager().getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 13) {
            Point p = new Point();
            d.getSize(p);
            screenWidth = p.x;
            screenHeight = p.y;
        } else {
            screenWidth = d.getWidth();
            screenHeight = d.getHeight();
        }

        return new int[] { screenWidth, screenHeight };
    }

    // returns screen size as { width, height } in DP
    public static int[] getScreenSizeAsDP(Activity activity) {
        int[] screenSize = getScreenSizeAsPixels(activity);
        convertFromPixelsToDP(activity, screenSize);
        return screenSize;
    }

    public static void convertFromPixelsToDP(Activity activity, int[] pixels) {
        final float scale = activity.getResources().getDisplayMetrics().density;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (int) ((pixels[i] / scale) + 0.5f);
        }
    }

    public static void convertFromDPToPixels(Activity activity, int[] pixels) {
        final float scale = activity.getResources().getDisplayMetrics().density;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (int) ((pixels[i] * scale) + 0.5f);
        }
    }
}

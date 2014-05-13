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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CalendarContract;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import com.appnexus.opensdk.utils.*;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;

@SuppressLint("InlinedApi")
class MRAIDImplementation {
    protected final AdWebView owner;
    private boolean readyFired = false;
    boolean expanded = false;
    boolean resized = false;
    int default_width, default_height;
    private int screenWidth, screenHeight;
    boolean supportsPictureAPI = false;
    boolean supportsCalendar = false;
    private AdActivity fullscreenActivity;
    private ViewGroup defaultContainer;
    boolean isViewable;
    private int[] position = new int[4];
    private int lastRotation;

    public MRAIDImplementation(AdWebView owner) {
        this.owner = owner;
    }

    void webViewFinishedLoading(WebView view) {
        // Fire the ready event only once
        if (!readyFired) {
            String adType = owner.adView.isBanner() ? "inline" : "interstitial";
            view.loadUrl("javascript:window.mraid.util.setPlacementType('"
                    + adType + "')");

            setSupportsValues(view);
            setScreenSize();
            setMaxSize();
            setDefaultPosition();

            view.loadUrl("javascript:window.mraid.util.stateChangeEvent('default')");
            view.loadUrl("javascript:window.mraid.util.readyEvent();");

            // Store width and height for close()
            default_width = owner.getLayoutParams().width;
            default_height = owner.getLayoutParams().height;

            readyFired = true;
            onViewableChange(owner.isViewable());
        }
    }

    private void setDefaultPosition() {
        if (!readyFired) return;

        Activity a = (Activity) owner.getContext();

        int[] location = new int[2];
        owner.getLocationOnScreen(location);

        // current position is relative to max size, so subtract the status bar from y
        int contentViewTop = a.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        location[1] -= contentViewTop;

        owner.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int width = owner.getMeasuredWidth();
        int height = owner.getMeasuredHeight();
        int[] size = {width, height};
        ViewUtil.convertFromPixelsToDP(a, size);

        owner.loadUrl(String.format("javascript:window.mraid.util.setDefaultPosition(%d, %d, %d, %d)",
                location[0], location[1], size[0], size[1]));
    }

    private void setSupports(WebView view, String feature, boolean value) {
        view.loadUrl(String.format("javascript:window.mraid.util.setSupports(\'%s\', %s)", feature, String.valueOf(value)));
    }

    @SuppressLint("NewApi")
    private void setSupportsValues(WebView view) {
        //SMS
        if (hasIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:5555555555")))) {
            setSupports(view, "sms", true);
        }

        //Tel
        if (hasIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("tel:5555555555")))) {
            setSupports(view, "tel", true);
        }

        //Calendar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (hasIntent(new Intent(Intent.ACTION_EDIT).setData(CalendarContract.Events.CONTENT_URI))) {
                setSupports(view, "calendar", true);
                supportsCalendar = true;
            } else if (hasIntent(new Intent(Intent.ACTION_EDIT).setType("vnd.android.cursor.item/event"))) {
                setSupports(view, "calendar", true);
                supportsCalendar = true;
                W3CEvent.useMIME = true;
            }
        }

        //Store Picture only if on API 11 or above
        PackageManager pm = owner.getContext().getPackageManager();
        if (pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, owner.getContext().getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setSupports(view, "storePicture", true);
                supportsPictureAPI = true;
            }
        }

        //Video should always work inline.
        setSupports(view, "inlineVideo", true);
    }

    boolean hasIntent(Intent i) {
        PackageManager pm = owner.getContext().getPackageManager();
        return pm.queryIntentActivities(i, 0).size() > 0;
    }

    void onViewableChange(boolean viewable) {
        if (!readyFired) return;
        isViewable = viewable;

        owner.loadUrl("javascript:window.mraid.util.setIsViewable(" + viewable + ")");
    }

    // parameters are view properties in pixels
    void setCurrentPosition(int left, int top, int width, int height) {
        if (!readyFired) return;

        if ((position[0] == left) && (position[1] == top)
                && (position[2] == width) && (position[3] == height)) return;
        position[0] = left;
        position[1] = top;
        position[2] = width;
        position[3] = height;

        Activity a = (Activity) owner.getContext();
        // current position is relative to max size, so subtract the status bar from y
        int contentViewTop = a.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        top -= contentViewTop;

        int[] properties = {left, top, width, height};

        // convert properties to DP
        ViewUtil.convertFromPixelsToDP(a, properties);
        left = properties[0];
        top = properties[1];
        width = properties[2];
        height = properties[3];

        owner.loadUrl(String.format("javascript:window.mraid.util.sizeChangeEvent(%d, %d)",
                width, height));
        owner.loadUrl(String.format("javascript:window.mraid.util.setCurrentPosition(%d, %d, %d, %d)",
                left, top, width, height));
    }

    void close() {
        if (expanded || resized) {
            AdView.LayoutParams lp = new AdView.LayoutParams(
                    owner.getLayoutParams());
            lp.height = default_height;
            lp.width = default_width;
            lp.gravity = Gravity.CENTER;
            owner.setLayoutParams(lp);
            owner.close();
            owner.loadUrl("javascript:window.mraid.util.stateChangeEvent('default');");

            if (!owner.adView.isInterstitial()) {
                owner.adView.getAdDispatcher().onAdCollapsed();
            }

            // Allow orientation changes
            Activity a = ((Activity) this.owner.getContext());
            if (a != null)
                a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            expanded = false;
            resized = false;
        } else {
            // state must be default
            owner.hide();
        }
    }

    void expand(ArrayList<BasicNameValuePair> parameters) {
        // Default value is fullscreen.
        int width = -1;
        int height = -1;
        boolean useCustomClose = false;
        String uri = null;
        boolean allowOrientationChange = true;
        AdActivity.OrientationEnum forceOrientation = AdActivity.OrientationEnum.none;
        for (BasicNameValuePair bnvp : parameters) {
            if (bnvp.getName().equals("w"))
                try {
                    width = Integer.parseInt(bnvp.getValue());
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            else if (bnvp.getName().equals("h"))
                try {
                    height = Integer.parseInt(bnvp.getValue());
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            else if (bnvp.getName().equals("useCustomClose"))
                useCustomClose = Boolean.parseBoolean(bnvp.getValue());
            else if (bnvp.getName().equals("url")) {
                uri = Uri.decode(bnvp.getValue());
            } else if (bnvp.getName().equals("allow_orientation_change")) {
                allowOrientationChange = Boolean.parseBoolean(bnvp.getValue());
            } else if (bnvp.getName().equals("force_orientation")) {
                forceOrientation = parseForceOrientation(bnvp.getValue());
            }
        }

        owner.expand(width, height, useCustomClose, this, allowOrientationChange, forceOrientation);

        // Fire the stateChange to MRAID
        if (!StringUtil.isEmpty(uri)) {
            this.owner.loadUrl(uri);
        }
        this.owner.loadUrl("javascript:window.mraid.util.stateChangeEvent('expanded');");
        expanded = true;

        // Fire the AdListener event
        if (!this.owner.adView.isInterstitial()) {
            this.owner.adView.getAdDispatcher().onAdExpanded();
        }
    }

    void dispatch_mraid_call(String url) {
        // Remove the fake protocol
        url = url.replaceFirst("mraid://", "");

        // Separate the function from the parameters
        String[] qMarkSplit = url.split("\\?");
        String func = qMarkSplit[0].replaceAll("/", "");
        String params;
        ArrayList<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
        if (qMarkSplit.length > 1) {
            params = url.substring(url.indexOf("?") + 1);

            for (String s : params.split("&")) {
                String[] pair = s.split("=");
                if (pair.length < 2) {
                    continue;
                }

                if (StringUtil.isEmpty(pair[1]) || "undefined".equals(pair[1])) {
                    continue;
                }

                parameters.add(new BasicNameValuePair(pair[0], pair[1]));
            }
        }

        if (func.equals("expand")) {
            expand(parameters);
        } else if (func.equals("close")) {
            close();
        } else if (func.equals("resize")) {
            resize(parameters);
        } else if (func.equals("setOrientationProperties")) {
            setOrientationProperties(parameters);
        } else if (supportsCalendar && func.equals("createCalendarEvent")) {
            createCalendarEvent(parameters);
        } else if (func.equals("playVideo")) {
            playVideo(parameters);
        } else if (supportsPictureAPI && func.equals("storePicture")) {
            storePicture(parameters);
        } else if (func.equals("open")) {
            open(parameters);
        } else {
            if (func.equals("enable")) {
                // suppress error for enable command
                return;
            }
            Clog.d(Clog.mraidLogTag, Clog.getString(R.string.unsupported_mraid, func));
        }
    }

    private void open(ArrayList<BasicNameValuePair> parameters) {
        String uri = null;
        for (BasicNameValuePair bnvp : parameters) {
            if (bnvp.getName().equals("uri")) {
                uri = Uri.decode(bnvp.getValue());
            }
        }

        if (!StringUtil.isEmpty(uri)) {
            this.owner.loadURLInCorrectBrowser(uri);
            this.owner.fireAdClicked();
        }
    }

    private void storePicture(ArrayList<BasicNameValuePair> parameters) {
        String uri = null;
        for (BasicNameValuePair bnvp : parameters) {
            if (bnvp.getName().equals("uri")) {
                uri = bnvp.getValue();
            }
        }
        if (uri == null) {
            Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
            return;
        }

        final String uri_final = Uri.decode(uri);

        AlertDialog.Builder builder = new AlertDialog.Builder(ViewUtil.getTopContext(owner));
        builder.setTitle(R.string.store_picture_title);
        builder.setMessage(R.string.store_picture_message);
        builder.setPositiveButton(R.string.store_picture_accept, new DialogInterface.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Check URI scheme
                if (uri_final.startsWith("data:")) {
                    //Remove 'data:(//?)' and save
                    String ext = ".png";
                    boolean isBase64 = false;
                    //First, find file type:
                    if (uri_final.contains("image/gif")) {
                        ext = ".gif";
                    } else if (uri_final.contains("image/jpeg") || uri_final.contains("image/pjpeg")) {
                        ext = ".jpg";
                    } else if (uri_final.contains("image/png")) {
                        ext = ".png";
                    } else if (uri_final.contains("image/tiff")) {
                        ext = ".tif";
                    } else if (uri_final.contains("image/svg+xml")) {
                        ext = ".svg";
                    }
                    if (uri_final.contains("base64")) {
                        isBase64 = true;
                    }
                    File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), System.currentTimeMillis() + ext);
                    FileOutputStream outstream = null;
                    try {
                        byte[] out_array;
                        outstream = new FileOutputStream(out);
                        if (out.canWrite()) {
                            if (!isBase64) {
                                out_array = Hex.hexStringToByteArray(uri_final.substring(uri_final.lastIndexOf(",") + 1, uri_final.length()));
                            } else {
                                out_array = Base64.decode(uri_final.substring(uri_final.lastIndexOf(",") + 1, uri_final.length()), Base64.DEFAULT);
                            }

                            outstream.write(out_array);
                        }
                    } catch (FileNotFoundException e) {
                        Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                    } catch (IOException e) {
                        Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                    } catch (IllegalArgumentException e) {
                        Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                    } finally {
                        if (outstream != null) {
                            try {
                                outstream.close();
                            } catch (IOException e) {
                                Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                            }
                        }
                    }

                } else {
                    //Use the download manager
                    final DownloadManager dm = (DownloadManager) owner.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(uri_final));

                    //Check if we're writing to internal or external
                    PackageManager pm = owner.getContext().getPackageManager();
                    if (pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, owner.getContext().getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, uri_final.split("/")[uri_final.split("/").length - 1]);
                        try {
                            r.allowScanningByMediaScanner();
                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            dm.enqueue(r);
                        } catch (IllegalStateException ex) {
                            Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                        }
                    } else {
                        Clog.d(Clog.mraidLogTag, Clog.getString(R.string.store_picture_error));
                    }
                }

                owner.fireAdClicked();
            }
        });

        builder.setNegativeButton(R.string.store_picture_decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Nothing needs to be done
            }
        });

        AlertDialog d = builder.create();
        d.show();


    }

    private void playVideo(ArrayList<BasicNameValuePair> parameters) {
        String uri = null;
        for (BasicNameValuePair bnvp : parameters) {
            if (bnvp.getName().equals("uri")) {
                uri = bnvp.getValue();
            }
        }
        if (uri == null) {
            Clog.d(Clog.mraidLogTag, Clog.getString(R.string.play_vide_no_uri));
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            i.setDataAndType(Uri.parse(URLDecoder.decode(uri, "UTF-8")), "video/mp4");
        } catch (UnsupportedEncodingException e) {
            Clog.d(Clog.mraidLogTag, Clog.getString(R.string.unsupported_encoding));
            return;
        }
        try {
            owner.getContext().startActivity(i);
            owner.fireAdClicked();
        } catch (ActivityNotFoundException e) {
            return;
        }
    }

    private void createCalendarEvent(ArrayList<BasicNameValuePair> parameters) {
        W3CEvent event = null;
        try {
            if (parameters != null && parameters.size() > 0) {
                event = W3CEvent.createFromJSON(URLDecoder.decode(parameters.get(0).getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            return;
        }


        if (event != null) {
            try {
                Intent i = event.getInsertIntent();
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                owner.getContext().startActivity(i);
                owner.fireAdClicked();
                Clog.d(Clog.mraidLogTag, Clog.getString(R.string.create_calendar_event));
            } catch (ActivityNotFoundException e) {

            }
        }

    }

    private AdActivity.OrientationEnum parseForceOrientation(String value) {
        AdActivity.OrientationEnum orientation = AdActivity.OrientationEnum.none;
        if (value.equals("landscape")) {
            orientation = AdActivity.OrientationEnum.landscape;
        } else if (value.equals("portrait")) {
            orientation = AdActivity.OrientationEnum.portrait;
        } // default is none anyway, so no need to check
        return orientation;
    }

    @SuppressWarnings({"UnusedDeclaration", "UnusedAssignment"})
    private void setOrientationProperties(ArrayList<BasicNameValuePair> parameters) {
        boolean allow_orientation_change = true;
        AdActivity.OrientationEnum orientation = AdActivity.OrientationEnum.none;

        for (BasicNameValuePair bnvp : parameters) {
            if (bnvp.getName().equals("allow_orientation_change")) {
                allow_orientation_change = Boolean.parseBoolean(bnvp.getValue());
            } else if (bnvp.getName().equals("force_orientation")) {
                orientation = parseForceOrientation(bnvp.getValue());
            }
        }

        // orientationProperties only affects expanded state
        if (expanded) {
            Activity containerActivity = owner.isFullScreen
                    ? getFullscreenActivity() : (Activity) ViewUtil.getTopContext(owner);
            if (allow_orientation_change) {
                AdActivity.unlockOrientation(containerActivity);
            } else {
                // forceOrientation only applies to pre-expansion, so don't use here
                AdActivity.lockToCurrentOrientation(containerActivity);
            }
        }
    }

    public enum CUSTOM_CLOSE_POSITION {
        top_left,
        top_right,
        center,
        bottom_left,
        bottom_right,
        top_center,
        bottom_center
    }

    private void resize(ArrayList<BasicNameValuePair> parameters) {
        int w = -1;
        int h = -1;
        int offset_x = 0;
        int offset_y = 0;
        String custom_close_position = "top-right";
        boolean allow_offscrean = true;
        for (BasicNameValuePair bnvp : parameters) {
            try {
                if (bnvp.getName().equals("w")) {
                    w = Integer.parseInt(bnvp.getValue());
                } else if (bnvp.getName().equals("h")) {
                    h = Integer.parseInt(bnvp.getValue());
                } else if (bnvp.getName().equals("offset_x")) {
                    offset_x = Integer.parseInt(bnvp.getValue());
                } else if (bnvp.getName().equals("offset_y")) {
                    offset_y = Integer.parseInt(bnvp.getValue());
                } else if (bnvp.getName().equals("custom_close_position")) {
                    custom_close_position = bnvp.getValue();
                } else if (bnvp.getName().equals("allow_offscreen")) {
                    allow_offscrean = Boolean.parseBoolean(bnvp.getValue());
                }
            } catch (NumberFormatException e) {
                Clog.d(Clog.mraidLogTag, Clog.getString(R.string.number_format));
                return;
            }
        }
        //If the resized ad is larger than the screen, reject with great prejudice
        if (w > screenWidth && h > screenHeight) {
            this.owner.loadUrl("javascript:mraid.util.errorEvent('Resize called with resizeProperties larger than the screen.', 'mraid.resize()')");
            return;
        }

        CUSTOM_CLOSE_POSITION cp_enum = CUSTOM_CLOSE_POSITION.top_right;
        try {
            cp_enum = CUSTOM_CLOSE_POSITION.valueOf(custom_close_position.replace('-', '_'));
        } catch (IllegalArgumentException e) {
        } //Default case is used


        Clog.d(Clog.mraidLogTag, Clog.getString(R.string.resize, w, h, offset_x, offset_y, custom_close_position, allow_offscrean));
        this.owner.resize(w, h, offset_x, offset_y, cp_enum, allow_offscrean);

        owner.fireAdClicked();

        this.owner
                .loadUrl("javascript:window.mraid.util.stateChangeEvent('resized');");
        resized = true;

    }

    private void setMaxSize() {
        if (owner.getContext() instanceof Activity) {
            Activity a = ((Activity) owner.getContext());

            int[] screenSize = ViewUtil.getScreenSizeAsPixels(a);
            int maxWidth = screenSize[0];
            int maxHeight = screenSize[1];

            // subtract status bar from total screen size
            int contentViewTop = a.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
            maxHeight -= contentViewTop;

            //Convert to DPs
            final float scale = a.getResources().getDisplayMetrics().density;
            maxHeight = (int) ((maxHeight / scale) + 0.5f);
            maxWidth = (int) ((maxWidth / scale) + 0.5f);

            owner.loadUrl("javascript:window.mraid.util.setMaxSize(" + maxWidth + ", " + maxHeight + ")");
        }
    }

    private void setScreenSize() {
        if (owner.getContext() instanceof Activity) {
            int[] screenSize = ViewUtil.getScreenSizeAsDP(((Activity) owner.getContext()));
            screenWidth = screenSize[0];
            screenHeight = screenSize[1];

            owner.loadUrl("javascript:window.mraid.util.setScreenSize(" + screenWidth + ", " + screenHeight + ")");
        }
    }

    void fireViewableChangeEvent() {
        boolean isCurrentlyViewable = owner.isViewable();
        if (isViewable != isCurrentlyViewable) {
            this.onViewableChange(isCurrentlyViewable);
        }
    }

    void onOrientationChanged(int orientation) {
        if (lastRotation != orientation) {
            lastRotation = orientation;
            setMaxSize();
            setScreenSize();
        }
    }

    AdActivity getFullscreenActivity() {
        return fullscreenActivity;
    }

    void setFullscreenActivity(AdActivity fullscreenActivity) {
        this.fullscreenActivity = fullscreenActivity;
    }

    ViewGroup getDefaultContainer() {
        return defaultContainer;
    }

    void setDefaultContainer(ViewGroup defaultContainer) {
        this.defaultContainer = defaultContainer;
    }
}

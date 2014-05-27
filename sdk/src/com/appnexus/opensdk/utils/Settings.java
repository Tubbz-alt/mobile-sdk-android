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

import android.location.Location;
import android.os.Build;
import com.appnexus.opensdk.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class Settings {
    public String hidmd5 = null;
    public String hidsha1 = null;
    public String carrierName = null;
    public String aaid = null;
    public boolean limitTrackingEnabled = false;

    public final String deviceMake = Build.MANUFACTURER;
    public final String deviceModel = Build.MODEL;

    public String app_id = null;

    public boolean test_mode = false;
    public String ua = null;
    public boolean first_launch;
    public final String sdkVersion = "1.16";

    public String mcc;
    public String mnc;
    public final String dev_timezone = TimeZone.getDefault().getID();
    public final String language = Locale.getDefault().getLanguage();

    public boolean locationEnabled = true;
    public Location location = null;

    public HashMap<String, String> externalMediationClasses = new HashMap<String, String>();
    public HashSet<String> invalidNetworks = new HashSet<String>();

    // STATICS
    public static final int HTTP_CONNECTION_TIMEOUT = 15000;
    public static final int HTTP_SOCKET_TIMEOUT = 20000;

    public static final int FETCH_THREAD_COUNT = 4;

    public static final int DEFAULT_REFRESH = 0;
    public static final int MIN_REFRESH_MILLISECONDS = 15000;
    public static final int DEFAULT_INTERSTITIAL_CLOSE_BUTTON_DELAY = 10000;

    public static final long MEDIATED_NETWORK_TIMEOUT = 15000;

    public static final String AN_UUID = "uuid2";
    public static String BASE_URL = "http://mediation.adnxs.com/";
    public static String COOKIE_DOMAIN = BASE_URL;
    public static String REQUEST_BASE_URL = BASE_URL + "mob?";
    public static String INSTALL_BASE_URL = BASE_URL + "install?";

    private static Settings settings_instance = null;

    public static Settings getSettings() {
        if (settings_instance == null) {
            settings_instance = new Settings();
            Clog.v(Clog.baseLogTag, Clog.getString(R.string.init));
        }
        return settings_instance;
    }

    private Settings() {

    }
}

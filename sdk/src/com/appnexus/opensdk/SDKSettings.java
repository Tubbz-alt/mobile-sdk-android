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

import android.location.Location;
import com.appnexus.opensdk.utils.Settings;

/**
 * Global static functions that apply to all SDK views and calls.
 */
public class SDKSettings {

    private SDKSettings() {

    }

    /**
     * Sets the Android Advertising ID to be passed in the ad request.
     *
     * @param aaid the android advertising id value.
     * @param limitTrackingEnabled whether limitTracking is enabled or not.
     */
    public static void setAAID(String aaid, boolean limitTrackingEnabled) {
        Settings.getSettings().aaid = aaid;
        Settings.getSettings().limitTrackingEnabled = limitTrackingEnabled;
    }

    /**
     * Retrieve the current Android Advertising ID to be reported to the ad
     * server. If the value is null, then either a non-null value has not
     * been passed to {@link #setAAID}
     *
     * @return The AAID to be passed in the ad request
     */
    public static String getAAID() {
        return Settings.getSettings().aaid;
    }

    /**
     * Sets whether or not location (latitude, longitude) is retrieved and
     * passed in the ad request. This includes whether location is sent to any
     * 3rd party networks.
     *
     * @param enabled whether to enable location or not. default is true
     */
    public static void setLocationEnabled(boolean enabled) {
        Settings.getSettings().locationEnabled = enabled;
    }

    /**
     * Returns true if the ad server calls will include location information
     * or false otherwise.
     */
    public static boolean getLocationEnabled() {
        return Settings.getSettings().locationEnabled;
    }

    /**
     * Retrieve the current location reported to the ad server.
     * If the value is null either location has not yet been retrieved
     * or location reporting has been disabled via
     * {@link #setLocationEnabled(boolean)}
     *
     * @return The location used in the last ad call.
     */
    public static Location getLocation() {
        return Settings.getSettings().location;
    }

    /**
     * Overrides the SDKs automatic location retrieval.
     * If the passed in location value is not null and location is enabled,
     * then the passed in location value will be sent to the ad server.
     * If the location parameter is null then the automatic location retrieval
     * will be used if and only if {@link #getLocationEnabled()}.
     *
     * @param location The location value to use in the ad call (may be null)
     */
    public static void setLocation(Location location) {
        Settings.getSettings().location =
                getLocationEnabled() ? location : null;
    }

}

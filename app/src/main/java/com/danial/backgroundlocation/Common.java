package com.danial.backgroundlocation;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

public class Common {
    public static final String KEY_REQUESTING_LOCATION_UPDATES = "LocationUpdatesEnable";

    public static String getLocationText(Location mLocation) {
        return mLocation == null ? "unknown location" : new StringBuilder()
                .append(mLocation.getLatitude())
                .append("/")
                .append(mLocation.getLongitude())
                .toString();
    }

    public static CharSequence getLocationTitle(MyBackgroundService myBackgroundService) {
        return String.format(String.format("Location updated : " + DateFormat.getDateInstance().format(new Date())));
    }

    public static void setRequestingLocationUpdates(Context context, boolean value) {

        PreferenceManager
                .getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, value)
                .apply();
    }

    public static boolean requestingLocationUpdates(Context context) {


        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }
}

package com.danial.backgroundlocation;

import android.location.Location;

class SendLocationToActivity {
    private Location mLocation;

    public Location getmLocation() {
        return mLocation;
    }

    public void setmLocation(Location mLocation) {
        this.mLocation = mLocation;
    }

    public SendLocationToActivity(Location mLocation) {
        this.mLocation = mLocation;
    }
}

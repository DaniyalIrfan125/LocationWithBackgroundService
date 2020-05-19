package com.danial.backgroundlocation;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Button requestLocation, removeLocation;
    MyBackgroundService myBackgroundService = null;
    boolean aBound = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBackgroundService.LocalBinder binder = (MyBackgroundService.LocalBinder) service;
            myBackgroundService = binder.getService();
            aBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBackgroundService = null;
            aBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (aBound) {
            unbindService(mServiceConnection);
            aBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(this)
                .withPermissions(Arrays.asList(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

                        requestLocation = findViewById(R.id.btn_requestLocation);
                        removeLocation = findViewById(R.id.btn_removeLocation);


                        requestLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                myBackgroundService.requestLocationUpdates();
                            }
                        });

                        removeLocation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                myBackgroundService.removeLocationUpdates();
                            }
                        });

                        setButtonState(Common.requestingLocationUpdates(MainActivity.this));
                        bindService(new Intent(MainActivity.this, MyBackgroundService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                    }
                }).check();

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Common.KEY_REQUESTING_LOCATION_UPDATES)) {
            setButtonState(sharedPreferences.getBoolean(Common.KEY_REQUESTING_LOCATION_UPDATES, false));
        }
    }

    private void setButtonState(boolean aBoolean) {
        if (aBoolean) {
            requestLocation.setEnabled(false);
            removeLocation.setEnabled(true);
        } else {
            requestLocation.setEnabled(true);
            removeLocation.setEnabled(false);

        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLocationListen(SendLocationToActivity event) {
        if (event != null) {
            String data = new StringBuilder()
                    .append(event.getmLocation().getLatitude())
                    .append("/")
                    .append(event.getmLocation().getLongitude())
                    .toString();

            Toast.makeText(myBackgroundService, data, Toast.LENGTH_SHORT).show();
        }

    }

}

package com.danial.backgroundlocation;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;

public class MyBackgroundService extends Service {

    private static final String CHANNEL_ID = "my_channel";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "com.danial.backgroundlocation" + ".started_from_notification";
    private final IBinder mBinder = new LocalBinder();
    private static final long UPDATE_INTERVAL_IN_MIL = 1000;
    private static final long FASTES_UPDATE_INTERVAL_IN_MIL = UPDATE_INTERVAL_IN_MIL / 2;
    private static final int NOTI_ID = 1223;
    private Boolean mChangingConfiguration = false;
    private NotificationManager notificationManager;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Handler serviceHandler;
    private Location mLocation;

    public MyBackgroundService() {

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);

        if (startedFromNotification) {

            removeLocationUpdates();
            stopSelf();
        }


        return START_NOT_STICKY;
    }

    public void removeLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            Common.setRequestingLocationUpdates(this, false);

        } catch (SecurityException e) {
            Common.setRequestingLocationUpdates(this, true);
            Log.e("result", "lost location permission could not remove updates");
        }
    }

    @Override
    public void onCreate() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                onNewLocation(locationResult.getLastLocation());
            }
        };


        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread("Danial");
        handlerThread.start();
        serviceHandler = new Handler(handlerThread.getLooper());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationManager.createNotificationChannel(mChannel);
        }

    }

    private void getLastLocation() {
        try {
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null)
                        mLocation = task.getResult();
                    else
                        Log.d("result", "Failed to get location");
                }

            });
        } catch (SecurityException e) {
            Log.e("result", e.getMessage());
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MIL);
        locationRequest.setFastestInterval(FASTES_UPDATE_INTERVAL_IN_MIL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location lastLocation) {
        mLocation = lastLocation;
        EventBus.getDefault().postSticky(new SendLocationToActivity(mLocation));

        if (serviceIsRunningInForeGround(this))
            notificationManager.notify(NOTI_ID, getNotification());

    }

    private Notification getNotification() {

        Intent intent = new Intent(this, MyBackgroundService.class);
        String text = Common.getLocationText(mLocation);

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch_black_24dp, "launch", activityPendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp, "remove", servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Common.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);

        }

        return builder.build();
    }

    private boolean serviceIsRunningInForeGround(Context context) {

        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE))
            if (getClass().getName().equals(service.service.getClassName()))
                if (service.foreground)
                    return true;

        return false;
    }

    public void requestLocationUpdates() {
        Common.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), MyBackgroundService.class));
        try{
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        }
        catch (SecurityException e){
            Log.e("result","Lost location request");
        }
    }

    public class LocalBinder extends Binder {
        MyBackgroundService getService() {
            return MyBackgroundService.this;
        }


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {

        stopForeground(true);
        mChangingConfiguration = false;

        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (!mChangingConfiguration && Common.requestingLocationUpdates(this))
            startForeground(NOTI_ID, getNotification());

        return true;
    }

    @Override
    public void onDestroy() {
        serviceHandler.removeCallbacks(null);

        super.onDestroy();
    }
}

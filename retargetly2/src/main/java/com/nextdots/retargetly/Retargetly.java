package com.nextdots.retargetly;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;

import com.nextdots.retargetly.api.ApiController;
import com.nextdots.retargetly.data.models.Event;
import com.nextdots.retargetly.data.models.RetargetlyParams;
import com.nextdots.retargetly.receivers.NetworkBroadCastReceiver;
import com.nextdots.retargetly.utils.GeoUtils;
import com.nextdots.retargetly.utils.RetargetlyUtils;
import com.nextdots.retargetly.utils.TaskId;


import static android.content.Context.LOCATION_SERVICE;
import static android.content.Intent.ACTION_VIEW;
import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;


public class Retargetly implements Application.ActivityLifecycleCallbacks, LocationListener {

    static public Application application = null;
    public static RetargetlyParams params;
    private boolean isFirst = false;

    private boolean sendGeoData;
    private boolean forceGPS;

    static public String source_hash;
    private String manufacturer;
    private String model;
    private String idiome;


    private ApiController apiController;
    private Location lastLocation;
    private GeoUtils geoUtils;
    private Class classDeeplink;

    public static void init(Application application, String source_hash) {
        new Retargetly(application, source_hash);
    }

    public static void init(Application application, String source_hash, Class classDeeplink) {
        new Retargetly(application, source_hash, classDeeplink);
    }

    public static void init(Application application, String source_hash, boolean forceGPS) {
        new Retargetly(application, source_hash, forceGPS);
    }

    public static void init(Application application, String source_hash, Class classDeeplink , boolean forceGPS) {
        new Retargetly(application, source_hash, classDeeplink, forceGPS);
    }
    public static void init(Application application, String source_hash, boolean forceGPS, boolean sendGeoData) {
        new Retargetly(application, source_hash, forceGPS, sendGeoData);
    }
    public static void init(Application application, String source_hash, Class classDeeplink, boolean forceGPS, boolean sendGeoData) {
        new Retargetly(application, source_hash, classDeeplink, forceGPS, sendGeoData);
    }
    public static void init(Application application, RetargetlyParams params){
        new Retargetly(application, params);
    }

    private Retargetly(Application application, String source_hash) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .build());
    }

    private Retargetly(Application application, String source_hash, Class classDeeplink) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .classDeeplink(classDeeplink)
                .build());
    }

    private Retargetly(Application application, String source_hash, boolean forceGPS) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .forceGPS(forceGPS)
                .build());
    }

    private Retargetly(Application application, String source_hash, Class classDeeplink,
                       boolean forceGPS) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .classDeeplink(classDeeplink)
                .forceGPS(forceGPS)
                .build());
    }

    private Retargetly(Application application, String source_hash, boolean forceGPS,
                       boolean sendGeoData) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .forceGPS(forceGPS)
                .sendGeoData(sendGeoData)
                .build());
    }

    private Retargetly(Application application, String source_hash, Class classDeeplink,
                       boolean forceGPS, boolean sendGeoData) {
        this(application, new RetargetlyParams.Builder(source_hash)
                .classDeeplink(classDeeplink)
                .forceGPS(forceGPS)
                .sendGeoData(sendGeoData)
                .build());
    }

    public Retargetly(Application application, RetargetlyParams params) {
        this.application = application;
        this.params = params;
        RetargetlyUtils.injectParam(application, params);
        this.manufacturer =  RetargetlyUtils.getManufacturer();
        this.model = RetargetlyUtils.getDeviceName();
        this.idiome = RetargetlyUtils.getLanguage();
        this.source_hash = params.getSourceHash();
        this.application.registerActivityLifecycleCallbacks(this);
        this.forceGPS = params.isForceGPS();
        this.sendGeoData = params.isSendGeoData();
        this.classDeeplink = params.getClassDeeplink();
        apiController = new ApiController();
        getDefaultParams();
    }

    public void setClassDeeplink(Class classDeeplink){
        this.classDeeplink = classDeeplink;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!isFirst) {
            hasPermission(activity);
            isFirst = true;
            RetargetlyUtils.LogR("First Activity " + activity.getClass().getSimpleName());
            if(classDeeplink==null){
                getDeeplink(activity);
            }
        }
        if(classDeeplink!=null) {
            final Class deep = activity.getClass();
            if (classDeeplink == deep) {
                getDeeplink(activity);
            }
        }
    }

    private void getDeeplink(Activity activity){
        final Intent intent = activity.getIntent();
        final String action = intent.getAction();
        final Uri data = intent.getData();
        if (data != null && action != null && action.equalsIgnoreCase(ACTION_VIEW))
            RetargetlyUtils.callEventDeeplink(data.toString());
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }


    @Override
    public void onLocationChanged(Location location) {
        RetargetlyUtils.LogR("GPS onLocationChanged");
        RetargetlyUtils.LogR("Send geo event");
        sendGeoEvent(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    private void getDefaultParams() {
        getIdThread(application, new ApiController.ListenerSendInfo() {
            @Override
            public void finishRequest() {
                if (sendGeoData && RetargetlyUtils.hasLocationEnabled(application))
                    apiController.callInitData(source_hash, new ApiController.ListenerSendInfo() {
                        @Override
                        public void finishRequest() {
                            application.sendBroadcast(new Intent("changedata"));
                            initGPS();
                        }
                    });
                getIp();
            }
        });
    }

    private void getIp() {
        if(params.isSendIpEnabled() || params.isSendNameWifiEnabled()){
            NetworkBroadCastReceiver.getIp(application, params.isSendIpEnabled(),
                    new ApiController.ListenerSendInfo() {
                @Override
                public void finishRequest() {
                    application.sendBroadcast(new Intent("changedata"));
                    sendOpenEvent();
                }
            });
            IntentFilter intentFilter = new IntentFilter(CONNECTIVITY_ACTION);
            application.registerReceiver(new NetworkBroadCastReceiver(),
                    intentFilter);
        }else{
            sendOpenEvent();
        }
    }

    private void sendOpenEvent() {
        RetargetlyUtils.LogR("Send open event");
        String apps = RetargetlyUtils.getInstalledApps(application);
        String country = RetargetlyUtils.getCurrentCountryIso();
        apiController.callCustomEvent(
                new Event(RetargetlyUtils.getUID(),source_hash,
                        application.getPackageName(), manufacturer, model, idiome,
                        apps, application.getString(R.string.app_name), country));
    }

    private void sendGeoEvent(Location location) {
        lastLocation = location;
        if ((location.getLatitude() != 0 && location.getLongitude() != 0) && sendGeoData) {
            RetargetlyUtils.LogR("Latitude: " + location.getLatitude());
            RetargetlyUtils.LogR("Longitude: " + location.getLongitude());
            RetargetlyUtils.LogR("Accuracy: " + location.getAccuracy());
            RetargetlyUtils.LogR("Alt: " + location.getAltitude());
            RetargetlyUtils.callEventCoordinate(
                    String.valueOf(location.getLatitude()),
                    String.valueOf(location.getLongitude()),
                    String.valueOf(location.getAccuracy()),
                    String.valueOf(location.getAltitude()));
            geoUtils.cancelCount();
            geoUtils.initCount();
        }
    }

    private void initGPS() {
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = ApiController.motionTreshold; //mts

        long MIN_TIME_BW_UPDATES = 1000 *  ApiController.motionFrequency; // milisegundos.

        RetargetlyUtils.LogR("Init GPS Distancia: " + ApiController.motionTreshold + " Frecuencia " +
                ApiController.motionFrequency);

        LocationManager manager = (LocationManager) application.getSystemService(LOCATION_SERVICE);
        if (manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                LocationManager locationManager = (LocationManager) application
                        .getSystemService(LOCATION_SERVICE);

                if (locationManager != null &&
                        ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    geoUtils = new GeoUtils(GeoUtils.SecondsToMilliseconds(ApiController.motionDetectionFrequency),
                            new GeoUtils.ListenerTask() {
                                @Override
                                public void next() {
                                    if (lastLocation != null) {
                                        RetargetlyUtils.LogR("Send geo event lastlocation");
                                        sendGeoEvent(lastLocation);
                                    }
                                }
                            });
                    geoUtils.initCount();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hasPermission(Activity activity) {
        if (sendGeoData && forceGPS)
            RetargetlyUtils.checkPermissionGps(activity);
    }

    public void getIdThread(final Application context, final ApiController.ListenerSendInfo listenerSendInfo) {
        final TaskId taskGetId= new TaskId(context, listenerSendInfo);
        taskGetId.execute();
    }

}
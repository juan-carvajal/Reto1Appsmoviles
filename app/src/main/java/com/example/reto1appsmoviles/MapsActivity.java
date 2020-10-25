package com.example.reto1appsmoviles;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.reto1appsmoviles.comm.HTTPSWebUtilDomi;
import com.example.reto1appsmoviles.models.PotHole;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private Button addPotHoleBTN;
    private TextView holesTextView;
    private UUID connectionID;
    Context mContext;
    LocationManager locationManager;
    Geocoder geocoder;
    private HTTPSWebUtilDomi https;
    private Gson gson;
    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;
    private Map<Marker, Map.Entry<String, PotHole>> markersMap = new HashMap<Marker, Map.Entry<String, PotHole>>();

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                new Thread(
                        ()->{
                            try {
                                Log.i(TAG,"Starting update");
                                String url = "https://appmoviles-47f25.firebaseio.com/userconns.json";
                                String response = https.GETrequest(url);
                                Type type = new TypeToken<HashMap<String, LatLng>>(){}.getType();
                                HashMap<String, LatLng> users = gson.fromJson(response, type);


                                url = "https://appmoviles-47f25.firebaseio.com/potholes.json";
                                response = https.GETrequest(url);
                                type = new TypeToken<HashMap<String, PotHole>>(){}.getType();
                                HashMap<String, PotHole> potHoles = gson.fromJson(response, type);
                                Runnable updateMapRunnable = new Runnable(){
                                    @Override
                                    public void run() {
                                        mMap.clear();
                                        markersMap.clear();
                                        if(users != null){
                                            for (Map.Entry<String, LatLng> entry : users.entrySet()) {
                                                String key = entry.getKey();
                                                if(!key.equals(connectionID.toString())){
                                                    LatLng value = entry.getValue();
                                                    MarkerOptions userIndicator = new MarkerOptions()
                                                            .position(value).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                                    mMap.addMarker(userIndicator);
                                                }

                                            }
                                        }


                                        PotHole nearest = null;
                                        double minDist = Double.MAX_VALUE;
                                        if(potHoles != null){
                                            for (Map.Entry<String, PotHole> entry : potHoles.entrySet()) {
                                                String key = entry.getKey();
                                                PotHole value = entry.getValue();

                                                if(mLastKnownLocation != null){
                                                    double dist = distance(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude(),value.latitude,value.longitude);
                                                    if(dist<minDist && value.confirmed == true){
                                                        nearest =value;
                                                        minDist =dist;
                                                    }
                                                }

                                                if(value.confirmed == true){
                                                    MarkerOptions potHoleIndicator = new MarkerOptions()
                                                            .position(new LatLng(value.latitude,value.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.outline_error_black_18dp));
                                                    Marker marker=mMap.addMarker(potHoleIndicator);
                                                    markersMap.put(marker, entry);
                                                }else{
                                                    MarkerOptions potHoleIndicator = new MarkerOptions()
                                                            .position(new LatLng(value.latitude,value.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.outline_error_outline_black_18dp));
                                                    Marker marker=mMap.addMarker(potHoleIndicator);
                                                    markersMap.put(marker, entry);
                                                }



                                            }
                                        }


                                        if(nearest != null){
                                            holesTextView.setText("Hueco a "+(int)(minDist*1000) +" metros.");
                                        }
                                    }
                                };

                                mHandler.post(updateMapRunnable);



                            }catch(Exception e){
                                Log.e(TAG, "Exception: %s", e);
                            }

                        }
                ).start();
            }finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mStatusChecker);
        try{
            new Thread(
                    ()->{
                        try {
                            String url = "https://appmoviles-47f25.firebaseio.com/userconns/"+ connectionID.toString() +".json";
                            https.DELETErequest(url);
                        }catch(Exception e){
                            Log.e(TAG, "Exception: %s", e);
                        }

                    }
            ).start();

        }catch(Exception e){
            Log.e(TAG, "Exception: %s", e);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStatusChecker);
        try{
            new Thread(
                    ()->{
                        try {
                            String url = "https://appmoviles-47f25.firebaseio.com/userconns/"+ connectionID.toString() +".json";
                            https.DELETErequest(url);
                        }catch(Exception e){
                            Log.e(TAG, "Exception: %s", e);
                        }

                    }
            ).start();
        }catch(Exception e){
            Log.e(TAG, "Exception: %s", e);
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mStatusChecker);
        try{
            new Thread(
                    ()->{
                        try {
                            String url = "https://appmoviles-47f25.firebaseio.com/userconns/"+ connectionID.toString() +".json";
                            https.DELETErequest(url);
                        }catch(Exception e){
                            Log.e(TAG, "Exception: %s", e);
                        }

                    }
            ).start();
        }catch(Exception e){
            Log.e(TAG, "Exception: %s", e);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        https = new HTTPSWebUtilDomi();
        gson = new Gson();
        this.connectionID = UUID.randomUUID();
        holesTextView = findViewById(R.id.closePotHole);
        holesTextView.setText("No hay huecos cerca.");
        addPotHoleBTN = findViewById(R.id.button);
        addPotHoleBTN.setOnClickListener(
                (v)->{
                    new Thread(
                            ()->{
                                try {
                                    List<Address> addr = geocoder.getFromLocation(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude(),1);
                                    PotHole hole = new PotHole(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude(),addr.get(0).getAddressLine(0),false);
                                    CreatePotHoleDialogFragment dialog = new CreatePotHoleDialogFragment(hole,this);
                                    dialog.show(getSupportFragmentManager(),"");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                    ).start();



                }
        );
        mContext = this;
        mHandler = new Handler();
        this.geocoder=new Geocoder(mContext);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                2000,
                10, this);

        mStatusChecker.run();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }


    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        new Thread(
                ()->{
                    try {
                        Log.i(TAG,"Starting update of location");
                        String url = "https://appmoviles-47f25.firebaseio.com/userconns/"+ connectionID.toString() +".json";
                        https.PUTrequest(url,gson.toJson(new LatLng(mLastKnownLocation.getLatitude(),mLastKnownLocation.getLongitude())));
                    }catch(Exception e){
                        Log.e(TAG, "Exception: %s", e);
                    }

                }
        ).start();
        this.mLastKnownLocation=location;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(),
                        location.getLongitude()), DEFAULT_ZOOM));
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i(TAG,"ENTRANDO AL LISTENER");
        if(this.markersMap.containsKey(marker) && this.markersMap.get(marker).getValue().confirmed == false){
            new Thread(
                    ()->{
                        try {
                            PotHole toUpdate = this.markersMap.get(marker).getValue();
                            toUpdate.confirmed=true;
                            String url = "https://appmoviles-47f25.firebaseio.com/potholes/"+ this.markersMap.get(marker).getKey() +".json";
                            https.PUTrequest(url,gson.toJson(toUpdate));
                        }catch(Exception e){
                            Log.e("Exception: %s", e.getMessage());
                        }

                    }
            ).start();
        }

        return true;
    }
}
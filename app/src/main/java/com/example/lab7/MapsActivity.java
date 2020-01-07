package com.example.lab7;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener, SensorEventListener {

    //sensores
    static public SensorManager mSensorManager;
    static Sensor Sensor;
    static long lastUpdate = 0;
    static float x;
    static float y;
    static float z;

    //memory

    private final String TASKS_JSON_FILE = "tasks.json";








    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList = new ArrayList<>();

        //sensores
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        mSensorManager.registerListener(this,Sensor, SensorManager.SENSOR_DELAY_NORMAL);

        Button clearButton = (Button) findViewById(R.id.button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i=0; i<markerList.size(); i++) {
                    markerList.get(i).remove();
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        if(Sensor != null)
            mSensorManager.unregisterListener(this, Sensor);
    }

    private void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
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
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();
        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null && mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }

        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
        restoreFromJson();
    }




    @Override
    public void onMapLongClick(LatLng latLng) {
        float distance = 0f;

        if(markerList.size() > 0) {
            Marker lastMarker = markerList.get(markerList.size() - 1);
            float[] tmpDis = new float[3];

            Location.distanceBetween(lastMarker.getPosition().latitude, lastMarker.getPosition().longitude,
                    latLng.latitude, latLng.longitude, tmpDis);
            distance = tmpDis[0];

        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_map))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f) Distance: %.2f", latLng.latitude, latLng.longitude, distance)));

        markerList.add(marker);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        final FloatingActionButton fab_dot = (FloatingActionButton) findViewById(R.id.fab_dot);
        final FloatingActionButton fab_x = (FloatingActionButton) findViewById(R.id.fab_x);
        fab_dot.show();
        fab_x.show();

        fab_x.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_dot.hide();
                fab_x.hide();

                TextView mytxt=(TextView) findViewById(R.id.textView);
                mytxt.setVisibility(View.INVISIBLE);

            }
        });

        fab_dot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Acceleration: \n x: ").append(x).append("  y: ")
                        .append(y);
                TextView mytxt=(TextView) findViewById(R.id.textView);
                mytxt.setText(stringBuilder);
                mytxt.setVisibility(View.VISIBLE);
            }
        });
        return false;
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult != null){
                    if(gpsMarker != null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_map))
                            .alpha(0.8f)
                            .title("Current Location"));


                }
            }
        };
    }

    public void zoomInClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int i) {

    }

    @Override
    protected void onResume(){
        super.onResume();
        if(Sensor != null)
            mSensorManager.registerListener(this, Sensor, 100000);
    }


    private void saveTasksToJson(){
        Gson gson = new Gson();
        for (int i=0; i<markerList.size(); i++) {
            String listJson = gson.toJson(markerList.get(i).getPosition());
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(TASKS_JSON_FILE, MODE_PRIVATE);
                FileWriter writer = new FileWriter(outputStream.getFD());
                writer.write(listJson);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(TASKS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<LatLng>() {
            }.getType();
            LatLng o = gson.fromJson(readJson, collectionType);
            markerList.clear();
            Marker marker1 = mMap.addMarker(new MarkerOptions()
                    .position(o)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_map))
                    .alpha(0.8f)
                    .title("1"));
            markerList.add(marker1);

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /*
    public class MarkerClass {
        int id;
        double latitude;
        double longitude;



        List<MarkerClass> myList = new ArrayList<Marker>();



        // Constructor Declaration of Class
        public MarkerClass(int id, double latitude,
                   double longitude) {
            this.id = id;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public int getId()
        {
            return id;
        }

        public double getLatitude()
        {
            return latitude;
        }

        // method 3
        public double getLongitude()
        {
            return longitude;
        }

        @Override
        public String toString()
        {
            return(this.getId()+
                    "\n" +
                    this.getLatitude()+"\n" + this.getLongitude());
        }
    } */

    @Override
    protected void onDestroy(){
        super.onDestroy();
        saveTasksToJson();
    }
}

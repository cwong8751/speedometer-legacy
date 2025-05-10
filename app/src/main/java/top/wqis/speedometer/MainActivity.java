package top.wqis.speedometer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import top.wqis.speedometer.model.Coordinate;

public class MainActivity extends AppCompatActivity {

    //region declare variables
    LocationManager locationManager;
    String locationProvider;
    TextView locationInfo;
    TextView speedInfo;
    TextView unitInfo;
    private boolean startRide = false;
    ArrayList<Coordinate> rideTrace = new ArrayList<>();
    private final String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getApplicationContext().getSharedPreferences("operator", MODE_PRIVATE);
        int opMode = sp.getInt("operator_mode", 0);


        if (opMode == 0){
            setContentView(R.layout.activity_main_bike);
        }
        else{
            setContentView(R.layout.activity_main);
        }

        //locationInfo = findViewById(R.id.TVLocationInfo);
        speedInfo = findViewById(R.id.TVspeedInfo);
        unitInfo = findViewById(R.id.TVUnitInfo);

        // Keep device screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set status bar color to be same as main screen color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        // Set nav bar color
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.black));

        findViewById(R.id.rootView).getRootView().setBackgroundColor(ContextCompat.getColor(this, R.color.black));

        // Check app permissions
        if (!EasyPermissions.hasPermissions(this, perms)){
            EasyPermissions.requestPermissions(this, getString(R.string.need_gps_perms),
                    1, perms);
        }

        // Hide gps icon
        ImageView gpsStatus = findViewById(R.id.IVGps);
        gpsStatus.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(1)
    private void getCurrentLocation() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Location location = locationManager.getLastKnownLocation(locationProvider);
            locationManager.requestLocationUpdates(locationProvider, 100, 1, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if(startRide){
                        int speed = Math.round((location.getSpeed() * 18) / 5);
//                        StringBuilder sb = new StringBuilder();
//
//                        sb.append("GPS数据\nLatitude: ");
//                        sb.append(location.getLatitude());
//                        sb.append("\nLongitude: ");
//                        sb.append(location.getLongitude());
//                        sb.append("\nSpeed: ");
//                        sb.append(location.getSpeed());
//                        sb.append("\nRounded speed: ");
//                        sb.append(speed);
//                        sb.append("\nProvider: ");
//                        sb.append(locationProvider);

                        //locationInfo.setText(sb.toString());

                        // If text is too big it goes in two lines, so set the font smaller
                        if (speed >= 100){
                         speedInfo.setTextSize(250);
                        }

                        speedInfo.setText(String.valueOf(speed));

                        // Add gps data to draw trace
                        rideTrace.add(new Coordinate(location.getLatitude(), location.getLongitude(), location.getSpeed()));
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    if (startRide){
                        getCurrentLocation();
                    }
                }

                @Override
                public void onProviderEnabled(String provider) {
                    //locationManager.getLastKnownLocation(provider);

                    // Enable the start button
                    Button btn = findViewById(R.id.BTStartRide);
                    btn.setEnabled(true);

                    // Show gps icon
                    ImageView gpsStatus = findViewById(R.id.IVGps);
                    gpsStatus.setVisibility(View.VISIBLE);

                    Toast.makeText(getApplicationContext(), "GPS已找到", Toast.LENGTH_SHORT).show();

                    if (startRide) {
                        getCurrentLocation();
                    }
                }

                @Override
                public void onProviderDisabled(String provider) {
                    // Disable the start button
                    Button btn = findViewById(R.id.BTStartRide);
                    btn.setEnabled(false);

                    // Hide gps icon
                    ImageView gpsStatus = findViewById(R.id.IVGps);
                    gpsStatus.setVisibility(View.INVISIBLE);

                    // Stops the ride
                    stopRide(null);

                    Toast.makeText(getApplicationContext(), getString(R.string.gps_disabled), Toast.LENGTH_SHORT).show();

                    speedInfo.setText("GO");
                }
            });
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.need_gps_perms),
                    1, perms);
        }
    }

    public void launchSettings(View view){
       startActivity(new Intent(this, SettingsActivity.class));
    }

    // Starts the ride
    public void startRide(View view){
        startRide = true;

        // Init location provider and criteria.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Use GPS as default provider
        locationProvider = LocationManager.GPS_PROVIDER;

        Log.i("Location provider", locationProvider);

        getCurrentLocation();

        // Show the stop button
        Button stopButton = findViewById(R.id.BTStopRide);
        stopButton.setVisibility(View.VISIBLE);

        // Hide start button
        Button startButton = findViewById(R.id.BTStartRide);
        startButton.setVisibility(View.GONE);

        // Hide trace button
        Button traceButton = findViewById(R.id.BTTrace);
        traceButton.setVisibility(View.GONE);

        // Hide settings button
        Button settingsButton = findViewById(R.id.BTSettings);
        settingsButton.setVisibility(View.GONE);


        // Show gps icon
        ImageView gpsStatus = findViewById(R.id.IVGps);
        gpsStatus.setVisibility(View.VISIBLE);
    }

    // Stops the ride
    public void stopRide(View view){
        startRide = false;

        // Show start button
        Button startButton = findViewById(R.id.BTStartRide);
        startButton.setVisibility(View.VISIBLE);

        // Hide the stop button
        Button stopButton = findViewById(R.id.BTStopRide);
        stopButton.setVisibility(View.GONE);

        // Show trace button
        Button traceButton = findViewById(R.id.BTTrace);
        traceButton.setVisibility(View.VISIBLE);

        // Show settings button
        Button settingsButton = findViewById(R.id.BTSettings);
        settingsButton.setVisibility(View.VISIBLE);

        // Hide the gps icon
        ImageView gpsStatus = findViewById(R.id.IVGps);
        gpsStatus.setVisibility(View.INVISIBLE);


        // Store ride into file

        new Thread(() -> {
            if (rideTrace.size() < 1){
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.ride_unsaved), Toast.LENGTH_SHORT).show());
                return;
            }

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String date = df.format(Calendar.getInstance().getTime());

            File rideInfoDir = new File(getFilesDir(), "/rideInfo");

            rideInfoDir.mkdirs();

            File rideFile = new File(getFilesDir() + "/rideInfo", date + ".txt");

            StringBuilder sb = new StringBuilder();

            // Insert date info
            sb.append(date);
            sb.append("\n");

            for (Coordinate r: rideTrace
            ) {
                sb.append(r.getLat());
                sb.append(",");
                sb.append(r.getLng());
                sb.append("#");
                sb.append(r.getSpeed());
                sb.append("\n");
            }

            String f = sb.toString();

            try{
                FileOutputStream fos = new FileOutputStream(rideFile);
                fos.write(f.getBytes(StandardCharsets.UTF_8));
                fos.close();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.saved_ok), Toast.LENGTH_SHORT).show());
            }
            catch (IOException e){
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.saved_failed), Toast.LENGTH_SHORT).show());
            }

            runOnUiThread(() -> speedInfo.setText(getString(R.string.go)));

            rideTrace.clear();
        }).run();
    }

    public void launchRideList(View view){
        startActivity(new Intent(this, RideListActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check app permissions
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, getString(R.string.need_gps_perms),
                    1, perms);
        }
    }

    //region bell ring
    public void playBellSound(){
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.bell);
        mp.start();
    }

    public void ringBell(View view){
        playBellSound();
    }

    public void ringBellLeft(View view){
        playBellSound();
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.left);
        mp.start();
    }

    public void ringBellRight(View view){
        playBellSound();
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.right);
        mp.start();
    }
}
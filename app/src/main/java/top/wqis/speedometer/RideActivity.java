package top.wqis.speedometer;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.utils.CoordinateConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.wqis.speedometer.model.Coordinate;

public class RideActivity extends AppCompatActivity {

    private MapView mapView;
    private BaiduMap baiduMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
        SDKInitializer.initialize(getApplicationContext());
        SDKInitializer.setCoordType(CoordType.BD09LL);
        setContentView(R.layout.activity_ride);

        mapView = findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();

        // Set home display upwards
        assert getSupportActionBar() != null;
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"#FFFFFF\">" + getString(R.string.ride_data) + "</font>"));
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.light_green)));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.light_green));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        // Disable map zoom controls
        mapView.showZoomControls(false);
        mapView.setLogoPosition(LogoPosition.logoPostionRightTop);

        try{
            Intent intent = getIntent();
            String rideInfoFile = intent.getStringExtra("rideFileInfo");

            Log.i("Ride info file param", rideInfoFile);

            ArrayList<Coordinate> traceOriginal = getRideTrace(rideInfoFile);
            ArrayList<Coordinate> trace = convertToBaiduCoordinates(traceOriginal);


            Log.i("trace 1", trace.get(0).getLng() + "," + trace.get(0).getLat());
            Log.i("trace 2", trace.get(1).getLng() + "," + trace.get(1).getLat());

            //Log.i("Trace size", String.valueOf(trace.size()));

            // Check if the file is empty
            if (trace == null || trace.size() == 0){
                Snackbar.make(findViewById(R.id.rideRootView), getString(R.string.data_load_failed), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.back), view -> finish()).show();
            }
            else if (trace.size() == 1){
                Snackbar.make(findViewById(R.id.rideRootView), "数据不够绘图", Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.back), view -> finish()).show();
            }
            else {
                // Draw the trace of the ride
                drawTrace(trace);

                // Put the start pin.
                setMarker(trace.get(0),true);

                // Put the end pin
                setMarker(trace.get(trace.size()-1),false);

                // Set map center to distance within the start and end points
                List<LatLng> pts = new ArrayList<>();

                for (Coordinate d: trace
                ) {
                    pts.add(new LatLng(d.getLat(), d.getLng()));
                }

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pts);

                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(builder.build(), 5, 5,
                        5, 5);
                baiduMap.setMapStatus(mapStatusUpdate);

                // Zoom the map
                MapStatusUpdate mapStatusUpdate2 = MapStatusUpdateFactory.zoomBy(5);
                baiduMap.setMapStatus(mapStatusUpdate2);

                // Calculate and display average speed
                TextView avgSpeed = findViewById(R.id.TVAvgSpeed);
                TextView maxSpeed = findViewById(R.id.TVMaxSpeed);

                avgSpeed.setText(Integer.toString(getAverageSpeed(trace)));
                maxSpeed.setText(Integer.toString(getMaxSpeed(trace)));

                // Calculate and display distance of ride
                TextView distance = findViewById(R.id.TVDistance);

                float[] re = new float[1];
                double sl = trace.get(0).getLat();
                double sn = trace.get(0).getLng();
                double el = trace.get(trace.size()-1).getLat();
                double en = trace.get(trace.size()-1).getLng();

                Location.distanceBetween(sl, sn, el, en, re);

                float dst = re[0];

                Log.i("Distance start end", String.valueOf(dst));

                // Calculate traveled distance of each section

                //TODO: 计算的距离不太对
                new Thread(() -> {
                    float dist = 0f;



                    // 计算点和点之间的距离
                    for (int i = 0; i < traceOriginal.size(); i++){
                        double startLat = traceOriginal.get(i).getLat();
                        double startLng = traceOriginal.get(i).getLng();
                        double endLat = traceOriginal.get(i++).getLat();
                        double endLng = traceOriginal.get(i++).getLng();

                        double d = getDistance(startLng, endLng, startLat, endLat);

                        Log.i("Distance between", String.valueOf(d));

                        dist += d;
                        d = 0;
                        Log.i("Distance", String.valueOf(dist));
                        Log.i("value of i", String.valueOf(i));
                    }

                    Log.i("size of trace", String.valueOf(traceOriginal.size()));

                    float finalDist = dist;
                    runOnUiThread(() -> {
                        distance.setText(finalDist + "");

                        TextView lengthUnit = findViewById(R.id.lengthUnitTV);
                        lengthUnit.setText("KM");
                    });



                }).start();
                

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Ends activity when user presses back on app bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    private void drawTrace(ArrayList<Coordinate> r){
        new Thread(() -> {
            List<LatLng> pts = new ArrayList<>();

            // Transform all coordinate objects to latlng objects
            for (Coordinate e: r
            ) {
                pts.add(new LatLng(e.getLat(), e.getLng()));
            }

            // Set line options
            OverlayOptions mOverlayOptions = new PolylineOptions()
                    .width(10)
                    .color(0xAAFF0000)
                    .points(pts);

            // Draw line
            Overlay mPolyline = baiduMap.addOverlay(mOverlayOptions);
        }).run();
    }

    private ArrayList<Coordinate> getRideTrace(String rideFileInfo) throws FileNotFoundException {
        ArrayList<Coordinate> rideTrace = new ArrayList<>();
        File rideFile = new File(getFilesDir() + "/rideInfo", rideFileInfo + ".txt");

        FileInputStream fis = new FileInputStream(rideFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);

        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            //Ignore the first line because the first line is date and time
            reader.readLine();
            String line = reader.readLine();

            if (line == "" || line == null){
                return null;
            }

            int i = 0;

            while (line != null) {

                String lat = line.substring(0, line.indexOf(","));
                String lng = line.substring(line.indexOf(",") + 1, line.indexOf("#"));
                String spd = line.substring(line.indexOf("#") + 1);

                rideTrace.add(new Coordinate(Double.parseDouble(lat), Double.parseDouble(lng), Double.parseDouble(spd)));
                line = reader.readLine();
                i++;
            }

            Log.i("value of i", String.valueOf(i));

        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.rideRootView), getString(R.string.data_load_failed), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.back), view -> finish()).show();
            e.printStackTrace();
        }

        return rideTrace;
    }

    private ArrayList<Coordinate> convertToBaiduCoordinates(ArrayList<Coordinate> r){

        if (r == null){
            return null;
        }

        ArrayList<Coordinate> d = new ArrayList<>();

        for (Coordinate f: r
        ) {
            LatLng sourceLatLng = new LatLng(f.getLat(), f.getLng());
            CoordinateConverter converter  = new CoordinateConverter();
            converter.from(CoordinateConverter.CoordType.GPS);
            converter.coord(sourceLatLng);
            LatLng desLatLng = converter.convert();

            d.add(new Coordinate(desLatLng.latitude, desLatLng.longitude, f.getSpeed()));
        }

        return d;
    }

    private void setMarker(Coordinate c, Boolean isStart){
        new Thread(() -> {
            LatLng mark = new LatLng(c.getLat(), c.getLng());
            BitmapDescriptor bitmap;

            if(isStart){
                bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_start);
            }
            else{
                bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_end);
            }

            OverlayOptions option = new MarkerOptions()
                    .position(mark)
                    .icon(bitmap);

            baiduMap.addOverlay(option);
        }).start();
    }

    private int getAverageSpeed(ArrayList<Coordinate> c){
        double avgSpeed = 0;

        for (Coordinate r: c
        ) {
            avgSpeed += r.getSpeed();
        }

        avgSpeed /= c.size();

        // Note: the speeds recorded in the files are m/s, we need to convert into km/h
        return Math.toIntExact(Math.round((avgSpeed * 18) / 5));
    }

    private int getMaxSpeed(ArrayList<Coordinate> c){
        ArrayList<Double> speed = new ArrayList<>();

        for (Coordinate r: c
        ) {
            speed.add(r.getSpeed());
        }

        // Note: the speeds recorded in the files are m/s, we need to convert into km/h

        int maxSpeed = Math.toIntExact(Math.round(Collections.max(speed)));

        maxSpeed = Math.round((maxSpeed * 18) / 5);

        return maxSpeed;
    }

    public double getDistance(double ln1, double ln2, double lt1, double lt2){
        double radLat1 = Math.toRadians(lt1);
        double radLat2 = Math.toRadians(lt2);

        double a = radLat1 - radLat2;
        double b = Math.toRadians(ln1) - Math.toRadians(ln2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * 6378137; // 数字是地球半径
        //s = Math.round(s * 100) / 100.0;

        return (1.38 * s) / 1000;
    }
}
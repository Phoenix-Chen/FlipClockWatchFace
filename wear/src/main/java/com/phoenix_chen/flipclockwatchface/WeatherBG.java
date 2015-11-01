package com.phoenix_chen.flipclockwatchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by Phoenix on 10/21/15.
 * Not working at this point.
 */
public class WeatherBG implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleApiClient googleClient;
    private Bitmap BG;
    Resources resources;
    int displayHeight,displayWidth;


    private double [] CurLoc= new double[2];

    public WeatherBG(Context context, Resources resources){
        this.resources=resources;
        displayHeight=resources.getDisplayMetrics().heightPixels;
        displayWidth=resources.getDisplayMetrics().widthPixels;

        // Build a new GoogleApiClient
        googleClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public double[] getCurLoc(){
        return CurLoc;
    }

    public Bitmap getBG() {
        return BG;
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        // Create the LocationRequest object
        LocationRequest locationRequest = LocationRequest.create();
        // Use high accuracy
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 2 seconds
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(2));
        // Set the fastest update interval to 2 seconds
        locationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(2));
        // Set the minimum displacement
        locationRequest.setSmallestDisplacement(2);

        // Register listener using the LocationRequest object
        LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, locationRequest, this);
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    @Override
    public void onLocationChanged(Location location){

        // Display the latitude and longitude in the UI
        //mTextView.setText("Latitude:  " + String.valueOf( location.getLatitude()) +
        //        "\nLongitude:  " + String.valueOf( location.getLongitude()));
        CurLoc[0]=location.getLatitude();
        CurLoc[1]=location.getLongitude();

        switch (parsejson(getWeather(CurLoc[0],CurLoc[1]))){
            case "Rain":
                BG=Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.rain), displayWidth, displayHeight, true);
                break;
            case "Clouds":
                BG=Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.clouds), displayWidth, displayHeight, true);
                break;
            case "Clear":
                BG=Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.clear), displayWidth, displayHeight, true);
                break;
            default:
                BG=Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.snow), displayWidth, displayHeight, true);
                break;
        }
    }

    private static String parsejson(String buffstring) {
        try {
            JSONObject obj = new JSONObject(buffstring);
            JSONArray arrdata = obj.getJSONArray("weather");
            String main = arrdata.getJSONObject(0).getString("main");
            return main;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private static String getWeather(double lat,double lon) {
        String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?";
        HttpURLConnection con = null;
        InputStream instream = null;
        try {
            con = (HttpURLConnection) (new URL(BASE_URL + "lat="+lat+"&lon="+lon + "&appid=d78607aadd690fc2a644b9e10f887054"))
                    .openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            // Let's read the response
            StringBuffer buffer = new StringBuffer();
            instream = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    instream));
            String line = null;
            while ((line = br.readLine()) != null)
                buffer.append(line + "\r\n");

            instream.close();
            con.disconnect();

            System.out.println(buffer.toString());
            return buffer.toString();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                instream.close();
            } catch (Throwable t) {
            }
            try {
                con.disconnect();
            } catch (Throwable t) {
            }
        }

        return null;
    }

}

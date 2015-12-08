package com.phoenix_chen.flipclockwatchface;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

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
 * Created by Phoenix on 10/27/15.
 * Memo: Lots of stuff need to handle
 * see: http://stackoverflow.com/questions/29712244/using-googleapiclient-in-a-service
 */
public class BGservice extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "BGservice";
    GoogleApiClient googleClient;
    private double [] CurLoc;
    int BGImageNum;

    @Override
    public void onCreate()
    {
        super.onCreate();
        googleClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleClient.connect();

        CurLoc= new double[2];
        BGImageNum=0;
        Log.i(TAG, "onCreate...");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Log.i(TAG, CurLoc[0] + " & " + CurLoc[1] + " & " + BGImageNum);
        String WEARABLE_DATA_PATH = "/wearable_data";

        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        //dataMap.putDouble("lat", CurLoc[0]);
        //dataMap.putDouble("lon", CurLoc[1]);
        dataMap.putInt("BGNum",BGImageNum);
        new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();

        Log.i(TAG, "Imagenum:" + BGImageNum);

        return START_STICKY;
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send DataMap");
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "onConnected...");

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
        Log.i(TAG, "onLocationChanged...");

        // Display the latitude and longitude in the UI
        //mTextView.setText("Latitude:  " + String.valueOf( location.getLatitude()) +
        //        "\nLongitude:  " + String.valueOf( location.getLongitude()));
        CurLoc[0]=location.getLatitude();
        CurLoc[1]=location.getLongitude();

        Log.i(TAG, "lat" + CurLoc[0] + " lon" + CurLoc[1]);


        openWeatherMapThread mapThread=new openWeatherMapThread();
        mapThread.start();

    }

    private class openWeatherMapThread extends Thread{
        @Override
        public void run(){
            switch (parsejson(getWeather(CurLoc[0],CurLoc[1]))){
                case "Rain":
                    BGImageNum=1;
                    break;
                case "Clouds":
                    BGImageNum=2;
                    break;
                case "Clear":
                    BGImageNum=3;
                    break;
                case "Mist":
                    BGImageNum=4;
                    break;
                case "Haze":
                    BGImageNum=5;
                    break;
                case "Snow":
                    BGImageNum=6;
                    break;
                default:
                    BGImageNum=0;
                    break;
            }
        }

        private String parsejson(String buffstring) {
            try {
                Log.i(TAG,buffstring);
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

        private String getWeather(double lat,double lon) {
            String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?";
            HttpURLConnection con = null;
            InputStream instream = null;
            try {
                Log.i(TAG,"step 1");
                con = (HttpURLConnection) (new URL(BASE_URL + "lat="+lat+"&lon="+lon + "&appid=d78607aadd690fc2a644b9e10f887054"))
                        .openConnection();

//            con.setReadTimeout(10000 /* milliseconds */);
//            con.setConnectTimeout(15000 /* milliseconds */);

                Log.i(TAG,"step 2");
                con.setRequestMethod("GET");
                Log.i(TAG, "step 3");
                con.setDoInput(true);
                Log.i(TAG, "step 4");
                con.setDoOutput(true);
                Log.i(TAG, "step 5");
                con.connect();

                // Let's read the response
                Log.i(TAG,"step 6");
                StringBuffer buffer = new StringBuffer();
                Log.i(TAG,"step 7");
                instream = con.getInputStream();
                Log.i(TAG,"step 8");
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        instream));
                Log.i(TAG,"step 9");
                String line = null;
                Log.i(TAG,"step 10");
                while ((line = br.readLine()) != null)
                    buffer.append(line + "\r\n");

                Log.i(TAG, "step 11");
                instream.close();
                Log.i(TAG, "step 12");
                con.disconnect();

                Log.i(TAG, "step 13");
                //System.out.println(buffer.toString());
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

            Log.i(TAG,"step 14");
            return null;
        }
    }


}
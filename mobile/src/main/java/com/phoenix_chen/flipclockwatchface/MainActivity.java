package com.phoenix_chen.flipclockwatchface;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    RadioGroup groupWeather;
    boolean showAlertDialog = true; // Use to prevent checking useimages trigger onCheckedChanged
    private static final String TAG = "Bebugging At Main";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set OnCheckedChangeListener for RadioGroup groupWeather
        groupWeather=(RadioGroup) findViewById(R.id.radio_group_weather);
        groupWeather.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (showAlertDialog) {
                    if (checkedId == R.id.radio_useweather) {
                        // Check if GPS is enable
                        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            buildAlertMessageNoGps();
                        }
                        Toast.makeText(getApplicationContext(), "now weather checked", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        Intent mBroadcastReceiver = new Intent(this, AlarmReceiver.class);
        PendingIntent recurringAlarm = PendingIntent.getBroadcast(this,0,mBroadcastReceiver, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar updateTime = Calendar.getInstance();
        alarms.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), TimeUnit.SECONDS.toMillis(1), recurringAlarm);

//        startService(BGIntent);

    }

    // Build an alert message if GPS is disable
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                        // Prevent checking useimages trigger onCheckedChanged
                        showAlertDialog = false;
                        groupWeather.check(R.id.radio_useimages);

                        dialog.cancel();
                        showAlertDialog = true;
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onResume(){
        super.onResume();

        // Check if GPS is disable when resume the activity
        if(groupWeather.getCheckedRadioButtonId()==R.id.radio_useweather){
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                buildAlertMessageNoGps();
            }
        }
    }
}

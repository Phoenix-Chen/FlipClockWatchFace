package com.phoenix_chen.flipclockwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Phoenix on 10/29/15.
 */
public class AlarmReceiver extends BroadcastReceiver
{

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(TAG, "Alarm sdbsdbjmfsjd");
        Intent myService = new Intent(context, BGservice.class);
        context.startService(myService);
    }
}
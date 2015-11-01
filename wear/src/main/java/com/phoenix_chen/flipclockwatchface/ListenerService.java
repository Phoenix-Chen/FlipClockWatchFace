package com.phoenix_chen.flipclockwatchface;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Phoenix on 10/30/15.
 */
public class ListenerService extends WearableListenerService {

    private static final String WEARABLE_DATA_PATH = "/wearable_data";
    final static String MY_ACTION = "Service2WatchFace";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i("myTag", "onDataChanged...");

        DataMap dataMap;
        for (DataEvent event : dataEvents) {

            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WEARABLE_DATA_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.i("myTag", "DataMap received on watch: " + dataMap);


                    MyThread myThread = new MyThread(dataMap);
                    myThread.start();
                }


            }
        }
    }

    public class MyThread extends Thread {
        DataMap dataMap;
        public MyThread(DataMap datamap){
            dataMap=datamap;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub


            //Thread.sleep(5000);
            Intent intent = new Intent();
            intent.setAction(MY_ACTION);
            intent.putExtra("DATAPASSED", dataMap.getInt("BGNum"));

            sendBroadcast(intent);
            Log.i("myTag","already sending...");


            stopSelf();
        }
    }
}
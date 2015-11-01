/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phoenix_chen.flipclockwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1)/20;
    //private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMicros(1000);


    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    MyReceiver myReceiver;
    int datapassed=0;

    @Override
    public Engine onCreateEngine() {
        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ListenerService.MY_ACTION);
        registerReceiver(myReceiver,intentFilter);

        return new Engine(); }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextPaint;

        boolean mAmbient;

        Time mTime;

        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        FlipNumbers digits[]=new FlipNumbers[6];
        Bitmap weekdays[]=new Bitmap[7];
        Bitmap background;
        Bitmap BGs[];
        int[][] nposition;
        int[][] dposition;
        int[] curTime=new int[6];
        int[] lastTime=new int[6];
        boolean firstEnter=true;
        int displayHeight;
        int displayWidth;
        int numHeight;
        int numWidth;


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

//            Intent mListener= new Intent(getApplication(),ListenerService.class);
//            startService(mListener);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mTime = new Time();

            // Initialized flip number's size
            displayHeight=resources.getDisplayMetrics().heightPixels;
            displayWidth=resources.getDisplayMetrics().widthPixels;
            numHeight=80;
            numWidth=displayWidth/7;

            // Prepare Number images for the time
            digits[0]=new FlipNumbers(2, numWidth, numHeight, resources);
            digits[1]=new FlipNumbers(9, numWidth, numHeight, resources);
            digits[2]=new FlipNumbers(5, numWidth, numHeight, resources);
            digits[3]=new FlipNumbers(9, numWidth, numHeight, resources);
            digits[4]=new FlipNumbers(5, numWidth/2, numHeight/2, resources);
            digits[5]=new FlipNumbers(9, numWidth/2, numHeight/2, resources);

            // Initialize x-y position for numbers
            nposition=new int[6][2];

            nposition[0][0]=displayWidth/2-numWidth*2-5;
            nposition[0][1]=displayHeight/2-40;
            nposition[1][0]=displayWidth/2-numWidth-3;
            nposition[1][1]=displayHeight/2-40;
            nposition[2][0]=displayWidth/2+3;
            nposition[2][1]=displayHeight/2-40;
            nposition[3][0]=displayWidth/2+numWidth+5;
            nposition[3][1]=displayHeight/2-40;
            nposition[4][0]=displayWidth/2+numWidth*2+6;
            nposition[4][1]=displayHeight/2;
            nposition[5][0]=displayWidth/2+numWidth*2+numWidth/2+7;
            nposition[5][1]=displayHeight/2;

            // Initialize weekdays
            weekdays[0]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.weekday7), displayWidth / 8, 20, true);
            weekdays[1]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday1), displayWidth / 8, 20, true);
            weekdays[2]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday2), displayWidth / 8, 20, true);
            weekdays[3]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday3), displayWidth / 8, 20, true);
            weekdays[4]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday4), displayWidth / 8, 20, true);
            weekdays[5]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday5), displayWidth / 8, 20, true);
            weekdays[6]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.weekday6), displayWidth / 8, 20, true);

            dposition= new int[1][2];

            dposition[0][0]=displayWidth/2;
            dposition[0][1]=displayHeight/2+42;

//            WeatherBG weatherBG=new WeatherBG(getApplicationContext(),resources);
//
//            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)){
//                background=weatherBG.getBG();
//            }else{
//                background = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,R.drawable.woodbackground), displayWidth, displayHeight, true);
//            }

            BGs=new Bitmap[4];
            BGs[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.woodbackground),displayWidth, displayHeight, true);
            BGs[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.rain),displayWidth, displayHeight, true);
            BGs[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.clouds),displayWidth, displayHeight, true);
            BGs[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.clear),displayWidth, displayHeight, true);

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.

            //canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            canvas.drawBitmap(BGs[datapassed],0,0,mBackgroundPaint);


            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
//            String text = mAmbient
//                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
//                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
//            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);


            //add images
            Paint p=new Paint();
            p.setColor(Color.BLACK);
            curTime[0]=getTenDigit(mTime.hour);
            curTime[1]=mTime.hour%10;
            curTime[2]=getTenDigit(mTime.minute);
            curTime[3]=mTime.minute%10;
            curTime[4]=getTenDigit(mTime.second);
            curTime[5]=mTime.second%10;

            if(mAmbient){ // Ambient Mode
                firstEnter=true;
                for(int c=0;c<4;c++){
                    canvas.drawBitmap(digits[c].getBitMapAt(curTime[c]*9),nposition[c][0],nposition[c][1],p);
                }

            }else{ // Normal mode
                if (firstEnter){

                    for(int i=0;i<curTime.length;i++){
                        canvas.drawBitmap(digits[i].getBitMapAt(curTime[i]*9),nposition[i][0],nposition[i][1],p);
                        digits[i].setCurIndex(curTime[i]*9);
                    }
                    firstEnter=false;

                }else{

                    for(int n=0;n<curTime.length;n++){

                        // Set the index back to 0 if it hits the top
                        if(digits[n].getCurIndex()>=digits[n].getBitMap().length){
                            digits[n].setCurIndex(0);
                        }

                        if(isNext(lastTime[n],curTime[n],digits[n]) || digits[n].getCurIndex()%9!=0){
                            canvas.drawBitmap(digits[n].getBitMapAt(digits[n].getCurIndex()),nposition[n][0],nposition[n][1],p);
                            digits[n].setCurIndex(digits[n].getCurIndex() + 1);
                        }else{
                            digits[n].setCurIndex(curTime[n]*9);
                            canvas.drawBitmap(digits[n].getBitMapAt(digits[n].getCurIndex()),nposition[n][0],nposition[n][1],p);
                        }
                    }
                }

                int curMonth=mTime.month;
                int curMonthDay=mTime.monthDay;
                int curWeekDay=mTime.weekDay;
                canvas.drawBitmap(weekdays[curWeekDay], dposition[0][0], dposition[0][1], p);
                //canvas.drawText(getlocation(), mXOffset, mYOffset + 100, mTextPaint);
            }



            // update the lastTime with the curTime
            for(int k=0;k<curTime.length;k++){
                lastTime[k]=curTime[k];
            }
        }

//        private String getlocation(){
//            //double[] loc=new double[2];
//
//            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                double longitude = location.getLongitude();
//                double latitude = location.getLatitude();
//
//                return latitude+" "+longitude;
//            }
//            return "";
//        }

        private boolean isNext(int last, int cur, FlipNumbers curDigit){
            if(last+1==cur || (last==(curDigit.getBitMap().length)/9-1 && cur == 0))
                return true;
            return false;
        }

        // Get the tens digit
        private int getTenDigit(int num){
            return (num-num%10)/10;
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }


    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub

            datapassed = arg1.getIntExtra("DATAPASSED",0);
            Log.i("myTag", "get the message in main:"+datapassed);

        }
    }
}

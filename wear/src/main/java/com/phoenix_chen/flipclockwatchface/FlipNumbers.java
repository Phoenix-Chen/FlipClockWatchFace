package com.phoenix_chen.flipclockwatchface;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Phoenix on 10/11/15.
 */
public class FlipNumbers {

    private Bitmap number[];
    private int CurIndex;


    public FlipNumbers(int digit, int width, int height, Resources resources){

        // Initialize number array
        number=new Bitmap[(digit+1)*9];

        // Set current Index at 0
        //CurIndex=0;//have to change late

        int c=0;
        if(digit<9){
            for(int o=0;o<digit;o++){
                for(int i=1;i<=9;i++){
                    String name="num"+o+"_"+i;
                    int id=resources.getIdentifier(name,"drawable","com.phoenix_chen.flipclockwatchface");
                    number[c]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, id), width, height,true);
                    c++;
                }
            }
            for(int i=1;i<=9;i++){
                String name="num"+digit+"_"+i+"s";
                int id=resources.getIdentifier(name,"drawable","com.phoenix_chen.flipclockwatchface");
                number[c]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, id), width, height,true);
                c++;
            }

        }else{
            for(int o=0;o<=digit;o++){
                for(int i=1;i<=9;i++){
                    String name="num"+o+"_"+i;
                    int id=resources.getIdentifier(name,"drawable","com.phoenix_chen.flipclockwatchface");
                    number[c]= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, id), width, height,true);
                    c++;
                }
            }
        }

    }

    public Bitmap[] getBitMap(){
        return number;
    }

    public Bitmap getBitMapAt(int index){
        return number[index];
    }

    public int getCurIndex(){
        return CurIndex;
    }

    public void setCurIndex(int index){
        CurIndex=index;
    }

}

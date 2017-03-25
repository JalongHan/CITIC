package com.gidea.citic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WaterWaveView mWaterWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWaterWaveView = new WaterWaveView(this);
        setContentView(mWaterWaveView);


    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
//        mWaterWaveView.resume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
//        mWaterWaveView.pause();
    }


}

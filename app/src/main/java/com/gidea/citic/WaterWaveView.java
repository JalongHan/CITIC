package com.gidea.citic;

import android.annotation.TargetApi;
import android.content.Context;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * ━━━━━━神兽出没━━━━━━
 * 　　　┏┓　　　┏┓
 * 　　┏┛┻━━━┛┻┓
 * 　　┃　　　　　　　┃
 * 　　┃　　　━　　　┃
 * 　　┃　┳┛　┗┳　┃
 * 　　┃　　　　　　　┃
 * 　　┃　　　┻　　　┃
 * 　　┃　　　　　　　┃
 * 　　┗━┓　　　┏━┛Code is far away from bug with the animal protecting
 * 　　　　┃　　　┃    神兽保佑,代码无bug
 * 　　　　┃　　　┃
 * 　　　　┃　　　┗━━━┓
 * 　　　　┃　　　　　　　┣┓
 * 　　　　┃　　　　　　　┏┛
 * 　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　┃┫┫　┃┫┫
 * 　　　　　┗┻┛　┗┻┛
 * <p>
 * 作者:jalong Han
 * 邮箱:hjl999@126.com
 * 时间:17/3/16
 * 功能:
 */

class WaterWaveView extends RSSurfaceView {
    private static final String TAG = "WaterWaveView";
    private WaterWaveRS mRender;
    private RenderScriptGL mRS;
    private int mWidth;
    private int mHeight;

    @TargetApi(11)
    public WaterWaveView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);

    }

    @TargetApi(11)
    private void ensureRenderScript(int width ,int height){
        Log.d(TAG,"__Width = "+width+",Height = "+height);
        if(mRender == null){
            mRender = new WaterWaveRS(width, height);
            mRender.init(mRS, getResources());
        }else{
//    		mRender.resize(width, height);
        }
        mRender.start();
    }

    @TargetApi(11)
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        super.surfaceCreated(holder);
        if(mRS == null){
            mRS = createRenderScriptGL(new RenderScriptGL.SurfaceConfig());
        }
        setRenderScriptGL(mRS);
    }

    @TargetApi(11)
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        ensureRenderScript(w, h);
    }

    @TargetApi(11)
    @Override
    protected void onDetachedFromWindow() {
        // Handle the system event and clean up
        mRender = null;
        if (mRS != null) {
            mRS = null;
            destroyRenderScriptGL();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /*switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            //case MotionEvent.ACTION_MOVE:
                mRender.addDrop(event.getX(), event.getY());
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    // Ignore
                }
                break;
        }*/
        Log.d(TAG,"_____MotionEvent action = "+event.getAction());
        if(event.getAction() == MotionEvent.ACTION_MOVE){

            mRender.addDrop(event.getX(),event.getY());
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                // Ignore
            }
//        	return true;
        }
        return true;
    }
}
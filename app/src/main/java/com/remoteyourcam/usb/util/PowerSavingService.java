package com.remoteyourcam.usb.util;

import android.app.Activity;
import android.os.Handler;
import android.view.WindowManager;

public class PowerSavingService {

    public final int DELAY_TURN_SCREEN_DOWN = 2 * 60 * 1000;
    public final int DELAY_TURN_SCREEN_DOWN_REQUEST_ACTION = 60 * 1000;
    public final int DELAY_CAMERA_OFF = 2 * 60 * 1000;


    public final float SCREEN_BRIGHNESS_DEFAULT = 0.8f;
    public final float SCREEN_BRIGHNESS_MEDIUM = 0.3f;
    public final float SCREEN_BRIGHNESS_LOW = 0f;
    public final float SCREEN_BRIGHNESS_MAX = 1f;

    private Handler handler;
    private Runnable runnableTurnScreenDown = new Runnable() {
        @Override
        public void run() {
            turnBrightnessDown(SCREEN_BRIGHNESS_LOW);
        }
    };

    private Runnable runnableTurnScreenDownRequestAction = new Runnable() {
        @Override
        public void run() {
            turnBrightnessDown(SCREEN_BRIGHNESS_MEDIUM);
        }
    };

    private Runnable runnableCameraOff = new Runnable() {
        @Override
        public void run() {
            if (powerSavingListener != null) {
                powerSavingListener.onPowerSavingTurnCameraOff();
            }
        }
    };

    private Activity activity;
    private PowerSavingListener powerSavingListener;

    public PowerSavingService(Activity activity, PowerSavingListener powerSavingListener) {
        this.activity = activity;
        this.powerSavingListener = powerSavingListener;

        handler = new Handler();

    }


    public void adjustBrightness(float brightness) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = brightness;
        activity.getWindow().setAttributes(params);
    }

    public void turnBrightnessDown(float brightness) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (params.screenBrightness > brightness) {
            params.screenBrightness = brightness;
            activity.getWindow().setAttributes(params);
        }
    }

    public void turnBrightnessUp(float brightness) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (params.screenBrightness < brightness) {
            params.screenBrightness = brightness;
            activity.getWindow().setAttributes(params);
        }
    }

    private void restartPowerSavingCountdowns() {
        this.handler.removeCallbacks(runnableTurnScreenDown);
        this.handler.postDelayed(runnableTurnScreenDown, DELAY_TURN_SCREEN_DOWN);

        this.handler.removeCallbacks(runnableTurnScreenDownRequestAction);
        this.handler.postDelayed(runnableTurnScreenDownRequestAction, DELAY_TURN_SCREEN_DOWN_REQUEST_ACTION);

        this.handler.removeCallbacks(runnableCameraOff);
        this.handler.postDelayed(runnableCameraOff, DELAY_CAMERA_OFF);
    }

    public void newUserInteraction() {
        this.restartPowerSavingCountdowns();
        turnBrightnessUp(SCREEN_BRIGHNESS_DEFAULT);
    }
}

package com.remoteyourcam.usb;

import android.app.Application;

import com.remoteyourcam.usb.ptp.Camera;

public class App extends Application {

    private Camera camera;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }
}

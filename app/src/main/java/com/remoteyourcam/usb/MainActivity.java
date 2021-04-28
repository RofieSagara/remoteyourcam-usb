/**
 * Copyright 2013 Nils Assbeck, Guersel Ayaz and Michael Zoech
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.remoteyourcam.usb;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.mtp.MtpStorageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.remoteyourcam.usb.ptp.Camera;
import com.remoteyourcam.usb.ptp.Camera.CameraListener;
import com.remoteyourcam.usb.ptp.EosCamera;
import com.remoteyourcam.usb.ptp.PtpService;
import com.remoteyourcam.usb.ptp.PtpUsbConnection;
import com.remoteyourcam.usb.ptp.model.LiveViewData;
import com.remoteyourcam.usb.view.SessionActivity;
import com.remoteyourcam.usb.view.SessionView;
import com.remoteyourcam.usb.view.TabletSessionFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends SessionActivity implements CameraListener {

    private static final int DIALOG_PROGRESS = 1;
    private static final int DIALOG_NO_CAMERA = 2;

    private static final String TAG = "Chris remote control";
    private final Handler handler = new Handler();

    private PtpService ptp;
    private Camera camera;

    private boolean isInStart;
    private boolean isInResume;
    private SessionView sessionFrag;
    private boolean isLarge;
    private AppSettings settings;
    private Toolbar toolbar;

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public void setSessionView(SessionView view) {
        sessionFrag = view;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        settings = new AppSettings(this);

        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ptp = PtpService.Singleton.getInstance(this);

        Fragment f = new TabletSessionFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, f)
                .commit();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (AppConfig.LOG) {
            Log.i(TAG, "onNewIntent " + intent.getAction());
        }
        this.setIntent(intent);
        if (isInStart) {
            ptp.initialize(this, intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reload) {
            isInStart = true;
            ptp.setCameraListener(this);
            ptp.initialize(this, getIntent());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (AppConfig.LOG) {
            Log.i(TAG, "onStart");
        }
        isInStart = true;
        ptp.setCameraListener(this);
        ptp.initialize(this, getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInResume = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInResume = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*if (AppConfig.LOG) {
            Log.i(TAG, "onStop (MainActivity on Stop)");
        }
        isInStart = false;
        ptp.setCameraListener(null);
        if (isFinishing()) {
            ptp.shutdown();
        }
         */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (AppConfig.LOG) {
            Log.i(TAG, "onDestroy");
        }
        isInStart = false;
        ptp.setCameraListener(null);
        if (isFinishing()) {
            ptp.shutdown();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PROGRESS:
                return ProgressDialog.show(this, "", "Generating information. Please wait...", true);
            case DIALOG_NO_CAMERA:
                AlertDialog.Builder b = new AlertDialog.Builder(this);
                b.setTitle(R.string.dialog_no_camera_title);
                b.setMessage(R.string.dialog_no_camera_message);
                b.setNeutralButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                return b.create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onCameraStarted(final Camera camera) {
        Toast.makeText(this, "onCameraStarted", Toast.LENGTH_SHORT).show();
        ((App) getApplication()).setCamera(camera);
        this.camera = camera;
        if (AppConfig.LOG) {
            Log.i(TAG, "camera started");
        }
        try {
            dismissDialog(DIALOG_NO_CAMERA);
        } catch (IllegalArgumentException ignored) {
        }
        if (camera instanceof EosCamera) {
            // final EosCamera eosCamera = (EosCamera) camera;
            // final PtpUsbConnection connection = eosCamera.getConnection();
            Button btn = findViewById(R.id.load_sd_btn);
            btn.setVisibility(View.VISIBLE);
            btn.setOnClickListener(view -> {
//                    MtpDevice device = new MtpDevice(eosCamera.device);
//                    device.open(connection.connection);
//                    ArrayList<String> filenames = new ArrayList<>();
//                    List<MtpObjectInfo> infos = getObjectList(device, 131073, -1879048192);
//
//                    int[] storageIds = device.getStorageIds();
//                    if (storageIds == null) {
//                        return;
//                    }
//                    ArrayList<MtpObjectInfo> filesPath = new ArrayList<>();
//                    for (int storageId : storageIds) {
//                        filesPath.addAll(scanObjectsInStorage(device, storageId, 0, 0));
//                    }
//                    for (MtpObjectInfo info : filesPath) {
//                        filenames.add(info.getName());
//                    }
                Intent i = new Intent(MainActivity.this, FilenameActivity.class);
                //i.putExtra("filenames", filenames);
                startActivity(i);
            });
        }
        getSupportActionBar().setTitle(camera.getDeviceName());
        camera.setCapturedPictureSampleSize(settings.getCapturedPictureSampleSize());
        sessionFrag.cameraStarted(camera);
    }

    @Override
    public void onCameraStopped(Camera camera) {
        if (AppConfig.LOG) {
            Log.i(TAG, "camera stopped");
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.camera = null;
        sessionFrag.cameraStopped(camera);
    }

    @Override
    public void onNoCameraFound() {
        //showDialog(DIALOG_NO_CAMERA);
        Toast.makeText(this, R.string.dialog_no_camera_title, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
        sessionFrag.enableUi(false);
        sessionFrag.cameraStopped(null);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPropertyChanged(int property, int value) {
        sessionFrag.propertyChanged(property, value);
    }

    @Override
    public void onPropertyStateChanged(int property, boolean enabled) {
        // TODO
    }

    @Override
    public void onPropertyDescChanged(int property, int[] values) {
        sessionFrag.propertyDescChanged(property, values);
    }

    @Override
    public void onLiveViewStarted() {
        sessionFrag.liveViewStarted();
    }

    @Override
    public void onLiveViewStopped() {
        sessionFrag.liveViewStopped();
    }

    @Override
    public void onLiveViewData(LiveViewData data) {
        if (!isInResume) {
            return;
        }
        sessionFrag.liveViewData(data);
    }

    @Override
    public void onCapturedPictureReceived(int objectHandle, String filename, Bitmap thumbnail, Bitmap bitmap) {
        if (thumbnail != null) {
            sessionFrag.capturedPictureReceived(objectHandle, filename, thumbnail, bitmap);
        } else {
            Toast.makeText(this, "No thumbnail available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBulbStarted() {
        sessionFrag.setCaptureBtnText("0");
    }

    @Override
    public void onBulbExposureTime(int seconds) {
        sessionFrag.setCaptureBtnText("" + seconds);
    }

    @Override
    public void onBulbStopped() {
        sessionFrag.setCaptureBtnText("Fire");
    }

    @Override
    public void onFocusStarted() {
        sessionFrag.focusStarted();
    }

    @Override
    public void onFocusEnded(boolean hasFocused) {
        sessionFrag.focusEnded(hasFocused);
    }

    @Override
    public void onFocusPointsChanged() {
        // TODO onFocusPointsToggleClicked(null);
    }

    @Override
    public void onObjectAdded(int handle, int format) {
        sessionFrag.objectAdded(handle, format);
    }

    private List<MtpObjectInfo> scanObjectsInStorage(MtpDevice mtpDevice, int storageId, int format, int parent) {
        ArrayList<MtpObjectInfo> filesObjects = new ArrayList<>();
        int[] objectHandles = mtpDevice.getObjectHandles(storageId, format, parent);
        if (objectHandles == null) {
            return new ArrayList<>();
        }

        for (int objectHandle : objectHandles) {
            // Limit only 50 to make this apps not slow as fuck
            if (filesObjects.size() > 10) {
                return filesObjects;
            }

            /*
             *　It's an abnormal case that you can't acquire MtpObjectInfo from MTP device
             */
            MtpObjectInfo mtpObjectInfo = mtpDevice.getObjectInfo(objectHandle);
            if (mtpObjectInfo == null) {
                continue;
            }

            /*
             * Skip the object if parent doesn't match
             */
            int parentOfObject = mtpObjectInfo.getParent();
            if (parentOfObject != parent) {
                continue;
            }

            int associationType = mtpObjectInfo.getAssociationType();

            if (associationType == MtpConstants.ASSOCIATION_TYPE_GENERIC_FOLDER) {
                /* Scan the child folder */
                filesObjects.addAll(scanObjectsInStorage(mtpDevice, storageId, format, objectHandle));
            } else if (mtpObjectInfo.getFormat() == MtpConstants.FORMAT_EXIF_JPEG &&
                    mtpObjectInfo.getProtectionStatus() != MtpConstants.PROTECTION_STATUS_NON_TRANSFERABLE_DATA) {
                filesObjects.add(mtpObjectInfo);
                /*
                 *  get bitmap data from the object
                 */
/*                byte[] rawObject = mtpDevice.getObject(objectHandle, mtpObjectInfo.getCompressedSize());
                Bitmap bitmap = null;
                if (rawObject != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    int scaleW = (mtpObjectInfo.getImagePixWidth() - 1) / MAX_IMAGE_WIDTH + 1;
                    int scaleH = (mtpObjectInfo.getImagePixHeight() - 1) / MAX_IMAGE_HEIGHT  + 1;
                    int scale = Math.max(scaleW, scaleH);
                    if (scale > 0) {
                        options.inSampleSize = scale;
                        bitmap = BitmapFactory.decodeByteArray(rawObject, 0, rawObject.length, options);
                    }
                }
                if (bitmap != null) {
                    publishProgress(bitmap);
                }*/
            }
        }
        return filesObjects;
    }

    public List<MtpStorageInfo> getStorageList(MtpDevice device) {
        if (device == null) {
            return null;
        }
        int[] storageIds = device.getStorageIds();
        if (storageIds == null) {
            return null;
        }

        int length = storageIds.length;
        ArrayList<MtpStorageInfo> storageList = new ArrayList<MtpStorageInfo>(length);
        for (int i = 0; i < length; i++) {
            MtpStorageInfo info = device.getStorageInfo(storageIds[i]);
            if (info == null) {
                Log.w(TAG, "getStorageInfo failed");
            } else {
                storageList.add(info);
            }
        }
        return storageList;
    }

    public List<MtpObjectInfo> getObjectList(MtpDevice device, int storageId, int objectHandle) {
        if (device == null) {
            return null;
        }
        if (objectHandle == 0) {
            // all objects in root of storage
            objectHandle = 0xFFFFFFFF;
        }
        int[] handles = device.getObjectHandles(storageId, 0, objectHandle);
        if (handles == null) {
            return null;
        }

        int length = handles.length;
        ArrayList<MtpObjectInfo> objectList = new ArrayList<MtpObjectInfo>(length);
        for (int i = 0; i < length; i++) {
            MtpObjectInfo info = device.getObjectInfo(handles[i]);
            if (info == null) {
                Log.w(TAG, "getObjectInfo failed");
            } else {
                objectList.add(info);
            }
        }
        return objectList;
    }
}

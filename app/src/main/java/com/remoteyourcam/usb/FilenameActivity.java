package com.remoteyourcam.usb;

import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.remoteyourcam.usb.ptp.EosCamera;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FilenameActivity  extends AppCompatActivity {

    private Future<Boolean> scope = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filenames);

        TextView tv = findViewById(R.id.filenames);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Loading Please wait...\n");
        tv.setText(stringBuilder);
        if (getApp().getCamera() == null) {
            Toast.makeText(this, "Camera is Null", Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        EosCamera camera = (EosCamera) getApp().getCamera();
        MtpDevice device = new MtpDevice(camera.device);
        device.open(camera.getConnection().connection);

        ExecutorService executor = Executors.newCachedThreadPool();
        Handler handler = new Handler(Looper.getMainLooper());

        scope = executor.submit(() -> {
            int[] storageIds = device.getStorageIds();
            if (storageIds == null) {
                return false;
            }

            for (int storageId : storageIds) {
                scanObjectsInStorage(device, storageId, 0, 0, data -> {
                    handler.post(() -> {
                        String currentText = tv.getText().toString();
                        StringBuilder builder = new StringBuilder(currentText);
                        builder.append(data.getName());
                        builder.append("\n");
                        tv.setText(builder);
                    });
                });
            }
            return true;
        });

        executor.shutdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scope != null) {
            scope.cancel(true);
        }
    }

    private App getApp() {
        return (App) getApplication();
    }

    private void scanObjectsInStorage(MtpDevice mtpDevice, int storageId, int format, int parent, Callback callback) {
        int[] objectHandles = mtpDevice.getObjectHandles(storageId, format, parent);
        if (objectHandles == null) {
            return;
        }

        for (int objectHandle : objectHandles) {
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
                scanObjectsInStorage(mtpDevice, storageId, format, objectHandle, callback);
            } else if (mtpObjectInfo.getFormat() == MtpConstants.FORMAT_EXIF_JPEG &&
                    mtpObjectInfo.getProtectionStatus() != MtpConstants.PROTECTION_STATUS_NON_TRANSFERABLE_DATA) {
                callback.OnProgress(mtpObjectInfo);
                //filesObjects.add(mtpObjectInfo);
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
    }

    interface Callback {
        void OnProgress(MtpObjectInfo data);
    }
}

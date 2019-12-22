package com.remoteyourcam.usb;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NavUtils;

import com.google.zxing.WriterException;
import com.remoteyourcam.usb.ptp.Camera;
import com.remoteyourcam.usb.ptp.PtpConstants;
import com.remoteyourcam.usb.ptp.model.LiveViewData;
import com.remoteyourcam.usb.ptp.model.ObjectInfo;
import com.remoteyourcam.usb.util.AsyncResponse;
import com.remoteyourcam.usb.util.Upload;

import org.json.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FotoboothActivity extends CameraBaseActivity implements Camera.CameraListener, Camera.StorageInfoListener,
        Camera.RetrieveImageInfoListener, Camera.RetrieveImageListener, AsyncResponse {

    private Handler handler;
    private Runnable runnable;

    private Button mStartPhotoboothCountdownBtn;
    private TextView mCountdownText;
    private PictureView mPictureView;
    private ImageView mLiveView;
    private Button mStartPreviewMode;
    private LinearLayout mDashboardLayout;
    private FrameLayout mPictureTakenLayout;
    private FrameLayout mPreviewLayout;
    private Button mKeepPictureBtn;
    private Button mCancelPictureBtn;
    private FrameLayout mReviewLayout;
    private Button mTakeNewPhotoBtn;
    private Button mShowQrBtn;
    private ImageView mReviewPicture;
    private ImageView mQrImageView;
    private FrameLayout mQRLayout;
    private Button mQrNewPhotoBtn;

    private ImageView mTakenPicture;

    private LiveViewData currentLiveViewData;
    private LiveViewData currentLiveViewData2;

    private int mCurrentCountDownValue;
    private Bitmap mTakenPictureBitmap;

    private Upload uploader = new Upload();

    private String photoDownloadLink;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    public void startPhotoCountdown() {
        this.mCurrentCountDownValue = 5;
        mCountdownText.setVisibility(View.VISIBLE);
        mStartPhotoboothCountdownBtn.setVisibility(View.INVISIBLE);
        mCountdownText.setText(Integer.toString(mCurrentCountDownValue));

        new CountDownTimer(mCurrentCountDownValue * 1000 + 2, 1000) {

            public void onTick(long millisUntilFinished) {
                // Toast.makeText(getBaseContext(), Integer.toString(mCurrentCountDownValue), Toast.LENGTH_SHORT).show();
                mCurrentCountDownValue--;
                mCountdownText.setText(Integer.toString(mCurrentCountDownValue));
            }

            public void onFinish() {
                mCurrentCountDownValue--;
                mCountdownText.setText(Integer.toString(mCurrentCountDownValue));
                Log.i("PhotoboothActivity", "Capture!");
                startPictureTakenMode();
                //camera.retrievePicture();
            }
        }.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setPTPInstance(this);
        super.setListener(this);

        setContentView(R.layout.activity_fotobooth);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mStartPhotoboothCountdownBtn = (Button)findViewById(R.id.startPhotoboothCountdownBtn);
        mCountdownText = (TextView) findViewById(R.id.countDownText);
        // mImageView = (ImageView) findViewById(R.id.liveview_preview);
        mPictureView = (PictureView) findViewById(R.id.liveview_preview);
        mLiveView = (ImageView) findViewById(R.id.liveview_preview_2);
        mStartPreviewMode = (Button) findViewById(R.id.startPreviewMode);
        mDashboardLayout = (LinearLayout) findViewById(R.id.layout_dashboard);
        mTakenPicture = (ImageView) findViewById(R.id.taken_picture);
        mPictureTakenLayout = (FrameLayout) findViewById(R.id.layout_picture_taken);
        mPreviewLayout = (FrameLayout) findViewById(R.id.layout_preview);

        mKeepPictureBtn = (Button) findViewById(R.id.keep_picture);
        mCancelPictureBtn = (Button) findViewById(R.id.cancel_picture);

        mReviewLayout = (FrameLayout) findViewById(R.id.layout_picture_review);
        mTakeNewPhotoBtn = (Button) findViewById(R.id.button_take_new_photo);
        mShowQrBtn = (Button) findViewById(R.id.button_show_qr);
        mReviewPicture = (ImageView) findViewById(R.id.bitmap_review_picture);
        mQrImageView = (ImageView) findViewById(R.id.qr_image);

        mQRLayout = (FrameLayout) findViewById(R.id.layout_qr);
        mQrNewPhotoBtn = (Button) findViewById(R.id.qr_new_photo_button);

        mCountdownText.setVisibility(View.INVISIBLE);

        uploader.setDelegate(this);


        //Upload.upload(image, "Filename.jpg");

        mStartPhotoboothCountdownBtn.setVisibility(View.INVISIBLE);
        mPictureView.setVisibility(View.INVISIBLE);
        mLiveView.setVisibility(View.INVISIBLE);
        mPictureTakenLayout.setVisibility(View.INVISIBLE);
        mReviewLayout.setVisibility(View.INVISIBLE);
        mQRLayout.setVisibility(View.INVISIBLE);
        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.photobooth_take_picture_countdown).setOnTouchListener(mDelayHideTouchListener);

        mStartPhotoboothCountdownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPhotoCountdown();
            }
        });

        mStartPreviewMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPrevieMode();
            }
        });

        mCancelPictureBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mTakenPicture.setImageBitmap(null);
                startPrevieMode();
            }
        });

        mKeepPictureBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startReviewMode();
            }
        });

        mTakeNewPhotoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPrevieMode();
            }
        });
        mQrNewPhotoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPrevieMode();
            }
        });

        mShowQrBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startQRMode();
            }
        });
    }

    public void startPrevieMode() {
        mLiveView.setImageBitmap(null);
        mPreviewLayout.setVisibility(View.VISIBLE);
        mDashboardLayout.setVisibility(View.INVISIBLE);
        mStartPhotoboothCountdownBtn.setVisibility(View.VISIBLE);
        //mPictureView.setVisibility(View.VISIBLE);
        mLiveView.setVisibility(View.VISIBLE);
        mPictureTakenLayout.setVisibility(View.INVISIBLE);
        mTakenPicture.setVisibility(View.VISIBLE);
        mCountdownText.setVisibility(View.INVISIBLE);
        mReviewLayout.setVisibility(View.INVISIBLE);
        mQRLayout.setVisibility(View.INVISIBLE);

        this.camera.setLiveView(true);
        camera.getLiveViewPicture(null);
    }


    public void startPictureTakenMode() {
        mTakenPicture.setImageBitmap(null);
        mKeepPictureBtn.setEnabled(false);
        camera.capture();
        //camera.retrievePicture(1);
        mPreviewLayout.setVisibility(View.INVISIBLE);
        mDashboardLayout.setVisibility(View.INVISIBLE);
        mStartPhotoboothCountdownBtn.setVisibility(View.INVISIBLE);
        mPictureView.setVisibility(View.INVISIBLE);
        mLiveView.setVisibility(View.INVISIBLE);
        mReviewLayout.setVisibility(View.INVISIBLE);
        mQRLayout.setVisibility(View.INVISIBLE);
        camera.retrieveStorages(this);

        mPictureTakenLayout.setVisibility(View.VISIBLE);
    }


    public void startReviewMode() {
        mReviewPicture.setImageBitmap(mTakenPictureBitmap);
        mDashboardLayout.setVisibility(View.INVISIBLE);
        mStartPhotoboothCountdownBtn.setVisibility(View.INVISIBLE);
        mPictureView.setVisibility(View.INVISIBLE);
        mLiveView.setVisibility(View.INVISIBLE);
        mPictureTakenLayout.setVisibility(View.INVISIBLE);
        mReviewLayout.setVisibility(View.VISIBLE);
        mQRLayout.setVisibility(View.INVISIBLE);
        startUpload();


    }

    public void startQRMode() {
        mDashboardLayout.setVisibility(View.INVISIBLE);
        mStartPhotoboothCountdownBtn.setVisibility(View.INVISIBLE);
        mPictureView.setVisibility(View.INVISIBLE);
        mLiveView.setVisibility(View.INVISIBLE);
        mPictureTakenLayout.setVisibility(View.INVISIBLE);
        mReviewLayout.setVisibility(View.INVISIBLE);

        mQRLayout.setVisibility(View.VISIBLE);


    }

    public void startUpload() {
        try {
           uploader.execute(mTakenPictureBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (AppConfig.LOG) {
           // Log.i(TAG, "onStart");
        }
        isInStart = true;
        ptp.setCameraListener(this);
        ptp.initialize(this, getIntent());
    }
    public void onCameraStarted(final Camera camera) {
        Log.i("FotoboothActivity", "onCameraStarted");
        super.onCameraStarted(camera);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
       // mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onLiveViewData(LiveViewData data) {
        Log.i("LOG", "on liveviewdata");
        //this.mPictureView.setLiveViewData(data);
        if (data == null || data.bitmap == null) {
            return;
        }
        this.mLiveView.setImageBitmap(data.bitmap);
        // this.mTakenPicture.setImageBitmap(data.bitmap);

        currentLiveViewData2 = currentLiveViewData;
        this.currentLiveViewData = data;
        camera.getLiveViewPicture(currentLiveViewData2);

        // sessionFrag.liveViewData(data);
    }

    @Override
    public void onLiveViewStopped() {
       Log.i("FotoboothActivity", "LiveView stopped");
    }

    @Override
    public void onCapturedPictureReceived(int objectHandle, String filename, Bitmap thumbnail, Bitmap bitmap) {
        Log.i("Fotobooth Activity", "Picture Taken");
        if (thumbnail != null) {
            //sessionFrag.capturedPictureReceived(objectHandle, filename, thumbnail, bitmap);
        } else {
            Toast.makeText(this, "No thumbnail available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageRetrieved(int objectHandle, final Bitmap image) {
        //
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                mTakenPicture.setImageBitmap(image);
                mTakenPictureBitmap = image;
                mKeepPictureBtn.setEnabled(true);
            }
        });
    }

    @Override
    public void onObjectAdded(int handle, int format) {
        //sessionFrag.objectAdded(handle, format);
        Log.i("FotoboothActivity", "objectAdded!".concat(Integer.toString(handle)));
       camera.retrieveImage(this, handle); // todo: test!!
    }

    @Override
    public void onStorageFound(int handle, String label) {
        Log.i("FotoboothActivity", "Storage found!!".concat(label));
        camera.retrieveImageHandles(this, handle);
    }

    @Override
    public void onAllStoragesFound() {

    }

    @Override
    public void onImageHandlesRetrieved(int[] handles) {
        Log.i("FotoboothActivity", "Handles received (%s)".concat(Integer.toString(handles.length)));
    }

    @Override
    public void onImageInfoRetrieved(int objectHandle, ObjectInfo objectInfo, Bitmap thumbnail) {

    }

    @Override
    public void processFinish(String output) {
        try {
            JSONObject obj = new JSONObject(output);
            Integer imageId = obj.getJSONObject("image").getInt("id");
            photoDownloadLink = "https://weddingapi.never-al.one/getImage/".concat(Integer.toString(imageId));

            QRGEncoder qrgEncoder = new QRGEncoder(
                    photoDownloadLink, null,
                    QRGContents.Type.TEXT,
                    512);
            try {
                final Bitmap qrCodeBitm = qrgEncoder.encodeAsBitmap();
                //
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        mQrImageView.setImageBitmap(qrCodeBitm);
                    }
                });

            } catch (WriterException e) {
                Log.v("QRCODING", e.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

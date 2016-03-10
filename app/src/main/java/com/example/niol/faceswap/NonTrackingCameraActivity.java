package com.example.niol.faceswap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.example.niol.faceswap.livedetect.CameraErrorCallback;
import com.example.niol.faceswap.livedetect.FaceOverlayView;
import com.example.niol.faceswap.livedetect.Util;

import java.util.List;

/**
 * Created by Devin on 3/9/16.
 */
public class NonTrackingCameraActivity extends Activity
        implements SurfaceHolder.Callback {

    public static final String TAG = NonTrackingCameraActivity.class.getSimpleName();

    private Camera mCamera;
    private Bitmap mPictureBitmap;

    // We need the phone orientation to correctly draw the overlay:
    private int mOrientation;
    private int mOrientationCompensation;
    private OrientationEventListener mOrientationEventListener;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    // The surface view for the camera data
    private SurfaceView mView;

    private NonTrackingCameraControlView mControlView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new SurfaceView(this);
        setContentView(mView);

        // Now create the OverlayView:
        mControlView = new NonTrackingCameraControlView(this);
        addContentView(mControlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        mOrientationEventListener = new SimpleOrientationEventListener(this);
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        mOrientationEventListener.disable();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mOrientationEventListener.enable();
        super.onResume();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();
        Camera.Parameters params = mCamera.getParameters();
        setAutoFocus(params);
        mCamera.startFaceDetection();
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }

        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        // Everything is configured! Finally start the camera preview again:
        mCamera.startPreview();
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(mErrorCallback);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = Util.getDisplayRotation(NonTrackingCameraActivity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, 0);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mControlView != null) {
            mControlView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        parameters.set("camera-id", 2);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallback(null);
        mCamera.setFaceDetectionListener(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    /**
     * We need to react on OrientationEvents to rotate the screen and
     * update the views.
     */
    private class SimpleOrientationEventListener extends OrientationEventListener {

        public SimpleOrientationEventListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(NonTrackingCameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
            }
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                mControlView.setOrientation(mOrientationCompensation);
            }
        }
    }

    public Bitmap takePicture() {
        mCamera.takePicture(null, null, mPicture);
        return mPictureBitmap;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken (byte [] data, Camera camera) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            mPictureBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        }
    };
}

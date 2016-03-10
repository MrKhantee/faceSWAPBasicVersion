package com.example.niol.faceswap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.View;

import com.example.niol.faceswap.livedetect.CameraActivity;
import com.example.niol.faceswap.livedetect.Util;

/**
 * Created by Devin on 3/9/16.
 */
public class NonTrackingCameraControlView extends View {
    private Paint mPaint;
    private Bitmap cameraButton, cameraButtonPressed, chooseButton, chooseButtonPressed;
    private boolean isCameraButtonPressed, isChooseButtonPressed;
    private Rect cameraButtonPos, chooseButtonPos;

    private int mDisplayOrientation;
    private int mOrientation;

    public NonTrackingCameraControlView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // We want a green box around the face:
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setAlpha(255);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        cameraButton = BitmapFactory.decodeResource(getResources(), R.drawable.camera_button);
        cameraButtonPressed = BitmapFactory.decodeResource(getResources(), R.drawable.camera_button_pressed);
        chooseButton = BitmapFactory.decodeResource(getResources(), R.drawable.choose_button);
        chooseButtonPressed = BitmapFactory.decodeResource(getResources(), R.drawable.choose_button_pressed);

        isCameraButtonPressed = false;
        isChooseButtonPressed = false;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (cameraButtonPos == null) {
            cameraButtonPos = new Rect(0, 0, cameraButton.getWidth() / 2, cameraButton.getHeight() / 2);
            cameraButtonPos.offset(getWidth() - cameraButtonPos.width() - 20, getHeight() - 20 - cameraButtonPos.height());

            chooseButtonPos = new Rect(0, 0, chooseButton.getWidth(), chooseButton.getHeight());
            chooseButtonPos.offset(20, getHeight() - 20 - chooseButtonPos.height());
        }

        if (isCameraButtonPressed)
            canvas.drawBitmap(cameraButtonPressed, null, cameraButtonPos, null);
        else
            canvas.drawBitmap(cameraButton, null, cameraButtonPos, null);

        if (isChooseButtonPressed)
            canvas.drawBitmap(chooseButtonPressed, null, chooseButtonPos, null);
        else
            canvas.drawBitmap(chooseButton, null, chooseButtonPos, null);

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isCameraButtonPressed) {
                takePicture();
                isCameraButtonPressed = false;
            }
            if (isChooseButtonPressed) {
                choosePicture();
                isChooseButtonPressed = false;
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int)event.getX();
            int y = (int)event.getY();

            if (cameraButtonPos.contains(x, y))
                isCameraButtonPressed = true;

            if (chooseButtonPos.contains(x, y))
                isChooseButtonPressed = true;
        }

        return true;
    }

    public void takePicture() {
        Bitmap picture = ((CameraActivity)getContext()).takePicture();

    }

    public void choosePicture() {

    }
}

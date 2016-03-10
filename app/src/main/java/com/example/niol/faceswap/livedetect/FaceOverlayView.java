// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.example.niol.faceswap.livedetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

import com.example.niol.faceswap.R;

/**
 * This class is a simple View to display the faces.
 */
public class FaceOverlayView extends View {

    private Paint mPaint;
    private Paint mTextPaint;
    private Bitmap mask, glasses, currentFace;
    private Bitmap glassesButton, glassesButtonPressed, maskButton, maskButtonPressed, cameraButton, cameraButtonPressed;
    private boolean isGlassesButtonPressed, isMaskButtonPressed, isCameraButtonPressed;
    private Rect glassesButtonPos, maskButtonPos, cameraButtonPos;

    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;

    public FaceOverlayView(Context context) {
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

        mask = BitmapFactory.decodeResource(getResources(), R.drawable.mask);
        glasses = BitmapFactory.decodeResource(getResources(), R.drawable.glasses);
        glassesButton = BitmapFactory.decodeResource(getResources(), R.drawable.glasses_button);
        glassesButtonPressed = BitmapFactory.decodeResource(getResources(), R.drawable.glasses_button_pressed);
        maskButton = BitmapFactory.decodeResource(getResources(), R.drawable.mask_button);
        maskButtonPressed = BitmapFactory.decodeResource(getResources(), R.drawable.mask_button_pressed);
        cameraButton = BitmapFactory.decodeResource(getResources(), R.drawable.camera_button);
        cameraButtonPressed = BitmapFactory.decodeResource(getResources(), R.drawable.camera_button_pressed);

        currentFace = mask;

        isGlassesButtonPressed = false;
        isMaskButtonPressed = false;
        isCameraButtonPressed = false;
    }

    public void setFaces(Face[] faces) {
        mFaces = faces;
        invalidate();
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
        if (glassesButtonPos == null || maskButtonPos == null) {
            glassesButtonPos = new Rect(0, 0, glassesButton.getWidth(), glassesButton.getHeight());
            maskButtonPos = new Rect(0, 0, maskButton.getWidth(), maskButton.getHeight());
            cameraButtonPos = new Rect(0, 0, cameraButton.getWidth(), cameraButton.getHeight());
            glassesButtonPos.offset(20, getHeight() - 20 - glassesButtonPos.height());
            maskButtonPos.offset(20, glassesButtonPos.top - 20 - maskButton.getHeight());
            cameraButtonPos.offset(getWidth() - 20 - cameraButtonPos.width(), getHeight() - 20 - cameraButtonPos.height());
        }

        if (mFaces != null && mFaces.length > 0) {
            Matrix matrix = new Matrix();
            Util.prepareMatrix(matrix, false, mDisplayOrientation, getWidth(), getHeight());
            canvas.rotate(-mOrientation);
            matrix.postRotate(mOrientation);
            RectF rectF = new RectF();
            Face face = mFaces[0];
            rectF.set(face.rect);
            float width = rectF.width();
            float height = rectF.height();
            float scaleY = 0.1f;
            float scaleX = 0.2f;
            rectF.left = rectF.left - width * scaleX;
            rectF.top = rectF.top - height * scaleY;
            rectF.right = rectF.right + width * scaleX;
            rectF.bottom = rectF.bottom + height * scaleY;
            rectF.offset(0, height * 0.05f);
            matrix.mapRect(rectF);
            canvas.drawBitmap(currentFace, null, rectF, mPaint);
        }

        if (isGlassesButtonPressed)
            canvas.drawBitmap(glassesButtonPressed, null, glassesButtonPos, null);
        else
            canvas.drawBitmap(glassesButton, null, glassesButtonPos, null);

        if (isMaskButtonPressed)
            canvas.drawBitmap(maskButtonPressed, null, maskButtonPos, null);
        else
            canvas.drawBitmap(maskButton, null, maskButtonPos, null);

        if (isCameraButtonPressed)
            canvas.drawBitmap(cameraButtonPressed, null, cameraButtonPos, null);
        else
            canvas.drawBitmap(cameraButton, null, cameraButtonPos, null);

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isGlassesButtonPressed) {
                currentFace = glasses;
                isGlassesButtonPressed = false;
            }

            if (isMaskButtonPressed) {
                currentFace = mask;
                isMaskButtonPressed = false;
            }

            if (isCameraButtonPressed) {
                takePicture();
                isCameraButtonPressed = false;
            }
        }

        else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int)event.getX();
            int y = (int)event.getY();

            if (glassesButtonPos.contains(x, y))
                isGlassesButtonPressed = true;
            if (maskButtonPos.contains(x, y))
                isMaskButtonPressed = true;
            if (cameraButtonPos.contains(x, y))
                isCameraButtonPressed = true;
        }

        return true;
    }

    public void takePicture() {
        Bitmap picture = ((CameraActivity)getContext()).takePicture();

        // TESTING BULLSHIT
        currentFace = picture;
    }
}
package com.example.niol.faceswap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by niol on 2016/2/14.
 */
public class ModifiedFaceView extends View implements Runnable{
    private Bitmap mBitmap, mBitmap2,finalBitmap;
    private SparseArray<Face> mFaces, mFaces2;

//    private GestureDetector gestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private static final float AXIS_X_MIN = -1f;
    private static final float AXIS_X_MAX = 1f;
    private static final float AXIS_Y_MIN = -1f;
    private static final float AXIS_Y_MAX = 1f;

    private RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
    private Rect mContentRect = new Rect();

    /****************************************/
    // for zooming and dragging             //
    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;
    private float scaleFactor = 1.f;
    private static int NONE = 0;
    private static int DRAG = 1;
    private static int ZOOM = 2;
    private int mode;
    private float startX = 0f;
    private float startY = 0f;
    private float translateX = 0f;
    private float translateY = 0f;
    private float previousTranslateX = 0f;
    private float previousTranslateY = 0f;
    private boolean dragged = true;
    /****************************************/

    public ModifiedFaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
//        gestureDetector = new GestureDetector(context, mGestureListener);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * Sets the bitmap background and the associated face detections.
     */
    void setContent() {
        invalidate();
    }
    float leftEyeX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(0).getPosition().x;
    }
    float leftEyeY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(0).getPosition().y;
    }
    float rightEyeX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(1).getPosition().x;
    }
    float rightEyeY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(1).getPosition().y;
    }
    float noseX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(2).getPosition().x;
    }
    float noseY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(2).getPosition().y;
    }
    float leftCheekX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(4).getPosition().x;
    }
    float leftCheekY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(4).getPosition().y;
    }
    float rightCheekX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(3).getPosition().x;
    }

    float rightCheekY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(3).getPosition().y;
    }
    float leftMouthX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(6).getPosition().x;
    }
    float leftMouthY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(6).getPosition().y;
    }
    float rightMouthX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(5).getPosition().x;
    }
    float rightMouthY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(5).getPosition().y;
    }
    float downMouthX(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(7).getPosition().x;
    }
    float downMouthY(SparseArray<Face> faces) {
        return faces.valueAt(0).getLandmarks().get(7).getPosition().y;
    }
    float leftMouthBorder(SparseArray<Face> faces) {
        float k = 0.1f;
        return leftMouthX(faces) - k *(rightMouthX(faces) - leftMouthX(faces));
    }
    float MouthWidth(SparseArray<Face> faces) {
        float k = 0.1f;
        return (1 + 2 * k) * (rightMouthX(faces) - leftMouthX(faces));
    }
    float  k3 = 0.15f;
    float topMouthBorder(SparseArray<Face> faces) {
        return eyeHeight(faces) + topEyeBorder(faces);
    }
    float mouthHeight(SparseArray<Face> faces) {
        float k = 1f;
        return k * (downMouthY(faces) - leftEyeY(faces));
    }
    float leftEyeBorder(SparseArray<Face> faces) {
        float k = 0.38f;
        return leftEyeX(faces) - k *(rightEyeX(faces) - leftEyeX(faces));
    }
    float eyeWidth(SparseArray<Face> faces) {
        float k = 0.38f;
        return (1 + 2 * k) * (rightEyeX(faces) - leftEyeX(faces));
    }
    float topEyeBorder(SparseArray<Face> faces) {
        float k = 0.7f;
        return leftEyeY(faces) - k * (noseY(faces) - leftEyeY(faces));
    }
    float eyeHeight(SparseArray<Face> faces) {
        float k = 0.98f;
        return k * (noseY(faces) - leftEyeY(faces));
    }
    int averageCheekColor(Bitmap bitmap, SparseArray<Face> faces){
        int leftCheekColor = bitmap.getPixel((int)faces.valueAt(0).getLandmarks().get(3).getPosition().x,(int)faces.valueAt(0).getLandmarks().get(3).getPosition().y),
                righttCheekColor = bitmap.getPixel((int)faces.valueAt(0).getLandmarks().get(4).getPosition().x,(int)faces.valueAt(0).getLandmarks().get(4).getPosition().y);
        return leftCheekColor ;
    }
    void smoothRegion(Bitmap bitmap, int left, int top, int right, int down){
        for(int x = left; x < right; x++){
            for(int y = top; y < down; y++){

            }
        }
    }
    boolean isColorDifferent(Bitmap bitmap,int x1,int y1,int x2,  int y2){
        int color1 = bitmap.getPixel(x1,y1), color2 =bitmap.getPixel(x2,y2),
                threshold = 150;
        int red1 = (color1  / 256 / 256) % 256, green1 = (color1 / 256) % 256, blue1 = color1 % 256,
                red2 = (color2 / 256 / 256) % 256, green2 = (color2 / 256) % 256, blue2 = color2 % 256;
        return (Math.abs(Color.red(color1) - Color.red(color2)) + Math.abs(Color.green(color1) - Color.green(color2)) + Math.abs(Color.blue(color1) - Color.blue(color2)) > threshold)?true:false;

    }
    int averageColor(int leftColor,int topColor,int rightColor,int downColor,float leftDistance,float topDistance,float rightDistance,float downDistance){
        float minDistance = 1f / leftDistance + 1f / rightDistance + 1f / topDistance + 1f / downDistance,alpha,red,green,blue;

        alpha = ((float)Color.alpha(leftColor) / leftDistance + (float)Color.alpha(rightColor) / rightDistance + (float)Color.alpha(topColor) / topDistance + (float)Color.alpha(downColor) / downDistance) / minDistance;
        red = ((float)Color.red(leftColor) / leftDistance + (float)Color.red(rightColor) / rightDistance + (float)Color.red(topColor) / topDistance + (float)Color.red(downColor) / downDistance) / minDistance;
        green = ((float)Color.green(leftColor) / leftDistance + (float)Color.green(rightColor) / rightDistance + (float)Color.green(topColor) / topDistance + (float)Color.green(downColor) / downDistance) / minDistance;
        blue = ((float)Color.blue(leftColor) / leftDistance + (float)Color.blue(rightColor) / rightDistance + (float)Color.blue(topColor) / topDistance + (float)Color.blue(downColor) / downDistance) / minDistance;
        return Color.argb((int)alpha,(int)red,(int)green,(int)blue);
    }
    void deleteEyes(Bitmap bitmap, SparseArray<Face> faces){
        int leftEyeBorder = (int)leftEyeBorder(faces),
                rightEyeBorder = (int)(leftEyeBorder(faces) + eyeWidth(faces)),
                downEyeBorder = (int)(topEyeBorder(faces) + eyeHeight(faces)),
                topEyeBorder = (int)topEyeBorder(faces),
                leftCheekX = (int)leftCheekX(faces),leftCheekY = (int)leftCheekY(faces),
                averageColor,topColor,downColor,leftColor,rightColor,leftCheekColor = bitmap.getPixel(leftCheekX,leftCheekY);

       for(int x = leftEyeBorder + 1;x < rightEyeBorder; x++){
            for(int y = topEyeBorder + 1;y < downEyeBorder; y++) {
                if (isColorDifferent(bitmap, x, y, leftCheekX, leftCheekY)) {
                    bitmap.setPixel(x, y, bitmap.getPixel(x ,y - 1));
                }
            }
        }
    }
    void extractEyes(Bitmap bitmap, SparseArray<Face> faces,Bitmap bitmap2, SparseArray<Face> faces2){
        //extract face from bitmap and put the face on the bitmap2
        int leftEyeBorder = (int)leftEyeBorder(faces),
                rightEyeBorder = (int)(leftEyeBorder(faces) + eyeWidth(faces)),
                downEyeBorder = (int)(topEyeBorder(faces) + eyeHeight(faces)),
                topEyeBorder = (int)topEyeBorder(faces),
                leftCheekX = (int)leftCheekX(faces),leftCheekY = (int)leftCheekY(faces);
        int leftEyeBorder2 = (int)leftEyeBorder(faces2),
                rightEyeBorder2 = (int)(leftEyeBorder(faces2) + eyeWidth(faces2)),
                downEyeBorder2 = (int)(topEyeBorder(faces2) + eyeHeight(faces2)),
                topEyeBorder2 = (int)topEyeBorder(faces2),
                leftCheekX2 = (int)leftCheekX(faces2),leftCheekY2 = (int)leftCheekY(faces2),
                color;
        float x2,y2;

        for(int x = leftEyeBorder + 1;x < rightEyeBorder; x++){
            for(int y = topEyeBorder + 1;y < downEyeBorder; y++){
                if(isColorDifferent(bitmap,x,y,leftCheekX,leftCheekY)) {
                    x2 = eyeWidth(faces2) / eyeWidth(faces) *(float) (x - leftEyeBorder) +(float)leftEyeBorder2 ;
                    y2 = eyeHeight(faces2) / eyeHeight(faces) *(float) (y - topEyeBorder) + (float)topEyeBorder2;
                    color = bitmap.getPixel((int)noseX(faces),(int)noseY(faces));
                    //bitmap2.setPixel((int)x2,(int)y2,color);

                    bitmap2.setPixel((int)x2,(int)y2,bitmap.getPixel(x,y));
                    //averageColor = bitmap.getPixel(leftCheekX,leftCheekY);
                    //bitmap.setPixel(x,y,averageColor);
                }
            }
        }
    }
    void deleteFace(Bitmap bitmap, SparseArray<Face> faces){
        deleteEyes(bitmap, faces);
        //deleteNoseAndMouth(bitmap,faces);
    }
    void deleteNoseAndMouth(Bitmap bitmap, SparseArray<Face> faces) {
        //int leftEyeBorder =
        //deleteRegion(bitmap,leftEyeBorder,topEyeBorder)
    }

    /************************************************************************/
    // for zooming and dragging                                             //
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                startX = ev.getX() - previousTranslateX;
                startY = ev.getY() - previousTranslateY;
                break;
            case MotionEvent.ACTION_MOVE:
                translateX = ev.getX() - startX;
                translateY = ev.getY() - startY;

                double distance = Math.sqrt(Math.pow(ev.getX() - (startX + previousTranslateX), 2) + Math.pow(ev.getY() - (startY + previousTranslateY), 2));

                if(distance > 0) {
                    dragged = true;
                }

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;
                break;
            case MotionEvent.ACTION_UP:
                mode = NONE;
                dragged = false;
                previousTranslateX = translateX;
                previousTranslateY = translateY;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = DRAG;
                previousTranslateX = translateX;
                previousTranslateY = translateY;
                break;
        }
//        gestureDetector.onTouchEvent(ev);
        mScaleDetector.onTouchEvent(ev);

        //The only time we want to re-draw the canvas is if we are panning (which happens when the mode is
        //DRAG and the zoom factor is not equal to 1) or if we're zooming
        if ((mode == DRAG && scaleFactor != 1f && dragged) || mode == ZOOM) {
            invalidate();
        }

        return true;
    }

    /**
     * Draws the bitmap background and the associated face landmarks.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.scale(mScaleFactor, mScaleFactor);

//        if((translateX * -1) < 0) {
//            translateX = 0;
//        } else if ((translateX * -1) > (scaleFactor - 1) * canvas.getWidth()) {
//            translateX = (1 - scaleFactor) * canvas.getWidth();
//        }
//
//        if(translateY * -1 < 0) {
//            translateY = 0;
//        } else if((translateY * -1) > (scaleFactor - 1) * canvas.getHeight()) {
//            translateY = (1 - scaleFactor) * canvas.getHeight();
//        }

//        translateX = (translateX * -1) < 0 ? 0 : translateX;
//        translateY = (translateY * -1) < 0 ? 0 : translateY;

        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);
        //canvas.clipRect(mContentRect);

        if ((mBitmap != null) && (mFaces != null ) && (mBitmap2 != null) && (mFaces2 != null)) {
            double scale = drawBitmap(canvas);
            //drawFaceAnnotations(canvas, scale);
            //Bitmap resizedbitmap=Bitmap.createBitmap(bitmap, 0,0,(int)faces.valueAt(0).getLandmarks().get(0).getPosition().x,(int)faces.valueAt(0).getLandmarks().get(0).getPosition().y);

        }
        canvas.restore();
    }
    /**
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     *
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     */
    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    /**
     * Draws the bitmap background, scaled to the device size.  Returns the scale for future use in
     * positioning the facial landmark graphics.
     */
    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        //Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale / 2), (int)(imageHeight * scale / 2));
        //Rect destBounds = new Rect(0, 0, (int)(mFaces.valueAt(0).getLandmarks().get(0).getPosition().x * scale), (int)(mFaces.valueAt(0).getLandmarks().get(0).getPosition().y * scale));

        //Bitmap resizedbitmap=Bitmap.createBitmap(mBitmap, (int)leftBorder, (int)upperborder , (int)width,(int)height);
        //Bitmap resizedbitmap=Bitmap.createBitmap(mBitmap, (int)leftBorder,(int)upperborder, (int)rightBorder,(int)lowerborder);

        //canvas.drawBitmap(resizedbitmap,0,0,null);
        //canvas.drawBitmap(resizedbitmap,null,destBounds,null);
        //canvas.drawBitmap(mBitmap, null, destBounds, null);

        //do the same thing for mBitmap2
        double imageWidth2 = mBitmap2.getWidth();
        double imageHeight2 = mBitmap2.getHeight();
        double scale2 = Math.min(viewWidth / imageWidth2, viewHeight / imageHeight2);

        //Rect destBounds2 = new Rect(0, 0, (int)(imageWidth2 * scale / 2), (int)(imageHeight2 * scale / 2));
        //Rect destBounds = new Rect(0, 0, (int)(mFaces.valueAt(0).getLandmarks().get(0).getPosition().x * scale), (int)(mFaces.valueAt(0).getLandmarks().get(0).getPosition().y * scale));

       /* Bitmap mutableBitmap = ModifiedFaceView.convertToMutable(Bitmap.createBitmap(mBitmap2));
        deleteFace(mutableBitmap,mFaces2);
        extractEyes(mBitmap,mFaces,mutableBitmap,mFaces2);*/
        Bitmap resizedbitmap = Bitmap.createBitmap(mBitmap, (int)leftEyeBorder(mFaces), (int)topEyeBorder(mFaces) , (int)eyeWidth(mFaces),(int)eyeHeight(mFaces));
        //Bitmap resizedbitmap=Bitmap.createBitmap(mBitmap, (int)leftBorder,(int)upperborder, (int)rightBorder,(int)lowerborder);
        Rect destBounds = new Rect(0, 0, (int)(imageWidth2 * scale2 ), (int)(imageHeight2 * scale2 ));
        Rect destBounds2 = new Rect((int)(leftEyeBorder(mFaces2) * scale2), (int)(topEyeBorder(mFaces2) * scale2), (int)((leftEyeBorder(mFaces2) + eyeWidth(mFaces2)) * scale2), (int)((topEyeBorder(mFaces2) + eyeHeight(mFaces2)) * scale2));

        //canvas.drawBitmap(resizedbitmap,0,0,null);
        canvas.drawBitmap(finalBitmap, null, destBounds, null);
        //canvas.drawBitmap(resizedbitmap,null,destBounds2,null);
        //canvas.drawBitmap(mBitmap2, null, destBounds, null);
        return scale;
    }

    /**
     * Draws a small circle for each detected landmark, centered at the detected landmark position.
     * <p>
     *
     * Note that eye landmarks are defined to be the midpoint between the detected eye corner
     * positions, which tends to place the eye landmarks at the lower eyelid rather than at the
     * pupil position.
     */
    private void drawFaceAnnotations(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        for (int i = 0; i < mFaces.size(); ++i) {
            Face face = mFaces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);
            }
        }
    }
    public void getPhotosAndFaces(Bitmap bitmap, SparseArray<Face> faces,Bitmap bitmap2,SparseArray<Face> faces2) {
        mBitmap = bitmap;
        mFaces = faces;

        mBitmap2 = bitmap2;
        mFaces2 = faces2;
    }

    public void setData(ModifiedFaceView modifiedFaceView) {
        mBitmap = modifiedFaceView.mBitmap;
        mBitmap2 = modifiedFaceView.mBitmap2;
        finalBitmap = modifiedFaceView.finalBitmap;
        mFaces = modifiedFaceView.mFaces;
        mFaces2 = modifiedFaceView.mFaces2;
    }

    @Override
    public void run() {
        //finalBitmap = Bitmap.createBitmap(mBitmap2);
        //finalBitmap = ModifiedFaceView.convertToMutable(finalBitmap);
        finalBitmap = mBitmap2.copy(mBitmap2.getConfig(),true);
        deleteFace(finalBitmap,mFaces2);

        extractEyes(mBitmap,mFaces,finalBitmap,mFaces2);
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(MIN_ZOOM, Math.min(mScaleFactor, MAX_ZOOM));

            invalidate();
            return true;
        }
    }

//    private final GestureDetector.SimpleOnGestureListener mGestureListener
//            = new GestureDetector.SimpleOnGestureListener() {
//
//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2,
//                                float distanceX, float distanceY) {
//            // Scrolling uses math based on the viewport (as opposed to math using pixels).
//
//            // Pixel offset is the offset in screen pixels, while viewport offset is the
//            // offset within the current viewport.
//            float viewportOffsetX = distanceX * mCurrentViewport.width()
//                    / mContentRect.width();
//            float viewportOffsetY = -distanceY * mCurrentViewport.height()
//                    / mContentRect.height();
//
//            // Updates the viewport, refreshes the display.
//            setViewportBottomLeft(
//                    mCurrentViewport.left + viewportOffsetX,
//                    mCurrentViewport.bottom + viewportOffsetY);
//
//            return true;
//        }
//    };

    /**
     * Sets the current viewport (defined by mCurrentViewport) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position,
     * and thus the bottom of the mCurrentViewport rectangle.
     */
//    private void setViewportBottomLeft(float x, float y) {
//
//        float curWidth = mCurrentViewport.width();
//        float curHeight = mCurrentViewport.height();
//        x = Math.max(AXIS_X_MIN, Math.min(x, AXIS_X_MAX - curWidth));
//        y = Math.max(AXIS_Y_MIN + curHeight, Math.min(y, AXIS_Y_MAX));
//
//        mCurrentViewport.set(x, y - curHeight, x + curWidth, y);
//
//        // Invalidates the View to update the display.
//        ViewCompat.postInvalidateOnAnimation(this);
//    }
}





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
import android.util.Log;
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
    private Bitmap bmp;
    private SparseArray<Face> faces;

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


    int[][] getFacePoints(Face face) {
        float[] x = new float[6];
        float[] y = new float[6];

        x[0] = leftEyeBorder(face);
        y[0] = topEyeBorder(face);
        x[1] = leftEyeBorder(face) + eyeWidth(face);
        y[1] = y[0];
        x[2] = x[1];
        y[2] = rightCheekY(face);
        x[3] = (downMouthX(face) + rightCheekX(face)) / 2;
        y[3] = downMouthY(face) + 0.2f * (downMouthY(face) - y[0]);
        x[4] = (downMouthX(face) + leftCheekX(face)) / 2;
        y[4] = y[3];
        x[5] = x[0];
        y[5] = leftCheekY(face);

        int[][] points = new int[2][6];
        for (int i = 0; i < 6; i++) {
            points[0][i] = (int)x[i];
            points[1][i] = (int)y[i];
        }

        return points;
    }


    float leftEyeX(Face face) {
        return face.getLandmarks().get(0).getPosition().x;
    }
    float leftEyeY(Face face) {
        return face.getLandmarks().get(0).getPosition().y;
    }
    float rightEyeX(Face face) {
        return face.getLandmarks().get(1).getPosition().x;
    }
    float rightEyeY(Face face) {
        return face.getLandmarks().get(1).getPosition().y;
    }
    float noseX(Face face) {
        return face.getLandmarks().get(2).getPosition().x;
    }
    float noseY(Face face) {
        return face.getLandmarks().get(2).getPosition().y;
    }
    float leftCheekX(Face face) {
        return face.getLandmarks().get(4).getPosition().x;
    }
    float leftCheekY(Face face) {
        return face.getLandmarks().get(4).getPosition().y;
    }
    float rightCheekX(Face face) {
        return face.getLandmarks().get(3).getPosition().x;
    }

    float rightCheekY(Face face) {
        return face.getLandmarks().get(3).getPosition().y;
    }
    float leftMouthX(Face face) {
        return face.getLandmarks().get(6).getPosition().x;
    }
    float leftMouthY(Face face) {
        return face.getLandmarks().get(6).getPosition().y;
    }
    float rightMouthX(Face face) {
        return face.getLandmarks().get(5).getPosition().x;
    }
    float rightMouthY(Face face) {
        return face.getLandmarks().get(5).getPosition().y;
    }
    float downMouthX(Face face) {
        return face.getLandmarks().get(7).getPosition().x;
    }
    float downMouthY(Face face) {
        return face.getLandmarks().get(7).getPosition().y;
    }
    float leftMouthBorder(Face face) {
        float k = 0.1f;
        return leftMouthX(face) - k *(rightMouthX(face) - leftMouthX(face));
    }
    float MouthWidth(Face face) {
        float k = 0.1f;
        return (1 + 2 * k) * (rightMouthX(face) - leftMouthX(face));
    }
    float  k3 = 0.15f;
    float topMouthBorder(Face face) {
        return eyeHeight(face) + topEyeBorder(face);
    }
    float mouthHeight(Face face) {
        float k = 1f;
        return k * (downMouthY(face) - leftEyeY(face));
    }
    float leftEyeBorder(Face face) {
        float k = 0.38f;
        return leftEyeX(face) - k *(rightEyeX(face) - leftEyeX(face));
    }
    float eyeWidth(Face face) {
        float k = 0.38f;
        return (1 + 2 * k) * (rightEyeX(face) - leftEyeX(face));
    }
    float topEyeBorder(Face face) {
        float k = 0.7f;
        return leftEyeY(face) - k * (noseY(face) - leftEyeY(face));
    }
    float eyeHeight(Face face) {
        float k = 0.98f;
        return k * (noseY(face) - leftEyeY(face));
    }

    void deleteEyes(Bitmap bitmap, Face face) {
        int refX = (int)leftCheekX(face);
        int refY = (int)leftCheekY(face);
        int refSize = 50;
        double threshold = 50;
        wipePixels(bitmap, (int) leftEyeBorder(face), (int) topEyeBorder(face), (int) eyeWidth(face), (int) eyeHeight(face), refX - refSize / 2, refY - refSize / 2, refSize, refSize, threshold);
    }

    void swapEyes(Bitmap bmp, Face face1, Face face2) {
        int eyeLeft1 = (int)leftEyeBorder(face1);
        int eyeTop1 = (int)topEyeBorder(face1);
        int eyeWidth1 = (int)eyeWidth(face1);
        int eyeHeight1 = (int)eyeHeight(face1);

        int eyeLeft2 = (int)leftEyeBorder(face2);
        int eyeTop2 = (int)topEyeBorder(face2);
        int eyeWidth2 = (int)eyeWidth(face2);
        int eyeHeight2 = (int)eyeHeight(face2);

        int leftEyeX = (int)((leftEyeX(face1) - eyeLeft1) / eyeWidth1 * eyeWidth2);
        int rightEyeX = (int)((rightEyeX(face1) - eyeLeft1) / eyeWidth1 * eyeWidth2);

        // 2d double gaussian weighting
        float[] gauss1 = gaussian(eyeWidth2, leftEyeX, eyeWidth2 / 10);
        float[] gauss2 = gaussian(eyeWidth2, rightEyeX, eyeWidth2 / 10);
        float[] gauss3 = gaussian(eyeHeight2, eyeHeight2 / 2, eyeHeight2 / 6);
        float[][] gauss2d1 = multiplyArrays(addArrays(gauss1, gauss2, eyeWidth2), eyeWidth2, gauss3, eyeHeight2);

        // 2d double gaussian weighting
        gauss1 = gaussian(eyeWidth1, leftEyeX, eyeWidth1 / 10);
        gauss2 = gaussian(eyeWidth1, rightEyeX, eyeWidth1 / 10);
        gauss3 = gaussian(eyeHeight1, eyeHeight1 / 2, eyeHeight1 / 6);
        float[][] gauss2d2 = multiplyArrays(addArrays(gauss1, gauss2, eyeWidth1), eyeWidth1, gauss3, eyeHeight1);

        swapPixels(bmp, eyeLeft1, eyeTop1, eyeWidth1, eyeHeight1, eyeLeft2, eyeTop2, eyeWidth2, eyeHeight2, gauss2d1, gauss2d1, 0.7f);
    }

    void swapFace(Bitmap bmp, Face face1, Face face2) {
        int[][] points1 = getFacePoints(face1);
        int[][] points2 = getFacePoints(face2);

        int x1 = points1[0][0];
        int y1 = points1[1][0];
        int width1 = points1[0][1] - x1;
        int height1 = points1[1][4] - y1;

        int x2 = points2[0][0];
        int y2 = points2[1][0];
        int width2 = points2[0][1] - x2;
        int height2 = points2[1][4] - y2;

        float[][] weights1 = gauss2d(width1, height1, width1 * 0.28f, height1 * 0.28f);
        float[][] weights2 = gauss2d(width2, height2, width2 * 0.28f, height2 * 0.28f);

        swapPixels(bmp, x1, y1, width1, height1, x2, y2, width2, height2, weights1, weights2, 0.11f);
    }

    void swapFace3Way(Bitmap bmp, Face face1, Face face2, Face face3) {
        int[][] points1 = getFacePoints(face1);
        int[][] points2 = getFacePoints(face2);
        int[][] points3 = getFacePoints(face3);

        int x1 = points1[0][0];
        int y1 = points1[1][0];
        int width1 = points1[0][1] - x1;
        int height1 = points1[1][4] - y1;

        int x2 = points2[0][0];
        int y2 = points2[1][0];
        int width2 = points2[0][1] - x2;
        int height2 = points2[1][4] - y2;

        int x3 = points3[0][0];
        int y3 = points3[1][0];
        int width3 = points3[0][1] - x3;
        int height3 = points3[1][4] - y3;

        float[][] weights1 = gauss2d(width1, height1, width1 * 0.28f, height1 * 0.28f);
        float[][] weights2 = gauss2d(width2, height2, width2 * 0.28f, height2 * 0.28f);
        float[][] weights3 = gauss2d(width3, height3, width3 * 0.28f, height3 * 0.28f);

        String tag = "ModifiedFaceView";
        Log.d(tag, "height1 = " + height1 + ", width1 = " + width1);
        Log.d(tag, "height2 = " + height2 + ", width2 = " + width2);
        Log.d(tag, "height3 = " + height3 + ", width3 = " + width3);

        swapPixels3Way(bmp, x1, y1, width1, height1, x2, y2, width2, height2, x3, y3, width3, height3, weights1, weights2, weights3, 0.11f);
    }

    boolean isColorEqual(int color1, int color2, double threshold) {
        double distSquared = Math.pow(Color.red(color1) - Color.red(color2), 2) +
                Math.pow(Color.green(color1) - Color.green(color2), 2) +
                Math.pow(Color.blue(color1) - Color.blue(color2), 2);

        return Math.sqrt(distSquared) < threshold;
    }

    void wipePixels(Bitmap bmp, int x, int y, int width, int height, int refX, int refY, int refWidth, int refHeight, double threshold) {
        for (int i = 0; i < width; i++) {
            int iRef = refX + (i % refWidth);
            for (int j = 0; j < height; j++) {
                int jRef = refY + (j % refHeight);
                if (!isColorEqual(bmp.getPixel(x + i, y + j), bmp.getPixel(iRef, jRef), threshold)) {
                    bmp.setPixel(x + i, y + j, bmp.getPixel(iRef, jRef));
                }
            }
        }
    }

    void swapPixels(Bitmap bmp, int x1, int y1, int width1, int height1, int x2, int y2, int width2, int height2, float[][] weight1, float[][] weight2, float threshold) {
        Bitmap subBmp1 = Bitmap.createBitmap(bmp, x1, y1, width1, height1);
        subBmp1 = Bitmap.createScaledBitmap(subBmp1, width2, height2, true);

        Bitmap subBmp2 = Bitmap.createBitmap(bmp, x2, y2, width2, height2);
        subBmp2 = Bitmap.createScaledBitmap(subBmp2, width1, height1, true);

        for (int x = 0; x < width1; x++) {
            for (int y = 0; y < height1; y++) {
                int color1 = bmp.getPixel(x + x1, y + y1);
                int color2 = subBmp2.getPixel(x, y);
                if (weight1[x][y] < threshold)
                    bmp.setPixel(x + x1, y + y1, combineColors(color2, weight1[x][y], color1, (1-weight1[x][y])));
                else
                    bmp.setPixel(x + x1, y + y1, subBmp2.getPixel(x, y));
            }
        }

        for (int x = 0; x < width2; x++) {
            for (int y = 0; y < height2; y++) {
                int color2 = bmp.getPixel(x + x2, y + y2);
                int color1 = subBmp1.getPixel(x, y);
                if (weight2[x][y] < threshold)
                    bmp.setPixel(x + x2, y + y2, combineColors(color1, weight2[x][y], color2, (1-weight2[x][y])));
                else
                    bmp.setPixel(x + x2, y + y2, subBmp1.getPixel(x, y));
            }
        }
    }

    void swapPixels3Way(Bitmap bmp, int x1, int y1, int width1, int height1, int x2, int y2, int width2, int height2, int x3, int y3, int width3, int height3, float[][] weight1, float[][] weight2, float[][] weight3, float threshold) {
        Bitmap subBmp1 = Bitmap.createBitmap(bmp, x1, y1, width1, height1);
        subBmp1 = Bitmap.createScaledBitmap(subBmp1, width2, height2, true);

        Bitmap subBmp2 = Bitmap.createBitmap(bmp, x2, y2, width2, height2);
        subBmp2 = Bitmap.createScaledBitmap(subBmp2, width3, height3, true);

        Bitmap subBmp3 = Bitmap.createBitmap(bmp, x3, y3, width3, height3);
        subBmp3 = Bitmap.createScaledBitmap(subBmp3, width1, height1, true);

        for (int x = 0; x < width1; x++) {
            for (int y = 0; y < height1; y++) {
                int color1 = bmp.getPixel(x + x1, y + y1);
                int color2 = subBmp2.getPixel(x, y);
                if (weight1[x][y] < threshold)
                    bmp.setPixel(x + x1, y + y1, combineColors(color2, weight1[x][y], color1, (1-weight1[x][y])));
                else
                    bmp.setPixel(x + x1, y + y1, subBmp2.getPixel(x, y));
            }
        }

        for (int x = 0; x < width2; x++) {
            for (int y = 0; y < height2; y++) {
                int color2 = bmp.getPixel(x + x2, y + y2);
                int color3 = subBmp3.getPixel(x, y);
                if (weight2[x][y] < threshold)
                    bmp.setPixel(x + x2, y + y2, combineColors(color3, weight2[x][y], color2, (2-weight2[x][y])));
                else
                    bmp.setPixel(x + x2, y + y2, color3);
            }
        }

        for (int x = 0; x < width3; x++) {
            for (int y = 0; y < height3; y++) {
                int color3 = bmp.getPixel(x + x3, y + y3);
                int color1 = subBmp1.getPixel(x, y);
                if (weight3[x][y] < threshold)
                    bmp.setPixel(x + x3, y + y3, combineColors(color1, weight3[x][y], color3, (1-weight3[x][y])));
                else
                    bmp.setPixel(x + x3, y + y3, color1);
            }
        }

        String tag = "ModifiedFaceView";
        Log.d(tag, "height1 = " + height1 + ", width1 = " + width1);
        Log.d(tag, "height2 = " + height2 + ", width2 = " + width2);
        Log.d(tag, "height3 = " + height3 + ", width3 = " + width3);
    }

    float[][] gauss2d(int width, int height, float sigmaX, float sigmaY) {
        float[] gaussX = gaussian(width, width / 2, sigmaX);
        float[] gaussY = gaussian(height, height / 2, sigmaY);
        return multiplyArrays(gaussX, width, gaussY, height);
    }

    float[] gaussian(int size, int mean, float sigma) {
        float[] array = new float[size];
        for (int i = 0; i < size; i++) {
            array[i] = (float)Math.exp(-Math.pow(mean - i, 2) / (2*Math.pow(sigma, 2)));
        }
        return array;
    }

    float[] addArrays(float[] array1, float[] array2, int size) {
        float[] newArray = new float[size];
        for (int i = 0; i < size; i++) {
            newArray[i] = array1[i] + array2[i];
        }
        return newArray;
    }

    float[][] multiplyArrays(float[] arrayX, int sizeX, float[] arrayY, int sizeY) {
        float[][] newArray = new float[sizeX][sizeY];
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                newArray[i][j] = arrayX[i] * arrayY[j];
            }
        }
        return newArray;
    }


    int combineColors(int color1, float weight1, int color2, float weight2) {
        int red = (int) ((Color.red(color1) * weight1 + Color.red(color2) * weight2) / (weight1+weight2));
        int green = (int) ((Color.green(color1) * weight1 + Color.green(color2) * weight2) / (weight1+weight2));
        int blue = (int)((Color.blue(color1) * weight1 + Color.blue(color2) * weight2) / (weight1+weight2));

        return Color.rgb(red, green, blue);
    }

    /************************************************************************/
    // for zooming and dragging                                             //
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        /*switch (ev.getAction() & MotionEvent.ACTION_MASK) {
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
        }*/

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

        canvas.translate(translateX / scaleFactor, translateY / scaleFactor);

        if (bmp != null && faces != null) {
            double scale = drawBitmap(canvas);
            //drawFaceAnnotations(canvas, scale);
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
        double imageWidth = bmp.getWidth();
        double imageHeight = bmp.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale ), (int)(imageHeight  * scale ));
        canvas.drawBitmap(bmp, null, destBounds, null);
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

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);
            }
        }
    }

    public void setBitmap(Bitmap bmp) {
        this.bmp = convertToMutable(bmp);
    }

    public void setFaces(SparseArray<Face> faces) {
        this.faces = faces;
    }

    public void setData(ModifiedFaceView modifiedFaceView) {
        bmp = modifiedFaceView.bmp;
        faces = modifiedFaceView.faces;
    }

    @Override
    public void run() {
        try {
            if (faces.size() == 3) {
                swapFace(bmp, faces.valueAt(0), faces.valueAt(1));
                swapFace(bmp, faces.valueAt(1), faces.valueAt(2));
                //swapFace3Way(bmp, faces.valueAt(0), faces.valueAt(1), faces.valueAt(2));
                //swapFace(bmp, faces.valueAt(0), faces.valueAt(1));
            }
            else {
                swapFace(bmp, faces.valueAt(0), faces.valueAt(1));
            }
        }
        catch (Exception e) {
            Log.d("thing", e.getMessage());
        }

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
}





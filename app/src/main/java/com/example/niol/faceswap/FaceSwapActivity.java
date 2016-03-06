package com.example.niol.faceswap;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

public class FaceSwapActivity extends AppCompatActivity {
    private static final String TAG = "faceSwapActivity";
    static final int PICK_FACE_IMAGE_REQUEST = 1;
    static final int PICK_FACE_IMAGE_2_REQUEST = 2;
    private Bitmap facephoto, facephoto2;
    private Detector<Face> safeDetector;
    private FaceDetector detector;
    private SparseArray<Face> faces, faces2;
    private FaceView faceView1, faceView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_swap);
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        ((FaceSwapApplication)getApplication()).setModifiedFaceView((ModifiedFaceView)findViewById(R.id.modifiedView));

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.

        /*****************************************************************************/
        // temporary init, make test easier
        safeDetector = new SafeFaceDetector(detector);
        final Button buttonFacePhoto = (Button) findViewById(R.id.buttonfacePhoto);
        faceView1 = (FaceView) findViewById(R.id.faceView);
        faceView2 = (FaceView) findViewById(R.id.faceView2);
        Uri uri = Uri.parse("content://media/external/images/media/881");
        Uri uri2 = Uri.parse("content://media/external/images/media/880");;
        try {
            facephoto = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            facephoto2 = MediaStore.Images.Media.getBitmap(getContentResolver(), uri2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Frame frame = new Frame.Builder().setBitmap(facephoto).build();
        faces = safeDetector.detect(frame);
        faceView1.setContent(facephoto, faces);
        Frame frame2 = new Frame.Builder().setBitmap(facephoto2).build();
        faces2 = safeDetector.detect(frame2);
        faceView2.setContent(facephoto2, faces2);
        /*****************************************************************************/

        buttonFacePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture(PICK_FACE_IMAGE_REQUEST);
            }
        });
        final Button buttonFacePhoto2 = (Button) findViewById(R.id.buttonfacePhoto2);
        buttonFacePhoto2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture(PICK_FACE_IMAGE_2_REQUEST);
            }
        });
        final Button buttonGenerateModifiedPhoto = (Button) findViewById(R.id.buttongeneratemodifiedphoto);
        buttonGenerateModifiedPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (facephoto != null && facephoto2 != null) {
                    if (faces.size() != 0 && faces2.size() != 0) {
                        modifyPhoto();
                        Intent intent = new Intent(getApplicationContext(), ViewFaceSwapActivity.class);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(FaceSwapActivity.this, "Error: No faces detected",
                                Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(FaceSwapActivity.this, "Error: Pictures not selected",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void modifyPhoto() {
        if (facephoto != null && facephoto2 != null && faces.size() != 0 && faces2.size() != 0) {
            ((FaceSwapApplication) getApplication()).getModifiedFaceView().getPhotosAndFaces(facephoto, faces, facephoto2, faces2);
            ((FaceSwapApplication) getApplication()).getModifiedFaceView().run();
        }
    }

    private void selectPicture(int imageRequest){
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        startActivityForResult(getIntent, imageRequest);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case PICK_FACE_IMAGE_REQUEST:
                if (resultCode == RESULT_OK && imageReturnedIntent != null && imageReturnedIntent.getData() != null) {

                    Uri uri = imageReturnedIntent.getData();
                    getContentResolver();
                    try {
                        facephoto = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                        // Log.d(TAG, String.valueOf(bitmap));

//                        FaceView faceView1 = (FaceView) findViewById(R.id.faceView);
                        Frame frame = new Frame.Builder().setBitmap(facephoto).build();
                        faces = safeDetector.detect(frame);
                        faceView1.setContent(facephoto, faces);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if (!safeDetector.isOperational()) {
                    // Note: The first time that an app using face API is installed on a device, GMS will
                    // download a native library to the device in order to do detection.  Usually this
                    // completes before the app is run for the first time.  But if that download has not yet
                    // completed, then the above call will not detect any faces.
                    //
                    // isOperational() can be used to check if the required native library is currently
                    // available.  The detector will automatically become operational once the library
                    // download completes on device.
                    Log.w(TAG, "Face detector dependencies are not yet available.");

                    // Check for low storage.  If there is low storage, the native library will not be
                    // downloaded, so detection will not become operational.
                    IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                    boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                    if (hasLowStorage) {
                        Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                        Log.w(TAG, getString(R.string.low_storage_error));
                    }
                }
                break;
            case PICK_FACE_IMAGE_2_REQUEST:
                if (resultCode == RESULT_OK && imageReturnedIntent != null && imageReturnedIntent.getData() != null) {

                    Uri uri = imageReturnedIntent.getData();
                    Log.d("linkpath", uri.toString());

                    getContentResolver();
                    try {
                        facephoto2 = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                        // Log.d(TAG, String.valueOf(bitmap));

//                        FaceView faceView2 = (FaceView) findViewById(R.id.faceView2);
                        Frame frame = new Frame.Builder().setBitmap(facephoto2).build();
                        faces2 = safeDetector.detect(frame);
                        faceView2.setContent(facephoto2, faces2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                if (!safeDetector.isOperational()) {
                    // Note: The first time that an app using face API is installed on a device, GMS will
                    // download a native library to the device in order to do detection.  Usually this
                    // completes before the app is run for the first time.  But if that download has not yet
                    // completed, then the above call will not detect any faces.
                    //
                    // isOperational() can be used to check if the required native library is currently
                    // available.  The detector will automatically become operational once the library
                    // download completes on device.
                    Log.w(TAG, "Face detector dependencies are not yet available.");

                    // Check for low storage.  If there is low storage, the native library will not be
                    // downloaded, so detection will not become operational.
                    IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                    boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

                    if (hasLowStorage) {
                        Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                        Log.w(TAG, getString(R.string.low_storage_error));
                    }
                }
                /*if(faces2.size()!=0)
                    Toast.makeText(this,faces2.toString() , Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this,"no face", Toast.LENGTH_SHORT).show();*/
                break;
        }
    }
}

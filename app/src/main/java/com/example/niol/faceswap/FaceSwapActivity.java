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
import android.view.Window;
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
    private Bitmap bmp;
    private Detector<Face> safeDetector;
    private FaceDetector detector;
    private SparseArray<Face> faces;
    private FaceView faceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_swap);
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        ((FaceSwapApplication)getApplication()).setModifiedFaceView((ModifiedFaceView)findViewById(R.id.modifiedView));

        // temporary init, make test easier
        safeDetector = new SafeFaceDetector(detector);
        final Button buttonFacePhoto = (Button) findViewById(R.id.buttonfacePhoto);
        faceView = (FaceView) findViewById(R.id.faceView);

        buttonFacePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPicture(PICK_FACE_IMAGE_REQUEST);
            }
        });

        final Button buttonGenerateModifiedPhoto = (Button) findViewById(R.id.buttongeneratemodifiedphoto);
        buttonGenerateModifiedPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bmp != null) {
                    if (faces.size() >= 2) {
                        modifyPhoto();
                        Intent intent = new Intent(getApplicationContext(), ViewFaceSwapActivity.class);
                        startActivity(intent);
                    } else {
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
        if (bmp != null && faces.size() != 0) {
            ((FaceSwapApplication) getApplication()).getModifiedFaceView().setBitmap(bmp);
            ((FaceSwapApplication) getApplication()).getModifiedFaceView().setFaces(faces);
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

        if (requestCode == PICK_FACE_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK && imageReturnedIntent != null && imageReturnedIntent.getData() != null) {

                Uri uri = imageReturnedIntent.getData();
                getContentResolver();
                try {
                    bmp = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
                    Frame frame = new Frame.Builder().setBitmap(bmp).build();
                    faces = safeDetector.detect(frame);
                    faceView.setContent(bmp, faces);
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
        }
    }
}

package com.example.niol.faceswap;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class FaceSwapActivity extends AppCompatActivity {
    static final int PICK_FACE_IMAGE_REQUEST = 1;
    static final int PICK_FACE_IMAGE_2_REQUEST = 2;
    private Bitmap facephoto, facephoto2, modifiedphoto;
    Detector<Face>  safeDetector;
    //ModifiedFaceView modifiedFaceView;
    SparseArray<Face> faces, faces2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_swap);
        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        safeDetector = new SafeFaceDetector(detector);
        final Button buttonFacePhoto = (Button) findViewById(R.id.buttonfacePhoto);
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
                if(facephoto!=null && facephoto2 != null && faces != null && faces2 != null){
                    modifiedPhoto();
                }else{
                    Toast.makeText(FaceSwapActivity.this, "Error:there is no two photos or no face in both two photos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void selectPicture(int imageRequest){
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, imageRequest);
    }
    private void modifiedPhoto(){
        ModifiedFaceView modifiedFaceView = (ModifiedFaceView)findViewById(R.id.modifiedView);
        modifiedFaceView.setContent(facephoto,faces,facephoto2,faces2);
        // Although detector may be used multiple times for different images, it should be released
        // when it is no longer needed in order to free native resources.
        //safeDetector.release();
    }
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case PICK_FACE_IMAGE_REQUEST:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    facephoto = BitmapFactory.decodeFile(filePath);
                    FaceView faceView = (FaceView) findViewById(R.id.faceView);
                    Frame frame = new Frame.Builder().setBitmap(facephoto).build();
                    faces = safeDetector.detect(frame);
                    faceView.setContent(facephoto);
                }
                break;
            case PICK_FACE_IMAGE_2_REQUEST:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    facephoto2 = BitmapFactory.decodeFile(filePath);
                    FaceView faceView2 = (FaceView) findViewById(R.id.faceView2);
                    Frame frame = new Frame.Builder().setBitmap(facephoto2).build();
                    faces2 = safeDetector.detect(frame);
                    faceView2.setContent(facephoto2);
                }
        }
    }
}

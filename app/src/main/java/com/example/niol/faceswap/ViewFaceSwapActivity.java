package com.example.niol.faceswap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class ViewFaceSwapActivity extends AppCompatActivity {

    private ModifiedFaceView modifiedFaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_face_swap);

        modifiedFaceView = (ModifiedFaceView)findViewById(R.id.modifiedView);
        modifiedFaceView.setData(((FaceSwapApplication)getApplication()).getModifiedFaceView());
    }



}

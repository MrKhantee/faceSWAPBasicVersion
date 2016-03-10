package com.example.niol.faceswap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class ViewFaceSwapActivity extends AppCompatActivity {

    private ModifiedFaceView modifiedFaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_face_swap);

        modifiedFaceView = (ModifiedFaceView)findViewById(R.id.modifiedView);
        modifiedFaceView.setData(((FaceSwapApplication)getApplication()).getModifiedFaceView());

        Button button_transfer = (Button) findViewById(R.id.button_transfer);
        button_transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPic(v);
            }
        });

    }
    private void sendPic (View v) {
        Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);
    }



}

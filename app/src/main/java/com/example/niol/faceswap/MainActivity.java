package com.example.niol.faceswap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    String mCurrentPhotoPath = "/storage/sdcard0/Pictures/Gallery" ;
    static final int REQUEST_TAKE_PHOTO = 1;
    //static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        final Button button_take_picture = (Button) findViewById(R.id.take_picture);
        final Button button_transfer_picture = (Button) findViewById(R.id.transfer_picture);
        final Button button_face_swap = (Button) findViewById(R.id.face_swap);

        button_take_picture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                dispatchTakePictureIntent();
            }
        });
        button_transfer_picture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                sendPicture(v);
            }
        });
        button_face_swap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                selectPicture(v);
            }
        });
    }

    private void dispatchTakePictureIntent() {
        /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(MainActivity.this, "Error occurred while creating the File",
                        Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

                Toast toast = Toast.makeText(MainActivity.this, photoFile.toString(),  Toast.LENGTH_SHORT);
                toast.show();
            }
        }*/
        Intent intent = new Intent(this, FaceTrackerActivity.class);
        startActivity(intent);
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "1mind_" + timeStamp + ".jpg";
        File photo = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/faceswap",  imageFileName);
        return photo;
    }
    public void selectPicture(View view) {
        Intent intent = new Intent(this, FaceSwapActivity.class);
        //maybe record the picture now?
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);

    }
    public void sendPicture(View view) {
        Intent intent = new Intent(this, WiFiDirectActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
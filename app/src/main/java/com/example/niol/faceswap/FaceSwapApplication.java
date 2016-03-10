package com.example.niol.faceswap;

import android.app.Application;
import android.graphics.Bitmap;

/**
 * Created by Devin on 3/5/16.
 */
public class FaceSwapApplication extends Application {
    private ModifiedFaceView modifiedFaceView;

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    private String imagePath;

    public ModifiedFaceView getModifiedFaceView()
    {
        return modifiedFaceView;
    }

    public void setModifiedFaceView(ModifiedFaceView modifiedFaceView)
    {
        this.modifiedFaceView = modifiedFaceView;
    }
}

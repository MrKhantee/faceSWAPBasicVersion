package com.example.niol.faceswap;

import android.app.Application;

/**
 * Created by Devin on 3/5/16.
 */
public class FaceSwapApplication extends Application {
    private ModifiedFaceView modifiedFaceView;

    public ModifiedFaceView getModifiedFaceView()
    {
        return modifiedFaceView;
    }

    public void setModifiedFaceView(ModifiedFaceView modifiedFaceView)
    {
        this.modifiedFaceView = modifiedFaceView;
    }
}

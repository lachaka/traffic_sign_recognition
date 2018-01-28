package com.speedcam;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getName();

    JavaCameraView cameraView;
    Mat frame;

    static {
        System.loadLibrary("opencv");
    }

    BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == BaseLoaderCallback.SUCCESS) {
                cameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }
/*
    private void saveModelsOnInternalStorage(String filename) {
        AssetManager assetManager = getAssets();
        try {
            InputStream in = assetManager.open(filename);
            OutputStream out;
            String outDir = Environment.getExternalStorageDirectory().getAbsolutePath();

            File outFile = new File(outDir, filename);

            out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            out.close();
        } catch(IOException e) {
            Log.e(TAG, "Failed to copy asset file: " + filename, e);
        }
    }


    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.e(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,
                    this, loaderCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        OpenCVDetection.signDetection(frame.getNativeObjAddr());
        return frame;
    }
}

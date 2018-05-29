package com.speedcam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.speedcam.Constants.COLOR_CHANNELS;
import static com.speedcam.Constants.IMAGE_SIZE;
import static com.speedcam.Constants.INPUT_NODE;
import static com.speedcam.Constants.MIN_DISTANCE_UPDATE;
import static com.speedcam.Constants.MIN_TIME_UPDATE;
import static com.speedcam.Constants.OUTPUT_NODE;
import static com.speedcam.Constants.SPEED_UNITS;
import static com.speedcam.Constants.TIME_POP_SIGN_LISTIVEW;


public class HomeActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, LocationListener {

    private static final String TAG = HomeActivity.class.getName();

    private TensorFlowInferenceInterface signClassifier;
    private CascadeClassifier cascadeClassifier;
    private File mCascadeFile;

    private ListView signView;
    private JavaCameraView cameraView;
    private Mat frame;

    private HashMap<Integer, Integer> signImages;
    private ArrayList<Integer> signList;
    private SignAdapter signAdapter;

    private Handler handler = new Handler(Looper.getMainLooper());

    BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == BaseLoaderCallback.SUCCESS) {
                cameraView.enableView();
                loadCascadeClassifier();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    private Runnable runnable = new Runnable() {
        public void run() {
            if (signList.size() > 2) {
                signList.remove(signList.size() - 1);
                signAdapter.notifyDataSetChanged();
            }
            handler.postDelayed(this, TIME_POP_SIGN_LISTIVEW);
        }
    };


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        signList = new ArrayList<>();

        signView = findViewById(R.id.signList);
        signAdapter = new SignAdapter(HomeActivity.this, signList);
        signView.setAdapter(signAdapter);

        signClassifier = new TensorFlowInferenceInterface(getAssets(), Constants.TENSORFLOW_MODEL_FILE);

        initSignImages();
        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                        MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this);
        this.updateSpeed(0);
        handler.postDelayed(runnable, TIME_POP_SIGN_LISTIVEW);
    }


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
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,
                    this, loaderCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
        signClassifier.close();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        frame = new Mat(height, width, CvType.CV_32FC3);
    }

    @Override
    public void onCameraViewStopped() {
        frame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frame = inputFrame.rgba();
        new SignRecognition().execute(frame);

        return frame;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            this.updateSpeed(location.getSpeed());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    private void updateSpeed(float currentSpeed) {
        TextView speedView = this.findViewById(R.id.speed_view);
        speedView.setText((int)currentSpeed + " " + SPEED_UNITS);
    }

    private class SignRecognition extends AsyncTask<Object, Void, Set<Integer>> {
        @Override
        protected Set doInBackground(Object[] objects) {
            Mat frame = (Mat) objects[0];

            MatOfRect detectedObjects = new MatOfRect();
            cascadeClassifier.detectMultiScale(frame, detectedObjects);

            Set<Integer> detectedSigns = new HashSet<>();
            for (Rect object : detectedObjects.toArray()) {
                Rect objectCoordinates = new Rect(object.x, object.y, object.width, object.height);
                Mat croppedObject = new Mat(frame, objectCoordinates);

                Mat resizedImg = new Mat(IMAGE_SIZE, IMAGE_SIZE, CvType.CV_32FC3);
                Imgproc.resize(croppedObject, resizedImg, new Size(IMAGE_SIZE, IMAGE_SIZE));

                float[] floatArrImg = convertMatToFloatArray(resizedImg);

                int prediction = predictSign(floatArrImg);
                detectedSigns.add(prediction);
            }

            return detectedSigns;
        }

        @Override
        protected void onPostExecute(Set<Integer> detectedSigns) {
            if (detectedSigns.size() != 0) {
                for (Integer detectedSign : detectedSigns) {
                    if (detectedSign != 44 && !signList.contains(signImages.get(detectedSign))) {
                        signList.add(0, signImages.get(detectedSign));
                    }
                }
                signAdapter.notifyDataSetChanged();
            }
        }

        private float[] convertMatToFloatArray(Mat image) {
            float[] floatImage = new float[image.rows() * image.cols() * 3];
            int index = 0;
            for (int i = 0; i < image.rows(); i++) {
                for (int j = 0; j < image.cols(); j++) {
                    floatImage[index] = (float) ((image.get(i, j)[0]) / 255.0);
                    floatImage[index + 1] = (float) (image.get(i, j)[1] / 255.0);
                    floatImage[index + 2] = (float) (image.get(i, j)[2] / 255.0);
                    index += 3;
                }
            }
            return floatImage;
        }

        private int predictSign(float[] floatArrImg) {
            long[] outputResult = {0, 0};

            signClassifier.feed(INPUT_NODE, floatArrImg, 1,
                    IMAGE_SIZE, IMAGE_SIZE, COLOR_CHANNELS);
            signClassifier.run(new String[] { OUTPUT_NODE }, false);
            signClassifier.fetch("prediction", outputResult);
            return (int) outputResult[0];
        }
    }

    private void saveCascadeFile() {
        final InputStream is;
        FileOutputStream os;
        try {
            is = getResources().getAssets().open(Constants.CASCADE_FILE_NAME);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, Constants.CASCADE_FILE_NAME);

            os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();
        } catch (IOException e) {
            Log.e(TAG, "Cascade classifier not found");
        }
    }

    private void loadCascadeClassifier() {
        saveCascadeFile();
        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
    }

    private void initSignImages() {
        signImages = new HashMap<>();

        int count = 0;
        for (Field field : R.drawable.class.getFields()) {
            if (field.getName().contains("sign_number")) {
                try {
                    field.setAccessible(true);
                    signImages.put(count++, field.getInt(R.drawable.class));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

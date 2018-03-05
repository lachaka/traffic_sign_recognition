package com.speedcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.speedcam.Constants.COLOR_CHANNELS;
import static com.speedcam.Constants.IMAGE_HEIGHT;
import static com.speedcam.Constants.IMAGE_WIDTH;
import static com.speedcam.Constants.INPUT_NODE;
import static com.speedcam.Constants.OUTPUT_NODE;


public class HomeActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = HomeActivity.class.getName();

    private LoadSignClassifier signClassifier;
    private CascadeClassifier cascadeClassifier;

    private ImageView signView;
    private JavaCameraView cameraView;

    private Mat frame;
    private File mCascadeFile;

    private HashMap<Long, Integer> signImages;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        signClassifier = new LoadSignClassifier(getAssets());
        initSignImages();
        signView = findViewById(R.id.sign_view);
        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);

        cameraView.setCvCameraViewListener(this);
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
        new ObjectDetection().execute(cascadeClassifier, frame);
        return frame;
    }

    private class ObjectDetection extends AsyncTask<Object, Void, List<Long>> {
        @Override
        protected List doInBackground(Object[] objects) {
            CascadeClassifier cascadeClassifier = (CascadeClassifier) objects[0];
            Mat frame = (Mat) objects[1];

            MatOfRect detectedObjects = new MatOfRect();
            Mat frameGray = new Mat();
            Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGRA2GRAY);

            cascadeClassifier.detectMultiScale(frameGray, detectedObjects);
            List<Long> detectedSigns = new ArrayList<>();
            for (Rect object : detectedObjects.toArray()) {
                Rect objectCoordinates = new Rect(object.x, object.y, object.width, object.height);
                Mat croppedObject = new Mat(frame, objectCoordinates);

                Mat resizedImg = new Mat(IMAGE_WIDTH, IMAGE_HEIGHT, CvType.CV_32FC4);
                Imgproc.resize(croppedObject, resizedImg, new Size(IMAGE_WIDTH, IMAGE_HEIGHT));

                float[] floatArrImg = convertMatToFloatArray(resizedImg);

                long[] outputResult = {0, 0};
                signClassifier.getTensorFlow().feed(INPUT_NODE, floatArrImg, 1,
                        IMAGE_WIDTH, IMAGE_HEIGHT, COLOR_CHANNELS);
                signClassifier.getTensorFlow().run(new String[]{OUTPUT_NODE}, false);
                signClassifier.getTensorFlow().fetch("prediction", outputResult);
                detectedSigns.add(outputResult[0]);
            }
            return detectedSigns;
        }

        @Override
        protected void onPostExecute(List<Long> detectedSigns) {
            HashSet noDupSet = new HashSet(detectedSigns);

            Log.i("wtf", String.valueOf(noDupSet.size()));
            if (detectedSigns.size() != 0) {
                Log.i("wtf", "begin");
                for (Long detectedSign : detectedSigns) {
                    Log.i("wtf", String.valueOf(detectedSign));
                    //signView.setImageResource(signImages.get(detectedSigns.get(0)));
                }
                Log.i("wtf", "end");
            }
        }

        private float[] convertMatToFloatArray(Mat image) {
            Bitmap bitmap = Bitmap.createBitmap(image.cols(),image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, bitmap);

            int[] intValues = new int[32 * 32];
            float[] result = new float[32 * 32 * 3];

            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                    bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < intValues.length; ++i) {
                int tmp = intValues[i];
                result[i * 3 + 0] = ((tmp >> 16) & 0xFF) / 255.0F;
                result[i * 3 + 1] = ((tmp >> 8) & 0xFF) / 255.0F;
                result[i * 3 + 2] = (tmp & 0xFF) / 255.0F;
            }
            return result;
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

        long count = 0;
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

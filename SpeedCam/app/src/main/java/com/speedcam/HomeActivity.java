package com.speedcam;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ListView;

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
import static com.speedcam.Constants.OUTPUT_NODE;


public class HomeActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = HomeActivity.class.getName();

    private TensorFlowInferenceInterface signClassifier;
    private CascadeClassifier cascadeClassifier;

    private JavaCameraView cameraView;

    private Mat frame;
    private File mCascadeFile;
    ListView signView;
    private HashMap<Integer, Integer> signImages;

    private ArrayList<Integer> signList;
    private SignAdapter signAdapter;

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

        setContentView(R.layout.activity_home);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        signList = new ArrayList<>();

        signView = findViewById(R.id.signList);
        signAdapter = new SignAdapter(HomeActivity.this, signList);
        signView.setAdapter(signAdapter);

        signClassifier = new TensorFlowInferenceInterface(getAssets(), Constants.TENSORFLOW_MODEL_FILE);

        initSignImages();
        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setMaxFrameSize(320,480);
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
      //  Mat croppedFrame = frame.submat(0, frame.cols(), frame.cols() / 2, frame.cols());
        new SignRecognition().execute(cascadeClassifier, frame);
        return frame;
    }

    private class SignRecognition extends AsyncTask<Object, Void, Set<Integer>> {
        @Override
        protected Set doInBackground(Object[] objects) {
            CascadeClassifier cascadeClassifier = (CascadeClassifier) objects[0];
            Mat frame = (Mat) objects[1];

            MatOfRect detectedObjects = new MatOfRect();
            Mat frameGray = new Mat();
            Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGRA2GRAY);

            cascadeClassifier.detectMultiScale(frameGray, detectedObjects);
            Set<Integer> detectedSigns = new HashSet<>();
            for (Rect object : detectedObjects.toArray()) {
                Rect objectCoordinates = new Rect(object.x, object.y, object.width, object.height);
                Mat croppedObject = new Mat(frame, objectCoordinates);

                Mat resizedImg = new Mat(IMAGE_SIZE, IMAGE_SIZE, CvType.CV_32FC4);
                Imgproc.resize(croppedObject, resizedImg, new Size(IMAGE_SIZE, IMAGE_SIZE));

                float[] floatArrImg = convertMatToFloatArray(resizedImg);

                long[] outputResult = {0, 0};
                signClassifier.feed(INPUT_NODE, floatArrImg, 1,
                        IMAGE_SIZE, IMAGE_SIZE, COLOR_CHANNELS);
                signClassifier.run(new String[] { OUTPUT_NODE }, false);
                signClassifier.fetch("prediction", outputResult);
                detectedSigns.add((int) outputResult[0]);
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
            Bitmap bitmap = Bitmap.createBitmap(image.cols(),image.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image, bitmap);

            int[] intValues = new int[32 * 32];
            float[] floatImage = new float[IMAGE_SIZE * IMAGE_SIZE * COLOR_CHANNELS];

            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                    bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < intValues.length; ++i) {
                int tmp = intValues[i];
                floatImage[i * 3 + 0] = ((tmp >> 16) & 0xFF) / 255.0F;
                floatImage[i * 3 + 1] = ((tmp >> 8) & 0xFF) / 255.0F;
                floatImage[i * 3 + 2] = (tmp & 0xFF) / 255.0F;
            }
            return floatImage;
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

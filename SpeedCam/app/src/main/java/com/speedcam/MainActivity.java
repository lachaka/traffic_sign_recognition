package com.speedcam;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static com.speedcam.Constants.CASCADE_FILE_PATH;
import static com.speedcam.Constants.COLOR_CHANNELS;
import static com.speedcam.Constants.IMAGE_HEIGHT;
import static com.speedcam.Constants.IMAGE_WIDTH;
import static com.speedcam.Constants.INPUT_NODE;
import static com.speedcam.Constants.OUTPUT_NODE;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getName();

    private LoadSignClassifier signClassifier;

    private ImageView signView;
    private JavaCameraView cameraView;
    private Mat frame;

    private HashMap<Long, Integer> signImages;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initSignImages();

        signClassifier = new LoadSignClassifier(getAssets());
        signView = findViewById(R.id.sign_view);
        cameraView = findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setMaxFrameSize(320, 240);
        cameraView.setCvCameraViewListener(this);

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

        MatOfRect detectedObjects = new MatOfRect();
        Mat frameGray = new Mat();
        Imgproc.cvtColor(frame, frameGray, Imgproc.COLOR_BGRA2GRAY);
        CascadeClassifier cascadeDetector = new CascadeClassifier(CASCADE_FILE_PATH);

        cascadeDetector.detectMultiScale(frameGray, detectedObjects);

        for (Rect object : detectedObjects.toArray()) {
            Rect objectCoordinates = new Rect(object.x, object.y, object.width, object.height);
            Mat croppedObject = new Mat(frame, objectCoordinates);

            Mat resizedImg = new Mat(IMAGE_WIDTH, IMAGE_HEIGHT, CvType.CV_32FC4);
            Imgproc.resize(croppedObject, resizedImg, new Size(IMAGE_WIDTH, IMAGE_HEIGHT));

            float[] floatArrImg = convertMatToFloatArray(resizedImg);
            try {
                long signNumber = (long) new SignRecognition().execute(signClassifier.getTensorFlow(),
                        floatArrImg).get();
                drawRectangle(object, signNumber);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return frame;
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

    private void drawRectangle(Rect sign, long signNumber) {
        if (signNumber != 43) {
            Imgproc.rectangle(frame, sign.tl(), sign.br(),
                    new Scalar(255, 0, 255), 1);
        }
    }

    public class SignRecognition extends AsyncTask<Object, Void, Long> {

        @Override
        protected Long doInBackground(Object... objects) {
            TensorFlowInferenceInterface tfInterface = (TensorFlowInferenceInterface) objects[0];
            float[] inputData = (float[]) objects[1];

            long[] outputResult = {0, 0};
            tfInterface.feed(INPUT_NODE, inputData, 1,
                    IMAGE_WIDTH, IMAGE_HEIGHT, COLOR_CHANNELS);
            tfInterface.run(new String[]{OUTPUT_NODE}, false);
            tfInterface.fetch("prediction", outputResult);

            return outputResult[0];
        }

        @Override
        protected void onPostExecute(Long signNumber) {
            if (signNumber < 8) { //TODO: add all signs
                signView.setImageResource(signImages.get(signNumber));
            }
        }
    }
}

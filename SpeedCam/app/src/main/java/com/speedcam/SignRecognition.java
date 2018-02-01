package com.speedcam;

import android.os.AsyncTask;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class SignRecognition extends AsyncTask {
    private static final String TAG = SignRecognition.class.getName();

    private static final int COLOR_CHANNELS = 3;
    private static final int IMAGE_WIDTH = 32;
    private static final int IMAGE_HEIGHT = 32;
    private static final String INPUT_NODE = "input";
    private static final String OUTPUT_NODE = "prediction";

    @Override
    protected Object doInBackground(Object... objects) {
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
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        //TODO: show image sign in image view
    }
}

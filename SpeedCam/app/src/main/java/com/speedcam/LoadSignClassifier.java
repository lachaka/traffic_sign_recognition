package com.speedcam;

import android.content.res.AssetManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

final public class LoadSignClassifier {
    private static final String TAG = LoadSignClassifier.class.getName();

    private TensorFlowInferenceInterface tensorFlow;

    public LoadSignClassifier(AssetManager assets) {
        tensorFlow = new TensorFlowInferenceInterface(assets, Constants.TENSORFLOW_MODEL_FILE);
    }

    public TensorFlowInferenceInterface getTensorFlow() {
        return tensorFlow;
    }
}

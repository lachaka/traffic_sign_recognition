package com.speedcam;

import android.content.res.AssetManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

final public class LoadSignClassifier {
    private static final String TAG = LoadSignClassifier.class.getName();

    private static final String MODEL_FILE = "file:///android_asset/frozen_model.pb";

    private TensorFlowInferenceInterface tensorFlow;

    public LoadSignClassifier(AssetManager assets) {
        tensorFlow = new TensorFlowInferenceInterface(assets, MODEL_FILE);
    }

    public TensorFlowInferenceInterface getTensorFlow() {
        return tensorFlow;
    }
}

package com.speedcam;


public final class Constants {
    public static final String CASCADE_FILE_NAME = "cascade.xml";
    public static final String TENSORFLOW_MODEL_FILE = "file:///android_asset/frozen_model.pb";
    public static final int IMAGE_WIDTH = 32;
    public static final int IMAGE_HEIGHT = 32;
    public static final int COLOR_CHANNELS = 3;
    public static final String INPUT_NODE = "images";
    public static final String OUTPUT_NODE = "prediction";
    public static final int NEGATIVE_SIGN = 44;
}

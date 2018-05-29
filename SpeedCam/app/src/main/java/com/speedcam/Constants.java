package com.speedcam;


public final class Constants {
    public static final String CASCADE_FILE_NAME = "cascade.xml";                                   // Opencv trained cascade file location
    public static final String TENSORFLOW_MODEL_FILE = "file:///android_asset/frozen_model.pb";     // Neural network trained model file location
    public static final int IMAGE_SIZE = 32;
    public static final int COLOR_CHANNELS = 3;
    public static final String INPUT_NODE = "images";           // Neural network input node
    public static final String OUTPUT_NODE = "prediction";      // Neural network output node
    public static final int NEGATIVE_SIGN = 44;                 // Trained model false class
    public static final float MIN_DISTANCE_UPDATE = 1;          // Minimum distance in meters for GPS updates
    public static final long MIN_TIME_UPDATE = 1000 * 1;        //GPS minimum time in seconds between GPS updates
    public static final String SPEED_UNITS = "km/h";
    public static final long TIME_POP_SIGN_LISTIVEW = 5000;     // Time for removing the last sign from the listview
}

    LOCAL_PATH := $(call my-dir)

    include $(CLEAR_VARS)

    #opencv
    OPENCVROOT:= ~/Downloads/OpenCV-android-sdk
    OPENCV_CAMERA_MODULES:=on
    OPENCV_INSTALL_MODULES:=on
    OPENCV_LIB_TYPE:=SHARED
    include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

    LOCAL_SRC_FILES := com_speedcam_OpenCVDetection.cpp

    LOCAL_LDLIBS += -llog
    LOCAL_MODULE := opencv


    include $(BUILD_SHARED_LIBRARY)

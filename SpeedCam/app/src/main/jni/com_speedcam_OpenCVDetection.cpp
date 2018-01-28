#include "com_speedcam_OpenCVDetection.h"

JNIEXPORT void JNICALL Java_com_speedcam_OpenCVDetection_signDetection
  (JNIEnv *, jclass, jlong addrFrame) {
    Mat& frame = *(Mat*)addrFrame;

    detectObject(frame);
  }

  void detectObject(Mat& frame) {
    String sign_cascade_file = "/storage/emulated/0/data/cascade.xml";
    CascadeClassifier sign_cascade;

    if(!sign_cascade.load(sign_cascade_file)) {
        printf("Error loading sign cascade\n");
        return;
    };

    std::vector<Rect> signs;
    Mat frame_gray;
    cvtColor(frame, frame_gray, COLOR_BGR2GRAY);

    sign_cascade.detectMultiScale(frame_gray, signs);
    for (int i = 0; i < signs.size(); i++)
    {
        Point start_p(signs[i].x, signs[i].y);
        Point end_p(signs[i].x + signs[i].width, signs[i].y + signs[i].height);
        rectangle(frame, start_p, end_p, Scalar(255, 0, 255 ), 2);
    }
  }


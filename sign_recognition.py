# Detecting traffic sign from a video file


import cv2, sys
import numpy as np
import skimage.transform
from skimage import io
import tensorflow as tf


NEG_IMAGE_CLASS = 43
IMAGE_WIDTH = 32
IMAGE_LENGTH = 32
COLOR_CHANNELS = 3

video_path = sys.argv[1]

sign_cascade = cv2.CascadeClassifier('./opencv_training/haar_classifier/cascade.xml')
video = cv2.VideoCapture(video_path)

ret, frame = video.read()
h, w, channel = frame.shape
fourcc = cv2.VideoWriter_fourcc(*'XVID')
out = cv2.VideoWriter('output.avi', fourcc, 20.0, (w, h))

saver = tf.train.import_meta_graph('./model/model.meta')
sess = tf.Session()
saver.restore(sess, tf.train.latest_checkpoint('./model/'))


while (video.isOpened()):
    ret, frame = video.read()
    if not ret:
        break

    signs = sign_cascade.detectMultiScale(frame, 1.4, 5)
 
    for (x, y, w, h) in signs:
        cropped_sign = frame[y: y + h, x: x + w]

        resized_sign = skimage.transform.resize(cv2.cvtColor(cropped_sign, cv2.COLOR_BGR2RGB), (IMAGE_WIDTH, IMAGE_LENGTH))
        reshaped_sign = resized_sign.reshape(IMAGE_WIDTH * IMAGE_LENGTH * COLOR_CHANNELS)
        predicted = sess.run("prediction:0",
                                  feed_dict={"images:0": [reshaped_sign]})[0]

        if predicted != NEG_IMAGE_CLASS:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 255), 2)
            font = cv2.FONT_HERSHEY_SIMPLEX
            cv2.putText(frame, str(predicted), (x, y - 10), font, 0.5, (255, 0, 255), 2, cv2.LINE_AA)
   
    out.write(frame)

video.release()
out.release()

import cv2
import numpy as np
import matplotlib.pyplot as plt
import skimage.transform
from skimage import io
import tensorflow as tf


NEG_IMAGE_CLASS = 43
IMAGE_WIDTH = 32
IMAGE_LENGTH = 32

sign_cascade = cv2.CascadeClassifier('./opencv_data/cascade.xml')
video = cv2.VideoCapture('./datasets/videos/TS2011video4.wmv')


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
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    signs = sign_cascade.detectMultiScale(gray, 1.3, 5)
 

    for (x, y, w, h) in signs:
        cropped_sign = frame[y: y + h, x: x + w]
        resized_sign = skimage.transform.resize(cropped_sign, (32, 32))
    
        predicted = sess.run(["prediction:0"],
                                  feed_dict={"images_ph:0": [resized_sign]})[0][0]

        if predicted != NEG_IMAGE_CLASS:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
            print(predicted)
    
    cv2.imshow('f', frame)
    out.write(frame)

video.release()
out.release()
cv2.destroyAllWindows()

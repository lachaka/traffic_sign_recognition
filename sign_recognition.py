import numpy as np
import cv2
import matplotlib.pyplot as plt
import skimage.transform
from skimage import io
import tensorflow as tf

IMAGE_WIDTH = 32
IMAGE_LENGTH = 32
sign_cascade = cv2.CascadeClassifier('./opencv_data/cascade.xml')
video = cv2.VideoCapture('./datasets/videos/TS2011video0.wmv')


saver = tf.train.import_meta_graph('./model/model.meta')
sess = tf.Session()
saver.restore(sess, tf.train.latest_checkpoint('./model/'))

#graph = tf.get_default_graph()
#images_ph = graph.get_tensor_by_name("images_ph:0")
#predicted_labels = graph.get_tensor_by_name("prediction:0")

while (video.isOpened()):
    ret, frame = video.read()
    if not ret:
        break
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    signs = sign_cascade.detectMultiScale(gray, 1.3, 5)

    for (x, y, w, h) in signs:
        cropped_sign = frame[y: y + h, x: x + w]
        resized_sign = []
        resized_sign.append(skimage.transform.resize(cropped_sign, (32, 32)))

        resized_sign.append(skimage.transform.resize(cropped_sign, (32, 32)))
        sign_arr = np.array(resized_sign)
        print(tf.shape(sign_arr))
        #print(images_ph.get_shape())
        print("-------------")
        predicted = sess.run("prediction:0",
                             feed_dict={"images_ph:0": sign_arr})[0]

        if predicted != 43:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
            print(predicted)

video.release()

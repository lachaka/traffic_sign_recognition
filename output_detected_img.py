# Output detected images from video.
# Change 'PATH_TO_CHANGE'.
import cv2

cv_cascade = cv2.CascadeClassifier('./opencv_data/cascade.xml')
video = cv2.VideoCapture('PATH_TO_CHANGE')

output_file_name = 0
output_dir = 'PATH_TO_CHANGE'

while (video.isOpened()):
    ret, frame = video.read()
    if not ret:
        break
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    signs = cv_cascade.detectMultiScale(gray, 1.3, 5)

    for (x, y, w, h) in signs:
        cropped_sign = frame[y: y + h, x: x + w]
        cv2.imwrite(output_dir + str(output_file_name) + '.ppm', cropped_sign)
        output_file_name += 1

video.release()

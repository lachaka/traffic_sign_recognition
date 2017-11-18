import cv2
import os
import skimage.io

directory = './data/'
images_path = './samples/'

if not os.path.exists(directory):
    os.makedirs(directory)

name = 0;
img_names = ['0.ppm', '00199.ppm', '00215.ppm', '00273.ppm']

for img_name in img_names:
	img = cv2.imread(images_path + img_name)
	h, w, channel = skimage.io.imread(images_path + img_name).shape

	for i in range(0, w - 128, 32):
		for j in range(0, h - 128, 32):
			crop_img = img[j:(j + 128), i:(i + 128)]
			cv2.imwrite(directory + '%s.ppm' % name, crop_img)
			name += 1

import cv2
import os
import skimage.io

directory = './data/'
images_path = './samples/'
bg_file = open('bg.txt', 'w')

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
			img_full_path = directory + str(name) + '.ppm'
			cv2.imwrite(img_full_path, crop_img)
			bg_file.write(img_full_path + '\n')
			name += 1
bg_file.close()

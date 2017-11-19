import os
from skimage import io

info_file = open('signs.info', 'w')
path = './datasets/training_images/Final_Training/Images'
directories = [d for d in os.listdir(path)
	              if os.path.isdir(os.path.join(path, d))]
for d in directories:
	temp_path = os.path.join(path, d)
	file_names = [os.path.join(temp_path, f) 
	                  for f in os.listdir(temp_path) if f.endswith(".ppm")]
	for item in file_names:
		h, w, channel = io.imread(item).shape
		info_file.write('{0} 1 0 0 {1} {2}\n'.format(item, w, h))
		
info_file.close()

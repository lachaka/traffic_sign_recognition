import os

bg_file = open('bg.txt', 'w')
path = './datasets/training_images/Final_Training/Images/00043'

images = [os.path.join(path, f) 
          for f in os.listdir(path) if f.endswith(".ppm")]

for image in images:
	bg_file.write(image + '\n')
		
bg_file.close()

import cv2

sign_cascade = cv2.CascadeClassifier('./opencv_data/cascade.xml')
video = cv2.VideoCapture('./datasets/videos/TS2010video.wmv')

output_file_name = 0
output_dir = './datasets/training_images/Final_Training/Images/00043/'

while (video.isOpened()):
	ret, frame = video.read()
	if not ret:
		break
	gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
	signs = sign_cascade.detectMultiScale(gray, 1.3, 5)
	
	for (x, y, w, h) in signs:
		cropped_sign = frame[y: y + h, x: x + w]
		cv2.imwrite(output_dir + str(output_file_name) + '.ppm', cropped_sign)
		output_file_name += 1		
	 
video.release()

import csv
import os
from skimage import io

path = '../training_images/Final_Training/Images/00043/'
csv_file_name = path + 'GT-00043.csv'
files = [os.path.join(path, f) 
          for f in os.listdir(path) if f.endswith(".ppm")]

csv = open(csv_file_name, 'w')
csv.write('Filename;Width;Height;Roi.X1;Roi.Y1;Roi.X2;Roi.Y2;ClassId\n')

for file in files:
    h, w, channel = io.imread(file).shape
    csv.write('{0};{1};{2};0;0;{1};{2};43\n'.format(os.path.basename(file), w, h))

csv.close()

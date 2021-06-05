
# code for displaying multiple images in one figure
  
#import libraries
import cv2
from matplotlib import pyplot as plt
  
# create figure
fig = plt.figure(figsize=(10, 7))
  
# setting values to rows and column variables
rows = 2
columns = 2
  
# reading images
prefix = 'lb20_cyc_m'
output_prefix = prefix + '_2to3Eviews'
output_name = output_prefix + '/' + output_prefix
querynum=1
outFN = output_name + "_q" + str(querynum+6)

for v in range(3):
    Image = cv2.imread(outFN+"_v"+ str(v) + ".png")
      
    # Adds a subplot at the 1st position
    fig.add_subplot(rows, columns, v+1)
    plt.imshow(Image)
    plt.axis('off')
    plt.title("View " + str(v))
    
plt.savefig(outFN+"_views.png")
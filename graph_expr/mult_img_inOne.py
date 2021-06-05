
# code for displaying multiple images in one figure
  
#import libraries
import cv2
from matplotlib import pyplot as plt
  
prefix = 'lb20_cyc_m'
output_prefix = prefix + '_2to3Eviews'
output_name = output_prefix + '/' + output_prefix
    
for querynum in range(13):
    outFN = output_name + "_q" + str(querynum+6)
    
    fig = plt.figure(figsize=(10, 7))
    plt.title("Query " + str(querynum+6), color='g')
    plt.axis('off')
    
    numViewsFN = open(outFN + "_edgeInfo.txt", "r")
    while True:
        line = numViewsFN.readline().replace('\n','')
        lineAsList = line.split(' ')
        numViews = int(lineAsList[2])
        break
    
    # columns = 2
    # rows = numViews % columns + 1
    columns = 2
    rows = 2

    for v in range(numViews):
        Image = cv2.imread(outFN+"_v"+ str(v) + ".jpg")
          
        # Adds a subplot at the 1st position
        fig.add_subplot(rows, columns, v+1)
        plt.imshow(Image)
        plt.axis('off')
        plt.title("View " + str(v))
        
    plt.savefig(outFN+"_views.png")
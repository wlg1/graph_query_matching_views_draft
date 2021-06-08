
# code for displaying multiple images in one figure
  
#import libraries
import cv2
from matplotlib import pyplot as plt

#tunable params
edgeType = 'm'
numE = '2'
  
prefix = 'lb20_cyc_'+edgeType
output_prefix = prefix + '_'+numE+'Eviews'
output_name = output_prefix + '/' + output_prefix
    
for querynum in range(13):
    outFN = output_name + "_q" + str(querynum+6)
    
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
    
    pt = 1
    fig = plt.figure(figsize=(10, 7))
    plt.title("Query " + str(querynum+6), color='g')
    plt.axis('off')

    numViewsOnPlot_count = 1
    for v in range(numViews):
        if numViewsOnPlot_count > 4:
            plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")
            fig = plt.figure(figsize=(10, 7))
            plt.title("Query " + str(querynum+6) + '_pt'+str(pt), color='g')
            plt.axis('off')
            numViewsOnPlot_count = 1
            pt += 1
        Image = cv2.imread(outFN+"_v"+ str(v) + ".jpg")
          
        # Adds a subplot at the 1st position
        fig.add_subplot(rows, columns, numViewsOnPlot_count)
        plt.imshow(Image)
        plt.axis('off')
        plt.title("View " + str(v))
        numViewsOnPlot_count += 1
        
    plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")

# code for displaying multiple images in one figure
  
#import libraries
import cv2, os
from matplotlib import pyplot as plt

#tunable params
edgeTypeList = ['c', 'd', 'm']
numE_List = ['2', '3']

def run(edgeType, numE):
    prefix = 'lb20_cyc_'+edgeType
    output_prefix = prefix + '_'+numE+'Eviews'
    output_folder = prefix + '_'+numE+'Eviews' + '/multi_view_imgs'
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    input_name = output_prefix + '/' + output_prefix
    output_name = output_folder + '/' + output_prefix
        
    for querynum in range(13):
        inFN = input_name + "_q" + str(querynum+6)
        outFN = output_name + "_q" + str(querynum+6)
        
        numViewsFN = open(inFN + "_edgeInfo.txt", "r")
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
        plt.title("Query " + str(querynum+6) + '_pt'+str(pt), color='g')
        plt.axis('off')
    
        numViewsOnPlot_count = 1
        for v in range(numViews):
            if numViewsOnPlot_count > 4:
                plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")
                fig = plt.figure(figsize=(10, 7))
                plt.title("Query " + str(querynum+6) + '_pt'+str(pt+1), color='g')
                plt.axis('off')
                numViewsOnPlot_count = 1
                pt += 1
            Image = cv2.imread(inFN+"_v"+ str(v) + ".jpg")
              
            # Adds a subplot at the 1st position
            fig.add_subplot(rows, columns, numViewsOnPlot_count)
            plt.imshow(Image)
            plt.axis('off')
            plt.title("View " + str(v+1))
            numViewsOnPlot_count += 1
            
        plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")
        
for edgeType in edgeTypeList:
    for numE in numE_List:
        run(edgeType, numE)
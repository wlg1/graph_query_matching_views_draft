
# code for displaying multiple images in one figure
  
#import libraries
import cv2, os
from matplotlib import pyplot as plt
from os import listdir
from os.path import isfile, join

#tunable params
edgeTypeList = ['m']
numE_List = ['2']

def run(edgeType, numE):
    # prefix = 'lb20_cyc_'+edgeType
    prefix = 'lb20_'+edgeType + '_'+numE+'Eviews'
    output_folder = prefix + '/multi_view_imgs'
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    # input_name = prefix + '/' + prefix
    output_name = output_folder + '/' + prefix
        
    # for querynum in range(13):
    for querynum in range(12,20):
        # inFN = input_name + "_q" + str(querynum)
        outFN = output_name + "_q" + str(querynum)
        
        # numViewsFN = open(inFN + "_edgeInfo.txt", "r")
        # while True:
        #     line = numViewsFN.readline().replace('\n','')
        #     lineAsList = line.split(' ')
        #     numViews = int(lineAsList[2])
        #     break
        
        # columns = 2
        # rows = numViews % columns + 1
        columns = 2
        rows = 2
        
        viewimgs = [f for f in listdir(prefix) if isfile(join(prefix, f))
                     and '.jpg' in f and 'partial' not in f
                     and 'q'+str(querynum) in f]
        
        pt = 1  #part; split images into 4 views each
        fig = plt.figure(figsize=(10, 7))
        plt.title("Query " + str(querynum) + '_pt'+str(pt), color='g')
        plt.axis('off')
    
        numViewsOnPlot_count = 1
        # for v in range(numViews):
        for imgname in viewimgs:
            if numViewsOnPlot_count > 4:
                plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")
                fig = plt.figure(figsize=(10, 7))
                plt.title("Query " + str(querynum) + '_pt'+str(pt+1), color='g')
                plt.axis('off')
                numViewsOnPlot_count = 1
                pt += 1
            # Image = cv2.imread(inFN+"_v"+ str(v) + ".jpg")
            Image = cv2.imread(prefix + '/' + imgname)
              
            # Adds a subplot at the 1st position
            fig.add_subplot(rows, columns, numViewsOnPlot_count)
            plt.imshow(Image)
            plt.axis('off')
            # plt.title("View " + str(v+1))
            viewname = imgname.split('_')[-1].replace('.jpg','')
            plt.title(viewname)
            numViewsOnPlot_count += 1
            
        plt.savefig(outFN+"_views" + '_pt'+str(pt)+".png")
        
for edgeType in edgeTypeList:
    for numE in numE_List:
        run(edgeType, numE)
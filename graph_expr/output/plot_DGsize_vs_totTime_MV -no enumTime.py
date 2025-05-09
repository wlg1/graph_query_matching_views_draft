import os, csv
import matplotlib.pyplot as plt

#tunable params
numNodes = [100, 120, 140, 160]
use_logScale = True

algoNames = ['inst_lb20_d_views_MVFltSim', 
              'inst_lb20_m_FltSim']
timeDict = {}  # qryNum : {algo : timeByLabel } 
for qryNum in range(0,20):
    timeDict[qryNum] = {}
    for algo in algoNames:
        timeDict[qryNum][algo] = [] #timeByLabel is a list

numSolns = {}  #qryNum : [numsolns for each label]
for qryNum in range(0,20):
    numSolns[qryNum] = [0] * len(numNodes)
    
for lblIndex, size in enumerate(numNodes): #loop thru x-axis. each is in diff file
    for algo in algoNames:
        filename = 'Email_lb20_' + str(size) + 'K_nodes_' \
            + algo + '_newcols.csv'
        file = open(filename, 'r')
        reader = csv.reader(file, delimiter=',')
        startReadFlag = False
        for row in reader:
            if not row:
                continue
            if row[0] == 'Average':
                startReadFlag = True
                continue
            if not startReadFlag:
                continue
            if row[0].isnumeric():
                tot_no_enum = float(row[4]) + float(row[6])
                timeDict[int(row[0])][algo].append(tot_no_enum)
                numSolns[int(row[0])][lblIndex] = row[-3].replace(',','')

# #plot edges vs algoTotTime. each algo is a separate line in plot

for qryNum in range(0,20):
    plt.clf()
    if use_logScale == True:
        plt.yscale("log")
    for algo in algoNames:
        # xAxis =  [str(x) for x in numLabels]
        qryAlgoNumSolns = ["{:,}".format(int(x)) for x in numSolns[qryNum]]
        xAxis =  [str(x)+'K, \n'+ qryAlgoNumSolns[i] +'\nsolns' for i, x in enumerate(numNodes)]
    
        yAxis = timeDict[qryNum][algo]
        plt.plot(xAxis, yAxis, label = algo)
    plt.title('#DGlabels vs totTime (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    output_prefix = 'q_' + str(qryNum)
    if use_logScale == True:
        plt.savefig(output_prefix+'_TotTimes_logscale.png', bbox_inches="tight")
    else:
        plt.savefig(output_prefix+'_TotTimes.png', bbox_inches="tight")




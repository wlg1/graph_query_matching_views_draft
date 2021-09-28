import os, csv
import matplotlib.pyplot as plt

#tunable params
# numNodes = [100, 120, 140, 160]
numNodes = [200, 250, 300]
use_logScale = False

# DG = 'Email_lb20'
DG = 'am_lb24_v1'
algoNames = ['inst_cyc_d_views_MVFltSim', 
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
        filename = DG + '_' + str(size) + 'K_nodes_' \
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
                timeDict[int(row[0])][algo].append(float(row[8]))
                numSolns[int(row[0])][lblIndex] = row[-3].replace(',','')

# #plot edges vs algoTotTime. each algo is a separate line in plot

for qryNum in range(0,20):
    plt.clf()
    if use_logScale == True:
        plt.yscale("log")
    for algo in algoNames:
        # xAxis =  [str(x) for x in numLabels]
        # qryAlgoNumSolns = ["{:,}".format(int(x)) for x in numSolns[qryNum]]
        xAxis =  [str(x)+'K, \n'+ numSolns[qryNum][i] +'\nsolns' for i, x in enumerate(numNodes)]
    
        yAxis = timeDict[qryNum][algo]
        plt.plot(xAxis, yAxis, label = algo)
    plt.title('#DGnodes vs totTime (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    output_prefix = 'q_' + str(qryNum)
    if use_logScale == True:
        plt.savefig(output_prefix+'_TotTimes_logscale.png', bbox_inches="tight")
    else:
        plt.savefig(output_prefix+'_TotTimes.png', bbox_inches="tight")




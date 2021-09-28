import csv
import matplotlib.pyplot as plt

#tunable params
numDElist = [1, 2, 3, 4]
use_logScale = False

DG = 'Email_lb20_'
# DG = 'bs_lb20_v1'
algoNames = ['lb20_q13_', 
              'lb20_cyc_m_q13__IEFLTSIM']
timeDict = {}  # qryNum : {algo : timeByLabel } 
for qryNum in range(0,20):
    timeDict[qryNum] = {}
    for algo in algoNames:
        timeDict[qryNum][algo] = [] #timeByLabel is a list

for lblIndex, numDE in enumerate(numDElist): #loop thru x-axis. each is in diff file
    algo = algoNames[0]
    filename = DG + algo + str(numDE) + 'd_MVFltSim_newcols.csv'
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

    algo = algoNames[1]
    filename = DG + algo + '_newcols.csv'
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

# #plot edges vs algoTotTime. each algo is a separate line in plot
for qryNum in range(1):
    plt.clf()
    if use_logScale == True:
        plt.yscale("log")
    for algo in algoNames:
        xAxis =  [str(x) + 'DE' for x in numDElist]
        yAxis = timeDict[qryNum][algo]
        plt.plot(xAxis, yAxis, label = algo)
    plt.title('#DE in views vs totTime (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    output_prefix = 'q_' + str(qryNum)
    if use_logScale == True:
        plt.savefig(output_prefix+'_TotTimes_logscale.png', bbox_inches="tight")
    else:
        plt.savefig(output_prefix+'_TotTimes.png', bbox_inches="tight")




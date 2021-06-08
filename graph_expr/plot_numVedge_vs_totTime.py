import os, csv
import matplotlib.pyplot as plt

#loop thru files in folder to get avg totTime of queries
#also each qry has its own plot

#tunable params
edgeTypeList = ['c']
numqrys = 13
totNumE = 3

def run(edgeType, numqrys, totNumE):
    algoNames = ['View_sim_rmvEmp', 'View_sim', 'FLTSIM', 'FLT', 'SIM']
    totTimes_ByEdge = []
    for numE in range(1,totNumE + 1):
        algo_totTimes = []
        for i in range(len(algoNames)):
            algo_totTimes.append([0] * numqrys)    
        output_prefix = 'output/Email_lb20_lb20_cyc_'+edgeType+'_'+str(numE)+'Eviews'
        input_path = output_prefix + '/'
        for filename in os.listdir(input_path):
            if 'ALL' not in filename:
                continue
            filename_as_list = filename.split('_')
            qrynum = int(filename_as_list[4].replace('q','')) - 6
            file = open(input_path+filename, 'r')
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if row[0] in algoNames:
                    algo_totTimes[algoNames.index(row[0])][qrynum] = float(row[6])
        totTimes_ByEdge.append(algo_totTimes)
    
    #plot edges vs algoTotTime. each algo is a separate line in plot
    xAxis =  [str(x+1)+'E' for x in list(range(totNumE))]
    for algoNum in range(len(algoNames)):
        yAxis = []
        for edgeTimes in totTimes_ByEdge:
            algoTimes = edgeTimes[algoNum]
            yAxis.append(sum(algoTimes) / len(algoTimes))
        plt.plot(xAxis, yAxis, label = algoNames[algoNum])
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    plt.savefig(output_prefix+'_avgTimes.png', bbox_inches="tight")
    
    for qrynum in range(numqrys):
        plt.figure()
        for algoNum in range(len(algoNames)):
            yAxis = []
            for edgeTimes in totTimes_ByEdge:
                algoTimes = edgeTimes[algoNum]
                yAxis.append(algoTimes[qrynum])
            plt.plot(xAxis, yAxis, label = algoNames[algoNum])
        plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
        plt.savefig(output_prefix+'_q'+str(qrynum+6)+'_Times.png', bbox_inches="tight")
            
for edgeType in edgeTypeList:
    run(edgeType, numqrys, totNumE)
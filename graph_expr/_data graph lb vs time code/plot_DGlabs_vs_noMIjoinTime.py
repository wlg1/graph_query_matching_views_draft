import os, csv
import matplotlib.pyplot as plt

#loop thru files in folder to get avg totTime of queries
#also each qry has its own plot

#tunable params
numqrys = 1
numLabels = [5, 10, 20, 40]
dataGraphType = 'Email_lb'
queryType_prefix = 'lb5_cyc_m_2Eviews_q'
use_logScale = False

# Email_lb20_100K_nodes_lb20_acyc_m_q4_2Eviews

def run(dataGraphType, queryType, numLabels, numqrys, use_logScale):
    # algoNames = ['View_sim_rmvEmp', 'View_sim', 'FLTSIM', 'FLT', 'SIM']
    # algoNames = ['View_sim_rmvEmp', 'View_sim']
    algoNames = ['View_sim_rmvEmp', 'View_sim', 'SIM']
    numSolns = [0] * len(numLabels)
    totTimes_ByEdge = []  #contains each x-axis tick's y-value
    for numLbs in numLabels: #loop thru x-axis. each x-axis tick is in diff folder
        algo_totTimes = []
        for i in range(len(algoNames)):
            algo_totTimes.append([0] * numqrys) #do this to avg at end
        output_prefix = 'output/' + dataGraphType + \
            str(numLbs) + '_v2_' + queryType
        input_path = output_prefix + '/'
        os.listdir(input_path).sort()
        for filename in os.listdir(input_path):
            if 'ALL' not in filename:
                continue
            file = open(input_path+filename, 'r')
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if row[0] in algoNames:
                    numSolns[numLabels.index(numLbs) ] = (row[-3])
                    algo_totTimes[algoNames.index(row[0])][0] = float(row[3]) + float(row[4])
        totTimes_ByEdge.append(algo_totTimes)
    
    #plot edges vs algoTotTime. each algo is a separate line in plot
    numSolns = ["{:,}".format(int(x)) for x in numSolns]
    # xAxis =  [str(x)+"\nsolns" for x in numSolns]
    xAxis =  [str(x)+'K, \n'+ numSolns[i] +'\nsolns' for i, x in enumerate(numLabels)]
    # xAxis =  [str(x)+"K" for x in nodeSizes]
    plt.clf()
    if use_logScale == True:
        plt.yscale("log")
    for algoNum in range(len(algoNames)):
        yAxis = []
        for edgeTimes in totTimes_ByEdge:
            algoTimes = edgeTimes[algoNum]
            yAxis.append(sum(algoTimes) / len(algoTimes))
        plt.plot(xAxis, yAxis, label = algoNames[algoNum])
    plt.title('#DGlabels vs _sgBuildTimes (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    if use_logScale == True:
        plt.savefig(output_prefix+'_noMIjoinTimes_logscale.png', bbox_inches="tight")
    else:
        plt.savefig(output_prefix+'__noMIjoinTimes.png', bbox_inches="tight")
    
    # for qrynum in range(numqrys):
    #     plt.figure()
    #     for algoNum in range(len(algoNames)):
    #         yAxis = []
    #         for edgeTimes in totTimes_ByEdge:
    #             algoTimes = edgeTimes[algoNum]
    #             yAxis.append(algoTimes[qrynum])
    #         plt.plot(xAxis, yAxis, label = algoNames[algoNum])
    #     plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    #     # plt.savefig(output_prefix+'_q'+str(qrynum+10)+'_Times.png', bbox_inches="tight")
    #     plt.savefig(output_prefix+'_q'+str(qrynum+10)+'_Times.png', bbox_inches="tight")
            
for q in range(6,19):
    if q in [9, 14, 15, 16, 18 ]:
        continue
    queryType = queryType_prefix + str(q)
    run(dataGraphType, queryType, numLabels, numqrys, use_logScale)
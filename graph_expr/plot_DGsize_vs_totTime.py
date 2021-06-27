import os, csv
import matplotlib.pyplot as plt

#loop thru files in folder to get avg totTime of queries
#also each qry has its own plot

#tunable params
edgeTypeList = ['c']
numqrys = 1
nodeSizes = [60, 80, 100]  #per K (10000)
dataGraphType = 'Email_lb20'
queryType = 'lb20_acyc_m_q5_2Eviews'
# queryType = 'lb20_cyc_m_2Eviews_q13'

# Email_lb20_100K_nodes_lb20_acyc_m_q4_2Eviews

def run(edgeType, dataGraphType, queryType, nodeSizes, numqrys):
    # algoNames = ['View_sim_rmvEmp', 'View_sim', 'FLTSIM', 'FLT', 'SIM']
    # algoNames = ['View_sim_rmvEmp', 'View_sim']
    algoNames = ['View_sim_rmvEmp', 'View_sim', 'SIM']
    numSolns = [0] * len(nodeSizes)
    totTimes_ByEdge = []  #contains each x-axis tick's y-value
    for numNodes in nodeSizes: #loop thru x-axis. each x-axis tick is in diff folder
        algo_totTimes = []
        for i in range(len(algoNames)):
            algo_totTimes.append([0] * numqrys) #do this to avg at end
        output_prefix = 'output/' + dataGraphType+ '_' \
            + str(numNodes) + 'K_nodes_' + queryType
        input_path = output_prefix + '/'
        os.listdir(input_path).sort()
        for filename in os.listdir(input_path):
            if 'ALL' not in filename:
                continue
            file = open(input_path+filename, 'r')
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if row[0] in algoNames:
                    numSolns[nodeSizes.index(numNodes) ] = (row[-3])
                    algo_totTimes[algoNames.index(row[0])][0] = float(row[6])
        totTimes_ByEdge.append(algo_totTimes)
    
    #plot edges vs algoTotTime. each algo is a separate line in plot
    numSolns = ["{:,}".format(int(x)) for x in numSolns]
    # xAxis =  [str(x)+"\nsolns" for x in numSolns]
    xAxis =  [str(x)+'K, \n'+ numSolns[i] +'\nsolns' for i, x in enumerate(nodeSizes)]
    # xAxis =  [str(x)+"K" for x in nodeSizes]
    plt.clf()
    for algoNum in range(len(algoNames)):
        yAxis = []
        for edgeTimes in totTimes_ByEdge:
            algoTimes = edgeTimes[algoNum]
            yAxis.append(sum(algoTimes) / len(algoTimes))
        plt.plot(xAxis, yAxis, label = algoNames[algoNum])
    plt.title('#DGnodes vs totTime (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    plt.savefig(output_prefix+'_avgTimes.png', bbox_inches="tight")
    
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
            
for edgeType in edgeTypeList:
    run(edgeType, dataGraphType, queryType, nodeSizes, numqrys)
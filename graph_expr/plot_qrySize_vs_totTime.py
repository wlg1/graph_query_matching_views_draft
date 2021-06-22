import os, csv
import matplotlib.pyplot as plt

#loop thru files in folder to get avg totTime of queries
#also each qry has its own plot

#tunable params
edgeTypeList = ['c']
numqrys = 1
qrySizes = [4,6,8]
dataType = 'Email_lb20_lb20_cyc_c_combQrys__15_2Eviews_ovl_1_10_6DE'

def run(edgeType, dataType, qrySizes, numqrys):
    # algoNames = ['View_sim_rmvEmp', 'View_sim', 'FLTSIM', 'FLT', 'SIM']
    # algoNames = ['View_sim_rmvEmp', 'View_sim']
    algoNames = ['View_sim_rmvEmp', 'View_sim', 'SIM']
    numSolns = [0] * len(qrySizes)
    totTimes_ByEdge = []  #contains each x-axis tick's y-value
    for numE in qrySizes: #loop thru x-axis. each x-axis tick is in diff folder
        algo_totTimes = []
        for i in range(len(algoNames)):
            algo_totTimes.append([0] * numqrys) #do this to avg at end
        output_prefix = 'output/' + dataType+ '_'+ str(numE) +'numE'
        input_path = output_prefix + '/'
        os.listdir(input_path).sort()
        for filename in os.listdir(input_path):
            if 'ALL' not in filename:
                continue
            # filename_as_list = filename.split('_')
            # numE = int(filename_as_list[7])
            file = open(input_path+filename, 'r')
            reader = csv.reader(file, delimiter=',')
            for row in reader:
                if row[0] in algoNames:
                    numSolns[qrySizes.index(numE) ] = (row[-3])
                    algo_totTimes[algoNames.index(row[0])][0] = float(row[6])
        totTimes_ByEdge.append(algo_totTimes)
    
    #plot edges vs algoTotTime. each algo is a separate line in plot
    numSolns = ["{:,}".format(int(x)) for x in numSolns]
    xAxis =  [str(x)+'E, '+numSolns[i] +'solns' for i, x in enumerate(qrySizes)]
    plt.clf()
    for algoNum in range(len(algoNames)):
        yAxis = []
        for edgeTimes in totTimes_ByEdge:
            algoTimes = edgeTimes[algoNum]
            yAxis.append(sum(algoTimes) / len(algoTimes))
        plt.plot(xAxis, yAxis, label = algoNames[algoNum])
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
    run(edgeType, dataType, qrySizes, numqrys)
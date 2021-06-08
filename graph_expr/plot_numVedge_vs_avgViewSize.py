import os, csv
import matplotlib.pyplot as plt

def RepresentsInt(s):
    try: 
        int(s)
        return True
    except ValueError:
        return False

#tunable params
edgeTypeList = ['c']
numqrys = 13
totNumE = 3

def run(edgeType, numqrys, totNumE):
    algoNames = ['simgrBYVIEWS_rmvEmpty', 'simgrBYVIEWS']
    tot_ByEdge = []
    for numE in range(1,totNumE + 1):
        algo_avgViewSizes = {}
        for algoN in algoNames:
            algo_avgViewSizes[algoN] = [0] * numqrys
        output_prefix = 'output/Email_lb20_lb20_cyc_'+edgeType+'_'+str(numE)+'Eviews'
        input_path = output_prefix + '/'
        for filename in os.listdir(input_path):
            if 'VIEWS' not in filename:
                continue
            filename_as_list = filename.split('_')
            if 'rmvEmpty' in filename:
                algoN = filename_as_list[8] + "_rmvEmpty"
            else:
                algoN = filename_as_list[8]
            qrynum = int(filename_as_list[6].replace('q','')) - 6
            file = open(input_path+filename, 'r')
            reader = csv.reader(file, delimiter=',')
            startRead = False
            for row in reader:
                if 'View' in row[0]:
                    startRead = True
                if startRead and RepresentsInt(row[0]):
                    algo_avgViewSizes[algoN][qrynum] = float(row[-1])
                elif startRead and '*' in row[0]:
                    break
        tot_ByEdge.append(algo_avgViewSizes)
    
    #plot edges vs algoTotTime. each algo is a separate line in plot
    xAxis =  [str(x+1)+'E' for x in list(range(0,totNumE))]
    for algoName in algoNames:
        yAxis = []
        for algo_avgViewSizes in tot_ByEdge:
            algoVsize = algo_avgViewSizes[algoName]
            yAxis.append(sum(algoVsize) / len(algoVsize))
        plt.plot(xAxis, yAxis, label = algoName)
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    plt.savefig(output_prefix+'_avgViewSizes.png', bbox_inches="tight")
    
    for qrynum in range(numqrys):
        plt.figure()
        for algoName in algoNames:
            yAxis = []
            for algo_avgViewSizes in tot_ByEdge:
                algoVsize = algo_avgViewSizes[algoName]
                yAxis.append(algoVsize[qrynum])
            plt.plot(xAxis, yAxis, label = algoName)
        plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
        plt.savefig(output_prefix+'_q'+str(qrynum+6)+'_ViewSizes.png', bbox_inches="tight")
        
for edgeType in edgeTypeList:
    run(edgeType, numqrys, totNumE)
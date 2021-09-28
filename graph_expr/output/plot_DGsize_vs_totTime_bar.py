import csv, os
import matplotlib.pyplot as plt
from os import listdir
from os.path import isfile, join

#tunable params
# DG = 'email_lb20_'
DG = 'am_lb24_v1_'
nodeSizes = [200, 250]
querynums = list(range(7,19))
use_logScale = False

numSolns = {}  #qryNum : [numsolns for each label]
for qryNum in range(0,20):
    numSolns[qryNum] = [0] * len(nodeSizes)

# timeDict = {}  # qryNum : {algo : timeByLabel } 
# for querynum in querynums:   
#     timeDict[querynum] = {}
#     for algo in algoNames:
#         timeDict[querynum][algo] = [] #timeByLabel is a list
        
for querynum in querynums:    
    viewfiles = [f for f in listdir(os.getcwd()) if isfile(join(os.getcwd(), f))
                and '_MVFltSim_newcols.csv' in f
                and 'q'+str(querynum)+'_' in f]

    algoNames = [ 'lb20_cyc_d_q'+str(querynum)+'_partial_v1_MVFltSim_newcols', 
                 'lb20_cyc_d_q'+str(querynum)+'_partial_v1_FltSim_newcols']
    # algoNames = ['lb20_cyc_d_q'+str(querynum)+'_partial_v1_MVFltSim_newcols']
    timeDict = {}
    # filtTimeDict = {}
    prefiltTimeDict = {}
    simfiltTimeDict = {}
    buildTimeDict = {}
    joinTimeDict = {}
    for algo in algoNames:
        timeDict[algo] = [] 
        # filtTimeDict[algo] = [] 
        prefiltTimeDict[algo] = [] 
        simfiltTimeDict[algo] = [] 
        buildTimeDict[algo] = [] 
        joinTimeDict[algo] = [] 

    for nodeSize in nodeSizes: #loop thru x-axis. each is in diff file
        for algo in algoNames:
            filename = DG + str(nodeSize) + 'K_nodes_' + \
                algo + '.csv'
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
                    prefiltTimeDict[algo].append(float(row[2]))
                    simfiltTimeDict[algo].append(float(row[3]))
                    # filtTimeDict[algo].append(float(row[4]))
                    buildTimeDict[algo].append(float(row[6]))
                    joinTimeDict[algo].append(float(row[7]))
                    timeDict[algo].append(float(row[8]))
                    # timeDict[int(row[0])][algo].append(float(row[8]))
                    # timeDict[int(row[0])][algo].append(float(row[4]))
                    # timeDict[int(row[0])][algo].append(float(row[6]))
    
    import pandas as pd
    import matplotlib
    import numpy as np
    
    fig = plt.figure(figsize=(20, 10))
    font = {'family' : 'normal',
        'weight' : 'bold',
        'size'   : 22}
    matplotlib.rc('font', **font)
    
    algo = algoNames[0]
    prefltBar = prefiltTimeDict[algo].copy()
    simfltBar = simfiltTimeDict[algo].copy()
    buildBar = buildTimeDict[algo].copy()
    joinBar = joinTimeDict[algo].copy()
    
    algo = algoNames[1]
    prefltBar2 = prefiltTimeDict[algo].copy()
    simfltBar2 = simfiltTimeDict[algo].copy()
    buildBar2 = buildTimeDict[algo].copy()
    joinBar2 = joinTimeDict[algo].copy()
    
    df = pd.DataFrame(dict(
    pre = prefltBar,
    sim = simfltBar,
    build = buildBar,
    joinT = joinBar,
    
    pre2 = prefltBar2,
    sim2 = simfltBar2,
    build2 = buildBar2,
    joinT2 = joinBar2))

    
    xAxis = np.arange(len(nodeSizes))
    # xAxis =  [str(x)+'views, \n'+ str(overlaps[i]) +' Solns' for i, x in enumerate(numDElist)]
    view_bar_list = [plt.bar(xAxis, df.pre, align='center', width= 0.2, label='prefiltTime', \
                             hatch='x', color = 'blue'),
                plt.bar(xAxis, df.sim, align='center', width= 0.2, label='simfiltTime', \
                        bottom = df.pre, hatch='o', color = 'orange'),
               plt.bar(xAxis, df.build, align='center', width= 0.2, label='buildTime', \
                       bottom = df.pre+ df.sim, hatch = '//', color = 'red'),
               plt.bar(xAxis, df.joinT, align='center', width= 0.2, label='joinTime', \
                       bottom = df.pre+ df.sim+ df.build, hatch = '-', color = 'purple')]
    
    xAxis = [x +0.2 for x in xAxis]
    nonview_bar_list = [plt.bar(xAxis, df.pre2, align='center', width= -0.2, label='_nolegend_', \
                              hatch='x', color = 'blue'),
                plt.bar(xAxis, df.sim2, align='center', width= -0.2, label='_nolegend_', \
                        bottom = df.pre2, hatch='o', color = 'orange'),
                plt.bar(xAxis, df.build2, align='center', width= -0.2, label='_nolegend_', \
                        bottom = df.pre2+ df.sim2, hatch = '//', color = 'red'),
                plt.bar(xAxis, df.joinT2, align='center', width= -0.2, label='_nolegend_', \
                        bottom = df.pre2+ df.sim2+ df.build2, hatch = '-', color = 'purple')]
    
    xAxis_ticks =  [str(x)+'K nodes' for x in nodeSizes]
    tickAxis = [x -0.1 for x in xAxis]
    plt.xticks(tickAxis, xAxis_ticks)
        
    plt.ylabel('Time (secs)')
    plt.title('#views vs Time (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    # plt.margins(y=0.5, tight=True)
    y_min, y_max = plt.gca().get_ylim()
    plt.gca().set_ylim([y_min, y_max * 1.05])
    
    totTimes = []
    algo = algoNames[0]
    for i in range(len(timeDict[algo])):
        tot = prefiltTimeDict[algo][i] + simfiltTimeDict[algo][i] + \
            buildTimeDict[algo][i] + joinTimeDict[algo][i]
        totTimes.append(tot)
        
    totTimes2 = []
    algo = algoNames[1]
    for i in range(len(timeDict[algo])):
        tot = prefiltTimeDict[algo][i] + simfiltTimeDict[algo][i] + \
            buildTimeDict[algo][i] + joinTimeDict[algo][i]
        totTimes2.append(tot)
            
    font = {'family' : 'normal',
        'size'   : 18}
    matplotlib.rc('font', **font)
    xlocs, xlabs = plt.xticks()
    # for i, v in enumerate(timeDict[algo]):
    for i, v in enumerate(totTimes):
        # plt.text(xlocs[i] - 0.1, v * 1.01, str(round(v,2)))
        plt.text(xlocs[i] - 0.1, v * 1.05, 'View')
    for i, v in enumerate(totTimes2):
        plt.text(xlocs[i] + 0.1, v * 1.05, 'FLTSIM')
    
    # plt.show()
    
    # output_prefix = DG + algoNames[0]
    # if use_logScale == True:
    #     plt.savefig(output_prefix+'_times_stackedbar_logscale.png', bbox_inches="tight")
    # else:
    #     plt.savefig(output_prefix+'_times_stackedbar.png', bbox_inches="tight")

    # LINE TREND PLOTS
    
    # totTimes = timeDict[algo]
    df = pd.DataFrame(dict(
    views = totTimes ))
    
    # FLTSIM_tot = [ timeDict[algo2][0] ] * (len(totTimes)+1)
    # df2 = pd.DataFrame(dict(
    # FLTSIM = FLTSIM_tot))
        
    #plot edges vs algoTotTime. each algo is a separate line in plot
    # plt.clf()
    if use_logScale == True:
        plt.yscale("log")
    # for algo in algoNames:
    #     xAxis =  [str(x)+'views, \n'+ str(overlaps[i]) +'\nOVLe' for i, x in enumerate(numDElist)]
    #     # xAxis =  [str(x) + 'views' for x in numDElist]
    #     # yAxis = timeDict[querynum][algo]
    #     yAxis = timeDict[algo]
    #     if 'FltSim' not in algo:
    #         plt.plot(xAxis, yAxis, label = algo+'views')
    #     else:
    #         plt.plot(xAxis, yAxis, label = algo)
    # df2['FLTSIM'].plot(color='red', linestyle='dotted')
    # df['views'].plot(color='purple')
    plt.title('# DGnodes vs totTime (secs)')
    # plt.title('#views vs buildTime (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    # output_prefix = 'q' + str(qryNum)
    output_prefix = DG + algoNames[0]
    if use_logScale == True:
        # plt.savefig(output_prefix+'_TotTimes_logscale.png', bbox_inches="tight")
        plt.savefig(output_prefix+'_bar_line_TotTimes_logscale.png', bbox_inches="tight")
    else:
        plt.savefig(output_prefix+'_bar_line_TotTimes.png', bbox_inches="tight")
        # plt.savefig(output_prefix+'_TotTimes.png', bbox_inches="tight")
        # plt.savefig(output_prefix+'_filtTimes.png', bbox_inches="tight")
        # plt.savefig(output_prefix+'_buildTimes.png', bbox_inches="tight")
    
    plt.clf()



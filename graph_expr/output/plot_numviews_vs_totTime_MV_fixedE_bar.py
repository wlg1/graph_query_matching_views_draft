import csv, os
import matplotlib.pyplot as plt
from os import listdir
from os.path import isfile, join

#tunable params
# DG = 'email_lb20_'
DG = 'bs_lb20_v1_350K_nodes_'
querynums = [12,14,15,18,19]
# querynums = list(range(12,13))
use_logScale = False

# timeDict = {}  # qryNum : {algo : timeByLabel } 
# for querynum in querynums:   
#     timeDict[querynum] = {}
#     for algo in algoNames:
#         timeDict[querynum][algo] = [] #timeByLabel is a list
        
for querynum in querynums:    
    viewfiles = [f for f in listdir(os.getcwd()) if isfile(join(os.getcwd(), f))
                and '_MVFltSim_newcols.csv' in f
                and 'q'+str(querynum)+'_' in f]
    minDE = 100
    maxDE = 0
    for file in viewfiles:
        numviews = int(file.split('_')[-3].replace('views',''))
        if numviews < minDE:
            minDE = numviews
        if numviews > maxDE:
            maxDE = numviews
    numDElist = list(range(minDE,maxDE+1))

    # algoNames = ['lb20_m_2Eviews_q'+str(querynum)+'_']
    algoNames = ['lb20_m_2Eviews_q'+str(querynum)+'_', 
                  'lb20_m_2Eviews_q'+str(querynum)+'_FltSim']
    timeDict = {}
    # filtTimeDict = {}
    prefiltTimeDict = {}
    simfiltTimeDict = {}
    buildTimeDict = {}
    joinTimeDict = {}
    totNodesMV = {}
    for algo in algoNames:
        timeDict[algo] = [] 
        # filtTimeDict[algo] = [] 
        prefiltTimeDict[algo] = [] 
        simfiltTimeDict[algo] = [] 
        buildTimeDict[algo] = [] 
        joinTimeDict[algo] = [] 
        totNodesMV[algo] = [] 

    overlaps = []
    desc_overlaps = []
    for lblIndex, numDE in enumerate(numDElist): #loop thru x-axis. each is in diff file
        algo = algoNames[0]
        filename = DG + algo + str(numDE) + 'views_MVFltSim_newcols.csv'
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
                
                totNodesMV[algo].append(row[-5].replace(',',''))
    
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
                prefiltTimeDict[algo].append(float(row[2]))
                simfiltTimeDict[algo].append(float(row[3]))
                # filtTimeDict[algo].append(float(row[4]))
                buildTimeDict[algo].append(float(row[6]))
                joinTimeDict[algo].append(float(row[7]))
                timeDict[algo].append(float(row[8]))
                # timeDict[int(row[0])][algo].append(float(row[8]))
                # timeDict[int(row[0])][algo].append(float(row[4]))
                # timeDict[int(row[0])][algo].append(float(row[6]))
                
        edgeInfoFN = [f for f in listdir(os.getcwd()) if isfile(join(os.getcwd(), f))
            and '_edgeInfo.txt' in f
            and 'q'+str(querynum) in f
            and str(numDE)+'views' in f][0]
        file = open(edgeInfoFN, 'r')
        # totNumLine = [line for line in file.readlines() if 'tot num' in line]
        numEcov = 0
        for line in file:
            if 'tot num' in line and 'desc' not in line:
                totNumVE = line.split(' ')[-1].replace('\n','')
            if '(' in line:
                line = line.replace('\n','')
                splitted_line = line.split(':')
                if int(splitted_line[-1]) != 0:
                    numEcov += 1
            if 'avg num overlapping desc' in line:
                line = line.replace('\n','')
                splitted_line = line.split(':')
                desc_overlaps.append( round(float(splitted_line[-1].replace(' ','')), 2) )
        overlap_stat = round(int(totNumVE) / numEcov,2)
        overlaps.append(overlap_stat)
        
    import pandas as pd
    import matplotlib
    
    algo = algoNames[0]
    algo2 = algoNames[1]
    
    # use same prefiltTime for all x-axis ticks
    prefiltTimeDict[algo] = [ prefiltTimeDict[algo][0] ] * len(prefiltTimeDict[algo])
    
    prefltBar = prefiltTimeDict[algo].copy()
    simfltBar = simfiltTimeDict[algo].copy()
    buildBar = buildTimeDict[algo].copy()
    joinBar = joinTimeDict[algo].copy()
    
    # prefltBar.append(prefiltTimeDict[algo2][0])
    prefltBar.append(prefiltTimeDict[algo][0] )
    
    simfltBar.append(simfiltTimeDict[algo2][0])
    buildBar.append(buildTimeDict[algo2][0])
    joinBar.append(joinTimeDict[algo2][0])
    
    df = pd.DataFrame(dict(
    pre = prefltBar,
    sim = simfltBar,
    B= buildBar,
    C= joinBar))
        
    fig = plt.figure(figsize=(20, 10))
    font = {'weight' : 'bold',
        'size'   : 16}
    matplotlib.rc('font', **font)
    
    # xAxis =  [str(x)+'views, \n'+ str(overlaps[i]) +' OVL E\n' + \
    #           str(desc_overlaps[i]) +' OVL DE\n' \
    #               for i, x in enumerate(numDElist)]
    xAxis =  [str(x)+'views, \n'+ str(overlaps[i]) +' OVL E\n' + \
              str(desc_overlaps[i]) +' OVL DE\n' \
              + str(totNodesMV[algo][i]) + ' nodesViews' \
                  for i, x in enumerate(numDElist)]
    xAxis.append('FLTSIM')
    view_bar_list = [plt.bar(xAxis, df.pre, align='center', width= 0.2, label='prefiltTime', \
                             hatch='x'),
                plt.bar(xAxis, df.sim, align='center', width= 0.2, label='simfiltTime', \
                        bottom = df.pre, hatch='o'),
               plt.bar(xAxis, df.B, align='center', width= 0.2, label='buildTime', \
                       bottom = df.pre+ df.sim, hatch = '//'),
               plt.bar(xAxis, df.C, align='center', width= 0.2, label='joinTime', \
                       bottom = df.pre+ df.sim+ df.B, hatch = '-')]
    
    plt.ylabel('Time (secs)')
    plt.title('#views vs Time (secs)')
    plt.legend(bbox_to_anchor=(1, 1), loc='upper left')
    # plt.margins(y=0.5, tight=True)
    y_min, y_max = plt.gca().get_ylim()
    plt.gca().set_ylim([y_min, y_max * 1.05])
    
    totTimes = []
    for i in range(len(timeDict[algo])):
        tot = prefiltTimeDict[algo][i] + simfiltTimeDict[algo][i] + \
            buildTimeDict[algo][i] + joinTimeDict[algo][i]
        totTimes.append(tot)
            
    font = {'size'   : 18}
    matplotlib.rc('font', **font)
    xlocs, xlabs = plt.xticks()
    # for i, v in enumerate(timeDict[algo]):
    for i, v in enumerate(totTimes):
        plt.text(xlocs[i] - 0.1, v * 1.05, str(round(v,2)))
    i += 1
    v = timeDict[algo2][0]
    plt.text(xlocs[i] * 0.91, v * 0.9, str(v))
    
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
    tot = prefiltTimeDict[algo][0] + simfiltTimeDict[algo2][0] + \
        buildTimeDict[algo2][0] + joinTimeDict[algo2][0]
    FLTSIM_tot = [tot] * (len(totTimes)+1)
    
    df2 = pd.DataFrame(dict(
    FLTSIM = FLTSIM_tot))
        
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
    df2['FLTSIM'].plot(color='red', linestyle='dotted')
    df['views'].plot(color='purple')
    plt.title('#views vs totTime (secs)')
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



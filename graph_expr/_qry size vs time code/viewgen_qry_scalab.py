import networkx as nx
from random import choice, randint
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import os, itertools
from random import sample

#OUTPUT: in same dir, a new .qry and img

qrys_to_combine = [7]  #or rand choice
input_file = 'inst_lb20_cyc_m.qry'
num_new_edges = 1
qry_intv_len = 2
minE = 3  # min E for reduced graph
num_desc = None  #None or an int

min_num_Vedges = 1
max_num_Vedges = min_num_Vedges
num_queries = 1
min_overlap_thres = 1  #avg overlap of edges needed
max_overlap_thres = 1

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'
    
input_path = 'queries/' + input_file
f = open(input_path, "r")
templates = []
G = nx.DiGraph()
#create graph objs for each qry template
while True:
    line = f.readline().replace('\n','')
    lineAsList = line.split(' ')
    if lineAsList[0] == 'q':
        qryTempID = lineAsList[2]
        if qryTempID != '0':
            templates.append(G)  #does not add by ref
        G = nx.DiGraph() 
    elif lineAsList[0] == 'v':
        nodeID =  lineAsList[1] + '_' + qryTempID
        nodeLabel = lineAsList[2]
        G.add_nodes_from([
        (nodeID, {"label": nodeLabel}) ])
    elif lineAsList[0] == 'e':
        head = lineAsList[1] + '_' + qryTempID
        tail = lineAsList[2] + '_' + qryTempID
        edgeLabel = lineAsList[3]
        # edgeLabel = '0'
        G.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
    if len(line) == 0:
        templates.append(G)
        break
    
# """
#combine templates to create larger queries
#for this script, don't overlap nodes + edges (do that in another)
#add X edges to connect views. no cycles.
#rand choose X b/w 2 and # edges in smallest view

F = templates[qrys_to_combine[0] - 6]
for qrynum in qrys_to_combine[1:]:
    F = nx.compose(F, templates[qrynum - 6])

# num_new_edges = randint(1,1)
# for i in range(num_new_edges):
    
# add new edges until combined graph is conn and no cycles:
while True:
    # if nx.is_connected(F.to_undirected()):
    if nx.is_connected(F.to_undirected()) and len(F.edges()) > num_new_edges:
        break
    randQryNum_1 = randint(0, len(qrys_to_combine)-1)
    while True:
        randQryNum_2 = randint(0, len(qrys_to_combine)-1)
        if randQryNum_2 != randQryNum_1:
            break
    G = templates[randQryNum_1]
    H = templates[randQryNum_2]
    head = choice(list(G.nodes))
    tail = choice(list(H.nodes))
    tempF = F.copy()
    if (head, tail) not in F.edges():
        #edgeLabel = str(randint(0,1))
        edgeLabel = '0'
        tempF.add_edge(head, tail, label = edgeLabel, 
                   color = get_edge_color(edgeLabel))
    if list(nx.simple_cycles(F)):
        continue #new qry is cyclic, so dont use
    else:
        F = tempF
# """

#turn some edges into desc
if num_desc != None:
    edges = list(F.edges()).copy() #must make copy to loop thru all b/c changing list as loop thru it, so indices differ each time call list(F.edges())
    for edgeNum in range(num_desc):
        randE = edges[edgeNum]
        F.remove_edge(randE[0], randE[1])
        edgeLabel = '1'
        head = randE[0]
        tail = randE[1]
        F.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))

#get graphs, each with diff num of edges
qrys_diff_numE = [F]
currNumE_toGet = len(F.edges()) - qry_intv_len
q = 0 
while True and q < 500:
    cand_qry = F.copy()
    randE = choice(list(F.edges()))
    # if F.get_edge_data(randE[0], randE[1])['label'] == '1':
    #     continue
    cand_qry.remove_edge(randE[0], randE[1])
    for vert in list(cand_qry.degree()):
        if vert[1] == 0:
            cand_qry.remove_node(vert[0])
    if nx.is_connected(cand_qry.to_undirected()):
        F = cand_qry
    #separate from above so smaller graph carries over to next iter
    if len(cand_qry.edges()) == currNumE_toGet and nx.is_connected(cand_qry.to_undirected()):    
        qrys_diff_numE.append(cand_qry)
        currNumE_toGet -= qry_intv_len
        currNumE_toGet = max(minE, currNumE_toGet)
        if currNumE_toGet <= minE:
            break
    q+=1

qrys_diff_numE.reverse()
filenames = []
for F in qrys_diff_numE:
    #output queries in format readable by patternMatch algos
    prefix = input_file.replace('inst_','').replace('.qry','')
    outFN = prefix+"_combQrys_"
    for qrynum in qrys_to_combine:
        outFN = outFN+"_"+str(qrynum)
    
    outFN = outFN + '_'+str(max_num_Vedges)+'Eviews'
    if min_overlap_thres != None:
        outFN = outFN + '_ovl_'+ str(min_overlap_thres) + '_' + str(max_overlap_thres)
    outFN = outFN + "_" + str(num_desc) + "DE" 
    
    #x-axis to vary
    outFN = outFN + "_" + str(len(F.edges())) + "numE"
    
    filenames.append(outFN)
    
    out_file = open(outFN+".qry", "w")
    out_file.write("q # 0\n")
    nodes = nx.get_node_attributes(F,'label')
    for nodeID, vertex in enumerate(nodes.keys()):
        out_file.write("v " + str(nodeID) + " " + nodes[vertex]  + '\n' )
    edges = nx.get_edge_attributes(F,'label')
    for e in edges:
        head = str(list(nodes.keys()).index(e[0]))
        tail = str(list(nodes.keys()).index(e[1]))
        out_file.write("e " + head + " " + tail + " " + edges[e] + '\n' )
            
    out_file.close()
    
    fig = plt.figure()
    ax1 = plt.subplot2grid((1, 1), (0, 0))
    pos = nx.spring_layout(F, k=1, iterations=5)
    node_labels = nx.get_node_attributes(F, 'label')
    edges = nx.get_edge_attributes(F,'label')
    colors = [F[u][v]['color'] for u,v in edges]
    # nx.draw(F, pos, with_labels=True,node_size=400, 
    #         labels = node_labels, edge_color=colors)
    nx.draw(F, pos, with_labels=True,node_size=400, 
            edge_color=colors)
    edge_labels = nx.get_edge_attributes(F, 'label')
    nx.draw_networkx_edge_labels(F, pos, edge_labels)
    plt.savefig(outFN+".png")

###### generate views
"""
to have comparable views: first pick views for smallest query, and then 
use the same ones for next smallest but add views that cover that, then 
carry those over to next, repeat until get to largest query. 
overlap for each query should be the same (not exactly, but approx)"""

#OUTPUT: in same dir, a folder of qry, views, infotxt, imgs

#store both directed and undirected graphs. rand get subgraphs of size X
#choose how many subgraphs of sizes 2 and 3 you want
#at end, record how many edges overlap, and % of those over total # edges

views = []
views_as_edgeLists = []
covered_edges = []
for Gnum, G in enumerate(qrys_diff_numE):
    unG = G.to_undirected()
    all_connected_subgraphs = []
    node_labels = nx.get_node_attributes(G,'label')
    edge_labels = nx.get_edge_attributes(G,'label')
    
    # here we ask for all connected subgraphs that have at least 2 nodes AND have less nodes than the input graph
    for num_Vedges in range(min_num_Vedges, max_num_Vedges+1):
        subgraphs = [(unG.edge_subgraph(selected_Es), G.edge_subgraph(selected_Es))  for selected_Es in itertools.combinations(G.edges(), num_Vedges)]
        for SG_pair in subgraphs:
            undirSG = SG_pair[0]
            dirSG = SG_pair[1]
            if nx.is_connected(undirSG):  #is_conn doesn't work for dir G
                newSG = nx.DiGraph()
                for node in dirSG.nodes():
                    newSG.add_node(node)
                    newSG.add_nodes_from([
                        (node, {"label": node_labels[node]}) ])
                for edge in dirSG.edges():
                    head = edge[0]
                    tail = edge[1]
                    edgeLabel = edge_labels[edge]
                    newSG.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
                if newSG not in all_connected_subgraphs and len(newSG.edges()) == num_Vedges:
                    all_connected_subgraphs.append(newSG)

    if not all_connected_subgraphs:
        continue
    
    if min_overlap_thres == None:
        #randomly choose views until all edges covered
        goFlag = True
        edges = list(G.edges())
        while goFlag:
            vw = sample(all_connected_subgraphs, 1)[0]
            if vw not in views:
                views.append(vw)
                # all_connected_subgraphs.remove(vw)
                covered_edges += list(vw.edges())
                if (all(x in covered_edges for x in edges)) or not all_connected_subgraphs:
                    goFlag = False
    else:
        #choose edges until avg total overlap is > overlap_threshold
        pass_overlap_thres = True
        edges = list(G.edges())
        escCounter = 0
        # while goFlag and escCounter < 20:
        while True:
            if not all_connected_subgraphs:
                break
            
            vw = sample(all_connected_subgraphs, 1)[0]
            view_as_eList = list(vw.edges())
            view_as_eList.sort()
            if view_as_eList not in views_as_edgeLists:
                temp_views = views.copy()
                temp_covered_edges = covered_edges.copy()
                temp_views.append(vw)
                temp_covered_edges += list(vw.edges())
            else:
                all_connected_subgraphs.remove(vw)
                continue
                    
            num_edges = {} # (x,y) : num overlaps
            tot_num_overlap = 0
            for e in edges:
                num_overlap = 0
                for vw in temp_views:
                    if e in list(vw.edges()):
                        num_overlap += 1
                num_edges[e] = num_overlap
                tot_num_overlap += num_overlap
                avg_overlap = tot_num_overlap / len(edges)
                
            if (all(x in covered_edges for x in edges)) and avg_overlap > min_overlap_thres:
                break
            
            if avg_overlap > max_overlap_thres:
                continue
            else:
                views.append(vw)
                views_as_edgeLists.append(view_as_eList)
                covered_edges += list(vw.edges())
                covered_edges = list(set(covered_edges))
                all_connected_subgraphs.remove(vw)
                
            # escCounter += 1
            # if escCounter > len(all_connected_subgraphs)*30:
            #     pass_overlap_thres = False
            #     break
            
        # if not pass_overlap_thres:
        #     continue
    
    #outputviews of this query set based on input filename
    output_prefix = filenames[Gnum]
    if not os.path.exists(output_prefix):
        os.makedirs(output_prefix)
    outFN = output_prefix + '/' + output_prefix
    out_file = open(outFN + ".vw", "w")
    
    for q, qry in enumerate(views):
        if max_num_Vedges > 1:
            fig = plt.figure()
            pos = nx.spring_layout(qry)
            node_labels = nx.get_node_attributes(qry, 'label')
            edges = nx.get_edge_attributes(qry,'label')
            colors = [qry[u][v]['color'] for u,v in edges]
            Ncolors = ['skyblue' for n in list(qry.nodes())]
            nx.draw(qry, pos, with_labels=True,node_size=800, font_weight = 'bold',
                    labels = node_labels, node_color = Ncolors, edge_color=colors)
            edge_labels = nx.get_edge_attributes(qry, 'label')
            nx.draw_networkx_edge_labels(qry, pos, edge_labels, font_weight = 'bold')
            plt.savefig(outFN+"_v"+ str(q) + ".jpg")
        
        out_file.write("q # " + str(q) + '\n')
        nodes = nx.get_node_attributes(qry,'label')
        for nodeID, vertex in enumerate(nodes.keys()):
            out_file.write("v " + str(nodeID) + " " + nodes[vertex]  + '\n' )
        edges = nx.get_edge_attributes(qry,'label')
        for e in edges:
            head = str(list(nodes.keys()).index(e[0]))
            tail = str(list(nodes.keys()).index(e[1]))
            out_file.write("e " + head + " " + tail + " " + edges[e] + '\n' )
            
    out_file.close()
    
    #tot num overlapping edges, which edges overlap and how many times
    num_edges = {} # (x,y) : num overlaps
    edges = list(G.edges())
    tot_num_overlap = 0
    for e in edges:
        num_overlap = 0
        for vw in views:
            if e in list(vw.edges()):
                num_overlap += 1
        num_edges[e] = num_overlap
        tot_num_overlap += num_overlap
    
    
    out_file = open(outFN + "_edgeInfo.txt", "w")
    out_file.write("num views: " + str(len(views)) + "\n")
    out_file.write("tot num overlapping edges: " + str(tot_num_overlap) + "\n")
    out_file.write("avg num overlapping edges: " + str(tot_num_overlap / len(edges)) + "\n")
    out_file.write("edge : num overlaps\n")
    for edge, num_overlaps in num_edges.items():
        out_file.write(repr(edge) + " : " + str(num_overlaps)  + "\n")    
    out_file.close()
    
    #output queries in format readable by patternMatch algos
    out_file = open(outFN + ".qry", "w")
    out_file.write("q # 0" + '\n')
    nodes = nx.get_node_attributes(G,'label')
    for nodeID, vertex in enumerate(nodes.keys()):
        out_file.write("v " + str(nodeID) + " " + nodes[vertex]  + '\n' )
    edges = nx.get_edge_attributes(G,'label')
    for e in edges:
        head = str(list(nodes.keys()).index(e[0]))
        tail = str(list(nodes.keys()).index(e[1]))
        out_file.write("e " + head + " " + tail + " " + edges[e] + '\n' )
    out_file.close()
    




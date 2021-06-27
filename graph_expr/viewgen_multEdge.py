import networkx as nx
from random import sample
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import itertools
import os

#OUTPUT: in same dir, a folder of qry, views, infotxt, imgs

#store both directed and undirected graphs. rand get subgraphs of size X
#choose how many subgraphs of sizes 2 and 3 you want
#at end, record how many edges overlap, and % of those over total # edges

#user tunable parameters
min_num_Vedges = 2
max_num_Vedges = min_num_Vedges
num_queries = 1
overlap_thres = None  #avg overlap of edges needed

input_file = 'inst_lb20_acyc_m_q5'

# input_path = 'queries/' + input_file + '.qry'
input_path = input_file + '.qry'
output_prefix = input_file.replace('inst_', '') + '_'+str(max_num_Vedges)+'Eviews'
if overlap_thres != None:
    output_prefix = output_prefix + '_ovl_' + str(overlap_thres)
if not os.path.exists(output_prefix):
    os.makedirs(output_prefix)
output_name = output_prefix + '/' + output_prefix
f = open(input_path, "r")

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'

for querynum in range(num_queries):
    node_labels = {} #id : label
    edge_labels = {} #(x,y) : label
    templates = []
    unTemplates = []
    G = nx.DiGraph()
    unG = nx.Graph()
    #create graph objs for each qry template
    while True:
        line = f.readline().replace('\n','')
        lineAsList = line.split(' ')
        if lineAsList[0] == 'q':
            currQ = int(lineAsList[2])
        if currQ == querynum:
            if lineAsList[0] == 'v':
                nodeID =  lineAsList[1]
                nodeLabel = lineAsList[2]
                node_labels[nodeID] = nodeLabel
                G.add_nodes_from([
                (nodeID, {"label": nodeLabel}) ])
                unG.add_nodes_from([
                (nodeID, {"label": nodeLabel}) ])
            elif lineAsList[0] == 'e':
                head = lineAsList[1]
                tail = lineAsList[2]
                edgeLabel = lineAsList[3]
                edge = (head, tail)
                edge_labels[edge] = edgeLabel
                G.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
                unG.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
        if currQ > querynum or len(line) == 0:
            templates.append(G)
            unTemplates.append(unG)
            break
    
    all_connected_subgraphs = []
    
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
    
    if overlap_thres == None:
        #randomly choose views until all edges covered
        covered_edges = []
        views = []
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
        covered_edges = []
        views = []
        goFlag = True
        edges = list(G.edges())
        escCounter = 0
        # while goFlag and escCounter < 20:
        while goFlag:
            vw = sample(all_connected_subgraphs, 1)[0]
            if vw not in views:
                views.append(vw)
                # all_connected_subgraphs.remove(vw)
                covered_edges += list(vw.edges())
                if (all(x in covered_edges for x in edges)) or not all_connected_subgraphs:
                    goFlag = False
                    
            num_edges = {} # (x,y) : num overlaps
            tot_num_overlap = 0
            for e in edges:
                num_overlap = 0
                for vw in views:
                    if e in list(vw.edges()):
                        num_overlap += 1
                num_edges[e] = num_overlap
                tot_num_overlap += num_overlap
                avg_overlap = tot_num_overlap / len(edges)
                
            if avg_overlap < overlap_thres:
                goFlag = True
                
            escCounter += 1
            if escCounter > len(all_connected_subgraphs)*30:
                pass_overlap_thres = False
                break
            
        if not pass_overlap_thres:
            continue
    
    #outputviews of this query set
    # outFN = output_name + "_q" + str(querynum+6)
    outFN = output_name
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
    out_file.write("q # " + str(querynum) + '\n')
    nodes = nx.get_node_attributes(G,'label')
    for nodeID, vertex in enumerate(nodes.keys()):
        out_file.write("v " + str(nodeID) + " " + nodes[vertex]  + '\n' )
    edges = nx.get_edge_attributes(G,'label')
    for e in edges:
        head = str(list(nodes.keys()).index(e[0]))
        tail = str(list(nodes.keys()).index(e[1]))
        out_file.write("e " + head + " " + tail + " " + edges[e] + '\n' )
    out_file.close()
    
f.close()
    
# A = (unTemplates[0].subgraph(c) for c in nx.connected_components(unTemplates[0]))
# x = list(A)[0]

# input_file = 'inst_lb20_cyc_m.qry'
# prefix = 'lb20_cyc_m'
# input_path = 'queries/' + input_file
# output_prefix = prefix + '_1Eviews'
# output_name = output_prefix + '/' + output_prefix
# f = open(input_path, "r")
# num_queries = 14

# for querynum in range(num_queries):
#     node_labels = {} #id : label
#     edges = []
#     edge_labels = {} #(x,y) : label
#     while True:
#         line = f.readline().replace('\n','')
#         lineAsList = line.split(' ')
#         if lineAsList[0] == 'q':
#             currQ = int(lineAsList[2])
#         if currQ == querynum:
#             if lineAsList[0] == 'v':
#                 node_labels[lineAsList[1]] = lineAsList[2]
#             elif lineAsList[0] == 'e':
#                 edge = (lineAsList[1], lineAsList[2])
#                 edges.append(edge)
#                 edge_labels[edge] = lineAsList[3]
#         if currQ > querynum or len(line) == 0:
#             break
            
#     #output queries in format readable by patternMatch algos
#     out_file = open(output_name + "_q" + str(querynum+6) + ".qry", "w")
#     out_file.write("q # 0\n")
#     for nodeID in range(len(node_labels)):
#         out_file.write("v " + str(nodeID) + " " + node_labels[str(nodeID)]  + '\n' )
#     for e in edges:
#         out_file.write("e " + e[0] + " " + e[1] + " " + edge_labels[e] + '\n' )
            
#     out_file.close()
    
#     #let num_overlap_E = round(20% of total number of edges) of edges overlap twice. 
#     #pick nodes with degree >= 2. rand pick 3 of their edges. use one as overlap, 
#     #   the other two as unique in their views.
#     #repeat this num_overlap_E times
#     #ISSUE: not all queries will be able to do this.
    
#     views = []
#     num_overlap_E = round(0.2*len(edges))
#     for e in range(num_overlap_E):
#         #combine 1-edge graphs into 2. 
#         #pick 3 random edges. check if they share the same node (connected). if not, pick again
        
#         goFlag = True
#         while goFlag:
#             sampEs = random.sample(edges, 3)  #no repeats
#             edge1 = sampEs[0]
#             edge2 = sampEs[1]
#             edge3 = sampEs[2]
#             for node in edge2:
#                 if node in edge1 and node in edge3:
#                     #create 2 views out of these 
#                     views.append([edge1, edge2])
#                     views.append([edge3, edge2])
#                     goFlag = False
#                     break
#             for node in edge1:
#                 if node in edge2 and node in edge3:
#                     views.append([edge2, edge1])
#                     views.append([edge3, edge1])
#                     goFlag = False
#                     break
#             for node in edge3:
#                 if node in edge1 and node in edge2:
#                     goFlag = False
#                     views.append([edge1, edge3])
#                     views.append([edge2, edge3])
#                     break
#             print()
                    

    
#     #get query edge. its old nodes are now 0 and 1 in 
#     #new view. map old query nodeIDs to new view nodeIDs
    
#     #output templates used as views of this query set
#     out_file = open(output_name + "_q" + str(querynum+6) + ".vw", "w")
#     for q, view in enumerate(edges):
#         out_file.write("q # " + str(q) + '\n')
#         for nodeID, vertex in enumerate(view):
#             out_file.write("v " + str(nodeID) + " " + node_labels[vertex]  + '\n' )
#         out_file.write("e 0 1 " + edge_labels[view] + '\n' )
            
#     out_file.close()




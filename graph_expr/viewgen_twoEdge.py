import networkx as nx
from random import choice, randint, sample
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import itertools


#store both directed and undirected graphs. rand get subgraphs of size X
#choose how many subgraphs of sizes 2 and 3 you want
#at end, record how many edges overlap, and % of those over total # edges

input_file = 'inst_lb20_cyc_m.qry'
prefix = 'lb20_cyc_m'
input_path = 'queries/' + input_file
output_prefix = prefix + '_2to3Eviews'
output_name = output_prefix + '/' + output_prefix
f = open(input_path, "r")
num_queries = 14

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
    # for nb_nodes in range(2, G.number_of_nodes()):
    for nb_nodes in range(3, 4):
        for SG in (unG.subgraph(selected_nodes) for selected_nodes in itertools.combinations(unG, nb_nodes)):
            if nx.is_connected(SG) and SG not in all_connected_subgraphs:
                dirSG = G.subgraph(SG.nodes())
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
                all_connected_subgraphs.append(SG)
                
    if not all_connected_subgraphs:
        continue
                
    #randomly choose views until all edges covered
    covered_edges = []
    views = []
    goFlag = True
    edges = list(unG.edges())
    while goFlag:
        vw = sample(all_connected_subgraphs, 1)[0]
        if vw not in views:
            views.append(vw)
            covered_edges += list(vw.edges())
            if (all(x in covered_edges for x in edges)):
                goFlag = False
    
    #outputviews of this query set
    outFN = output_name + "_q" + str(querynum+6)
    out_file = open(outFN + ".vw", "w")
    for q, qry in enumerate(views):
        fig = plt.figure()
        ax1 = plt.subplot2grid((1, 1), (0, 0))
        pos = nx.spring_layout(qry)
        node_labels = nx.get_node_attributes(qry, 'label')
        edges = nx.get_edge_attributes(qry,'label')
        colors = [qry[u][v]['color'] for u,v in edges]
        nx.draw(qry, pos, with_labels=True,node_size=400, 
                labels = node_labels, edge_color=colors)
        edge_labels = nx.get_edge_attributes(qry, 'label')
        nx.draw_networkx_edge_labels(qry, pos, edge_labels)
        plt.savefig(outFN+"_v"+ str(q) + ".png")
        
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
    
        

    #num overlapping edges
            
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




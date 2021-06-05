import networkx as nx
from random import choice, randint
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt



#store both directed and undirected graphs. rand get subgraphs of size X
#choose how many subgraphs of sizes 2 and 3 you want
#at end, record how many edges overlap, and % of those over total # edges

input_file = 'inst_lb20_cyc_m.qry'
input_path = 'queries/' + input_file
f = open(input_path, "r")

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'

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
        qryTempID = lineAsList[2]
        if qryTempID != '0':
            templates.append(G)  #does not add by ref
            unTemplates.append(unG)
        G = nx.DiGraph() 
        unG = nx.Graph()
    elif lineAsList[0] == 'v':
        nodeID =  lineAsList[1] + '_' + qryTempID
        nodeLabel = lineAsList[2]
        node_labels[nodeID] = nodeLabel
        G.add_nodes_from([
        (nodeID, {"label": nodeLabel}) ])
        unG.add_nodes_from([
        (nodeID, {"label": nodeLabel}) ])
    elif lineAsList[0] == 'e':
        head = lineAsList[1] + '_' + qryTempID
        tail = lineAsList[2] + '_' + qryTempID
        edgeLabel = lineAsList[3]
        edge = (head, tail)
        edge_labels[edge] = edgeLabel
        G.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
        unG.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
    if len(line) == 0:
        templates.append(G)
        unTemplates.append(unG)
        break

import itertools

G = unTemplates[0]
all_connected_subgraphs = []

# here we ask for all connected subgraphs that have at least 2 nodes AND have less nodes than the input graph
# for nb_nodes in range(2, G.number_of_nodes()):
for nb_nodes in range(3, 4):
    for SG in (G.subgraph(selected_nodes) for selected_nodes in itertools.combinations(G, nb_nodes)):
        if nx.is_connected(SG) and SG not in all_connected_subgraphs:
            newSG = nx.DiGraph()
            for node in SG.nodes():
                newSG.add_node(node)
                newSG.add_nodes_from([
                    (node, {"label": node_labels[node]}) ])
            for edge in SG.edges():
                head = edge[0]
                tail = edge[1]
                edgeLabel = edge_labels[edge]
                newSG.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
            all_connected_subgraphs.append(SG)

            
views = [all_connected_subgraphs[0]]
#outputviews of this query set
out_file = open("test.vw", "w")
for q, qry in enumerate(views):
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




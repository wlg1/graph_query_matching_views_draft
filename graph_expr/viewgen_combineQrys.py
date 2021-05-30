import networkx as nx
from random import choice, randint
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pdb

input_file = 'inst_lb20_cyc_m.qry'
input_path = 'queries/' + input_file
f = open(input_path, "r")

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'

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
        G.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
    if len(line) == 0:
        templates.append(G)
        break
    
#combine templates to create larger queries
#for this script, don't overlap nodes + edges (do that in another)
#add X edges to connect views. no cycles.
#rand choose X b/w 2 and # edges in smallest view

num_new_qrys = randint(1,1)
new_qrys = []
new_qrys_views = []
for i in range(num_new_qrys):
    #rand choose num templates and which to use
    G = templates[0]
    H = templates[1]
    new_qrys_views.append(G)
    new_qrys_views.append(H)
    F = nx.compose(G,H)
    
    num_new_edges = randint(1,1)
    for i in range(num_new_edges):
        head = choice(list(G.nodes))
        tail = choice(list(H.nodes))
        edgeLabel = str(randint(0,1))
        F.add_edge(head, tail, label = edgeLabel, color = get_edge_color(edgeLabel))
    
    new_qrys.append(F)

#output queries in format readable by patternMatch algos
out_file = open("temp0_temp1_v1.qry", "w")
for q, qry in enumerate(new_qrys):
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

fig = plt.figure()
ax1 = plt.subplot2grid((1, 1), (0, 0))
pos = nx.spring_layout(F)
node_labels = nx.get_node_attributes(F, 'label')
edges = nx.get_edge_attributes(F,'label')
colors = [F[u][v]['color'] for u,v in edges]
nx.draw(F, pos, with_labels=True,node_size=400, 
        labels = node_labels, edge_color=colors)
edge_labels = nx.get_edge_attributes(F, 'label')
nx.draw_networkx_edge_labels(F, pos, edge_labels)
plt.savefig("temp0_temp1_v1.png")

#output templates used as views of this query set
out_file = open("temp0_temp1_v1.vw", "w")
for q, qry in enumerate(new_qrys_views):
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

F = templates[0]
fig = plt.figure()
ax1 = plt.subplot2grid((1, 1), (0, 0))
pos = nx.spring_layout(F)
node_labels = nx.get_node_attributes(F, 'label')
edges = nx.get_edge_attributes(F,'label')
colors = [F[u][v]['color'] for u,v in edges]
nx.draw(F, pos, with_labels=True,node_size=400, 
        labels = node_labels, edge_color=colors)
edge_labels = nx.get_edge_attributes(F, 'label')
nx.draw_networkx_edge_labels(F, pos, edge_labels)
plt.savefig("view0.png")

F = templates[1]
fig = plt.figure()
ax1 = plt.subplot2grid((1, 1), (0, 0))
pos = nx.spring_layout(F)
node_labels = nx.get_node_attributes(F, 'label')
edges = nx.get_edge_attributes(F,'label')
colors = [F[u][v]['color'] for u,v in edges]
nx.draw(F, pos, with_labels=True,node_size=400, 
        labels = node_labels, edge_color=colors)
edge_labels = nx.get_edge_attributes(F, 'label')
nx.draw_networkx_edge_labels(F, pos, edge_labels)
plt.savefig("view1.png")





import networkx as nx
import pdb

input_file = 'inst_lb20_cyc_m.qry'
input_path = 'queries/' + input_file
f = open(input_path, "r")

templates = []
G = nx.Graph()
#create graph objs for each qry template
while True:
    line = f.readline().replace('\n','')
    lineAsList = line.split(' ')
    if lineAsList[0] == 'q':
        if lineAsList[2] != '0':
            templates.append(G)  #does not add by ref
        G = nx.Graph() 
    elif lineAsList[0] == 'v':
        nodeID =  int(lineAsList[1])
        nodeLabel = lineAsList[2]
        G.add_nodes_from([
        (nodeID, {"label": nodeLabel}) ])
    elif lineAsList[0] == 'e':
        head = int(lineAsList[1])    
        tail = int(lineAsList[2])
        edgeLabel = lineAsList[3]
        G.add_edge(head,tail, label = edgeLabel)
    if len(line) == 0:
        templates.append(G)
        break
    
#combine templates to create larger queries
#for this script, don't overlap nodes + edges (do that in another)
#add X edges to connect views. no cycles.
#rand choose X b/w 2 and # edges in smallest view



pdb.set_trace()

#output queries in format readable by patternMatch algos
#nx.get_node_attributes(G,'label')
#nx.get_edge_attributes(templates[0],'label')
      




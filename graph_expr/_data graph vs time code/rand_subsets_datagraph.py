import networkx as nx
import random, math

data_graph = 'Email_lb20'

input_path = 'data_graphs/' + data_graph + ".dag"
G = nx.DiGraph()
with open(input_path, 'r') as input_file:
    for line in input_file:
        line = line.replace('\n','')
        lineAsList = line.split(' ')
        if lineAsList[0] == 'v':
            nodeID =  lineAsList[1]
            nodeLabel = lineAsList[2]
            G.add_nodes_from([(nodeID, {"label": nodeLabel}) ])
        elif lineAsList[0] == 'e':
            head = lineAsList[1]
            tail = lineAsList[2]
            G.add_edge(head,tail)

#nx.is_connected(G2)

new_graphs = []
newNodes = []
startingNumNodes = 60000  
nodeCands = range(len(G.nodes()))
# for i in range(1,11):
for i in range(0, 6):
    # outFN = 'data_graphs/' + data_graph + "_" + str(10*i) + "K_nodes"
    outFN = 'data_graphs/' + data_graph + "_" + str(20*i +60) + "K_nodes"
    out_file = open(outFN + ".dag", "w")
    #numNodes = math.floor(len(G.nodes()) / 10) * i
    # numNodes = 10000
    if i == 0:
        numNodes = startingNumNodes
    else:
        numNodes = 20000
    randNodes = random.sample(nodeCands, numNodes)
    
    #rmv from nodeCands to prevent next loop from choosing them
    nodeCands = [x for x in nodeCands if x not in randNodes]
    
    randNodes = [str(node) for node in randNodes]
    newNodes += randNodes #ensures builds on top of prev nodes
    unsorted_subG = G.subgraph(newNodes)
    
    ### rename nodes and edges; req for dagHom to run
    nodesList = list(unsorted_subG.nodes())
    nodesList = [int(node) for node in nodesList]
    nodesList.sort()
    nodesList = [str(node) for node in nodesList]
    
    subG = nx.DiGraph()
    for node in unsorted_subG.nodes():
        new_node = str(nodesList.index(node))
        label = unsorted_subG.nodes[node]['label']
        subG.add_nodes_from([(new_node, {"label": label}) ])
    
    for edge in unsorted_subG.edges():
        head = str(nodesList.index(edge[0]))
        tail = str(nodesList.index(edge[1]))
        subG.add_edge(head,tail)
    ### END rename nodes and edges
    
    print(i, len(subG.nodes()), 'nodes', len(subG.edges()), 'edges')
    
    out_file.write("dag\n")
    for nodeID in subG.nodes():
        nodeID = str(nodeID)
        out_file.write("v " + nodeID + " " + subG.nodes[nodeID]['label'] + '\n' )
    for e in subG.edges():
        out_file.write("e " + e[0] + " " + e[1] + " 1" + '\n' )
        
    out_file.close()
        
    


    
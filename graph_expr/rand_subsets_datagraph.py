import networkx as nx
import random

# random.seed(100)

data_graph = 'am_lb24_v1'
# data_graph = 'bs_lb20_v1'
# data_graph = 'email_lb20'

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

intv = 50
startingNumNodes = 200000  
sNN_inK = startingNumNodes / 1000
nodeCands = range(len(G.nodes()))
new_graphs = []
newNodes = []
for i in range(0, 5):
    outFN = 'data_graphs/' + data_graph + "_" + str(int(intv*i + sNN_inK)) + "K_nodes"
    out_file = open(outFN + ".dag", "w")
    #numNodes = math.floor(len(G.nodes()) / 10) * i
    if i == 0:
        numNodes = startingNumNodes
    else:
        numNodes = intv * 1000
    #randNodes: new nodes to add on top of prev subset
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
        
    


    
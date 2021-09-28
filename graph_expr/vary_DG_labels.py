import networkx as nx
import random

data_graph = 'go_lb40_v1'
num_new_labels = 80

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
            
#for every label pick 50% of nodes and assign them completely 
#new label, this goes from 20 to 40 labels, 60, 80, etc

node_labels = nx.get_node_attributes(G, 'label')
node_labels = list(set(list(node_labels.values())))
node_labels.sort()

#get all nodes with that label
# print('old: label, #nodes')
sum_labeled_nodes = 0
label_to_nodes = {}  # 'label' : list of nodes w/ that label
for label in node_labels:
    label_to_nodes[label] = [x for x,y in G.nodes(data=True) if y['label']==label]
    # print(label, len(label_to_nodes[label]))
    # sum_labeled_nodes += len(label_to_nodes[label])
# print('total', sum_labeled_nodes)
    
#double total num of labels
#for each label, pick 50% of nodes and randassign new label
new_labels = [str(x) for x in range(int(num_new_labels/2)+ 1, num_new_labels+1)]
for label in node_labels:
    rand_nodes = random.sample(label_to_nodes[label], round(len(label_to_nodes[label])/2))
    for node in rand_nodes:
            new_node_label = random.choice(new_labels)
            attrs = {node : {"label" : new_node_label}} # {nodeID: {"label": new_value}}
            nx.set_node_attributes(G, attrs)

#check new population of labels
# print('NEW: label, #nodes')
# sum_labeled_nodes = 0
# node_labels = nx.get_node_attributes(G, 'label')
# node_labels = list(set(list(node_labels.values())))
# node_labels.sort()
# for label in node_labels:
#     nodes_w_label = [x for x,y in G.nodes(data=True) if y['label']==label]
#     print(label, len(nodes_w_label))
#     sum_labeled_nodes += len(nodes_w_label)
# print('total', sum_labeled_nodes)

prefix = data_graph.split('_')[0]
outFN = 'data_graphs/' + prefix + "_lb" + str(num_new_labels) + "_v1"
out_file = open(outFN + ".dag", "w")
out_file.write("dag\n")
for nodeID in G.nodes():
    nodeID = str(nodeID)
    out_file.write("v " + nodeID + " " + G.nodes[nodeID]['label'] + '\n' )
for e in G.edges():
    out_file.write("e " + e[0] + " " + e[1] + " 1" + '\n' )
    
out_file.close()    
        

    
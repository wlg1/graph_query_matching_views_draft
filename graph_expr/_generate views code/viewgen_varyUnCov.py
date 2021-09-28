import networkx as nx
from random import sample
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import itertools, os, math

#OUTPUT: in same dir, a folder of qry, views, infotxt, imgs
#choose how many subgraphs of sizes E you want
#at end, record how many edges overlap, and % of those over total # edges

#user tunable parameters
num_queries = 20
input_file = 'inst_lb20_m'

input_path = '../queries/' + input_file + '.qry'
f = open(input_path, "r")

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'

for querynum in range(0, num_queries):
    output_prefix = input_file.replace('inst_', '') + "_percCov"
    if not os.path.exists(output_prefix):
        os.makedirs(output_prefix)
    if num_queries > 1:
        outFN = output_name = output_prefix + '_q' + str(querynum)
    else:
        outFN = output_prefix
    output_path = output_prefix + '/' + outFN
    
    node_labels = {} #id : label
    edge_labels = {} #(x,y) : label
    templates = []
    unTemplates = []
    G = nx.DiGraph()
    unG = nx.Graph()  #undir b/c is_conn doesn't work for dir G
    
    #go thru files containing multiple qrys to find curr qry num
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

    cov_edges = []
    for edge in G.edges():
        cov_edges.append(edge)
        view = G.edge_subgraph(cov_edges).copy()
        num_cov = len(cov_edges)
        perc_cov = round(num_cov / len(G.edges()),2)
    
        fig = plt.figure()
        pos = nx.spring_layout(view)
        node_labels = nx.get_node_attributes(view, 'label')
        edges = nx.get_edge_attributes(view,'label')
        colors = [view[u][v]['color'] for u,v in edges]
        Ncolors = ['skyblue' for n in list(view.nodes())]
        nx.draw(view, pos, with_labels=True,node_size=800, font_weight = 'bold',
                labels = node_labels, node_color = Ncolors, edge_color=colors)
        edge_labels = nx.get_edge_attributes(view, 'label')
        nx.draw_networkx_edge_labels(view, pos, edge_labels, font_weight = 'bold')
        plt.savefig(output_path+ "_" + str(perc_cov)+"_view.jpg")
            
        new_output_path = output_path + "_" + str(perc_cov) + '_cov'
        out_file = open(new_output_path + ".vw", "w")
        out_file.write('q # 0 \n')
        nodes = nx.get_node_attributes(view,'label')
        for nodeID, vertex in enumerate(nodes.keys()):
            out_file.write("v " + str(nodeID) + " " + nodes[vertex]  + '\n' )
        edges = nx.get_edge_attributes(view,'label')
        for e in edges:
            head = str(list(nodes.keys()).index(e[0]))
            tail = str(list(nodes.keys()).index(e[1]))
            out_file.write("e " + head + " " + tail + " " + edges[e] + '\n' )
        out_file.close()
        
        #output queries in format readable by patternMatch algos
        # out_file = open(new_output_path + ".qry", "w")
        out_file = open(new_output_path + ".qry", "w")
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
    

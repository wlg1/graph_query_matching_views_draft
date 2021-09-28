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
num_Vedges = 2
max_num_extra_views = 5
num_queries = 20

# partially cov subgraph parameters
# perc_E_uncov = 0.5
perc_E_uncov = 1

input_file = 'inst_lb20_m'

input_path = '../queries/' + input_file + '.qry'
# input_path = input_file + '.qry'
f = open(input_path, "r")

def get_edge_color(label):
    if label == '0':
        return 'r'
    else:
        return 'b'

for querynum in range(12, num_queries):
    output_prefix = input_file.replace('inst_', '') + '_'+str(num_Vedges)+'Eviews'
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
        
    # choose a partial subgraph according to tunable parameters
    num_cov_E = math.ceil(len(G.edges) * perc_E_uncov)
    partial_cov_subgraphs = [(unG.edge_subgraph(selected_Es), G.edge_subgraph(selected_Es))  for selected_Es in itertools.combinations(G.edges(), num_cov_E)]
    for SG_pair in partial_cov_subgraphs: #loop until find conn subgr
        undirSG = SG_pair[0]
        dirSG = SG_pair[1]
        if nx.is_connected(undirSG):  #is_conn doesn't work for dir G
            newSG = nx.DiGraph()  #create new to get labels in
            for node in dirSG.nodes():
                newSG.add_node(node)
                newSG.add_nodes_from([
                    (node, {"label": node_labels[node]}) ])
            for edge in dirSG.edges():
                head = edge[0]
                tail = edge[1]
                edgeLabel = edge_labels[edge]
                newSG.add_edge(head,tail, label = edgeLabel, color = get_edge_color(edgeLabel))
            if len(newSG.edges()) == num_cov_E:
                undir_partial_subgraph = undirSG
                partial_subgraph = newSG
                break
            
    # get all connected subgraphs of PARTIAL SUBGRAPH of desired size
    all_connected_subgraphs = []
    subgraphs = [(undir_partial_subgraph.edge_subgraph(selected_Es), partial_subgraph.edge_subgraph(selected_Es))  for selected_Es in itertools.combinations(partial_subgraph.edges(), num_Vedges)]
    for SG_pair in subgraphs:
        undirSG = SG_pair[0]
        dirSG = SG_pair[1]
        if nx.is_connected(undirSG):  #is_conn doesn't work for dir G
            newSG = nx.DiGraph()  #create new to get labels in
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

    if not all_connected_subgraphs: #no views possible to choose
        continue
    
    #randomly choose views until all edges in partial subgraph are covered
    covered_edges = set()
    views = []
    edges_to_cov = list(partial_subgraph.edges())
    #make copy b/c just b/c view not used for first set of views, doesn't mean can't be used in later sets
    all_connected_subgraphs_copy = all_connected_subgraphs.copy() 
    while all_connected_subgraphs_copy: #avoids infinite while True loop
        # vw = sample(all_connected_subgraphs_copy, 1)[0] 
        #GREEDY: score each cand view by how many new E they cov
        view_scores = {}
        for vw in all_connected_subgraphs_copy:
            view_scores[vw] = len(set(vw.edges()) - covered_edges)
        vw = max(view_scores, key=view_scores.get)
        
        all_connected_subgraphs_copy.remove(vw) #avoids infinite loop
        if vw not in views and len(set(vw.edges()) - covered_edges) > 0:
            views.append(vw)
            all_connected_subgraphs.remove(vw) #remove for ALL future sets of views
            covered_edges.update(list(vw.edges()))
            if (all(x in covered_edges for x in edges_to_cov)) or not all_connected_subgraphs:
                break
    
    #remove views that are redundant
    views_copy = views.copy()
    for vw in views_copy:
        views_copy_2 = views.copy()
        views_copy_2.remove(vw)
        union_E = []
        for vw2 in views_copy_2:
            union_E += [edge for edge in vw2.edges()]
        union_E = set(union_E)
        if set(vw.edges()).issubset(union_E):
             views.remove(vw)
    
    #want at least 3 nested sets of views
    # if len(all_connected_subgraphs) < 3: 
    #     continue
    
    fig = plt.figure()
    pos = nx.spring_layout(partial_subgraph)
    node_labels = nx.get_node_attributes(partial_subgraph, 'label')
    edges = nx.get_edge_attributes(partial_subgraph,'label')
    colors = [partial_subgraph[u][v]['color'] for u,v in edges]
    Ncolors = ['skyblue' for n in list(partial_subgraph.nodes())]
    nx.draw(partial_subgraph, pos, with_labels=True,node_size=800, font_weight = 'bold',
            labels = node_labels, node_color = Ncolors, edge_color=colors)
    edge_labels = nx.get_edge_attributes(partial_subgraph, 'label')
    nx.draw_networkx_edge_labels(partial_subgraph, pos, edge_labels, font_weight = 'bold')
    plt.savefig(output_path+"_partialSG.jpg")
    
    # continue choosing views until reach max num extra views
    for i in range(max_num_extra_views):
        if i > 0:
            vw = sample(all_connected_subgraphs, 1)[0]
            if vw not in views:
                views.append(vw)
                all_connected_subgraphs.remove(vw)
            
        new_output_path = output_path + "_" + str(len(views)) + 'views'
        out_file = open(new_output_path + ".vw", "w")
        for q, qry in enumerate(views):
            if num_Vedges > 1 and (not all_connected_subgraphs or i == max_num_extra_views-1):
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
                plt.savefig(new_output_path+"_view"+ str(q) + ".jpg")
            
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
        edges = list(G.edges()) #query edges
        desc_edges = [key for key, value in G.edges().items() if value['label'] == '1']
        tot_num_overlap = 0
        tot_desc_overlap = 0
        for e in edges:
            num_overlap = 0
            for vw in views:
                if e in list(vw.edges()):
                    num_overlap += 1
            num_edges[e] = num_overlap
            tot_num_overlap += num_overlap
        for de in desc_edges:
            num_overlap = 0
            for vw in views:
                if de in list(vw.edges()):
                    num_overlap += 1
            tot_desc_overlap += num_overlap
        
        out_file = open(new_output_path + "_edgeInfo.txt", "w")
        out_file.write("num views: " + str(len(views)) + "\n")
        out_file.write("tot num overlapping edges: " + str(tot_num_overlap) + "\n")
        out_file.write("avg num overlapping edges: " + str(tot_num_overlap / len(edges)) + "\n")
        out_file.write("tot num overlapping desc edges: " + str(tot_desc_overlap) + "\n")
        out_file.write("avg num overlapping desc edges: " + str(tot_desc_overlap / len(desc_edges)) + "\n")
        out_file.write("edge : num overlaps\n")
        for edge, num_overlaps in num_edges.items():
            out_file.write(repr(edge) + " : " + str(num_overlaps)  + "\n")    
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
        
        if not all_connected_subgraphs: #no more left to choose from
            break
    
f.close()
    

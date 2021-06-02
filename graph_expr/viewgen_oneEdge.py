#each edge is a view

input_file = 'inst_lb20_cyc_m.qry'
prefix = 'lb20_cyc_m'
input_path = 'queries/' + input_file
output_prefix = prefix + '_1Eviews'
output_name = output_prefix + '/' + output_prefix
f = open(input_path, "r")
num_queries = 14

for querynum in range(num_queries):
    node_labels = {} #id : label
    edges = []
    edge_labels = {} #(x,y) : label
    while True:
        line = f.readline().replace('\n','')
        lineAsList = line.split(' ')
        if lineAsList[0] == 'q':
            currQ = int(lineAsList[2])
        if currQ == querynum:
            if lineAsList[0] == 'v':
                node_labels[lineAsList[1]] = lineAsList[2]
            elif lineAsList[0] == 'e':
                edge = (lineAsList[1], lineAsList[2])
                edges.append(edge)
                edge_labels[edge] = lineAsList[3]
        if currQ > querynum or len(line) == 0:
            break
            
    #output queries in format readable by patternMatch algos
    out_file = open(output_name + "_q" + str(querynum+6) + ".qry", "w")
    out_file.write("q # 0\n")
    for nodeID in range(len(node_labels)):
        out_file.write("v " + str(nodeID) + " " + node_labels[str(nodeID)]  + '\n' )
    for e in edges:
        out_file.write("e " + e[0] + " " + e[1] + " " + edge_labels[e] + '\n' )
            
    out_file.close()
    
    #get query edge. its old nodes are now 0 and 1 in 
    #new view. map old query nodeIDs to new view nodeIDs
    
    #output templates used as views of this query set
    out_file = open(output_name + "_q" + str(querynum+6) + ".vw", "w")
    for q, view in enumerate(edges):
        out_file.write("q # " + str(q) + '\n')
        for nodeID, vertex in enumerate(view):
            out_file.write("v " + str(nodeID) + " " + node_labels[vertex]  + '\n' )
        out_file.write("e 0 1 " + edge_labels[view] + '\n' )
            
    out_file.close()




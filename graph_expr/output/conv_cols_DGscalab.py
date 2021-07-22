import pandas as pd
import numpy as np
import csv, os, pdb

directory = r'D:\Documents\_prog\prog_cust\eclipse-workspace\graph_expr\output\__badcol_outputs'

# nodeSizes = [100, 120, 140, 160]
# newFolder = 'Email_lb20_160K_nodes_lb20_cyc_m_2Eviews_q6'

def output_goodcol(fn, newFolder):
    input_fn = '__badcol_outputs/' + fn + '.csv'
    output_fn = newFolder+'/'+fn+'_newcols.csv'
    output_file = open(output_fn, mode='w', newline='')
    output_writer = csv.writer(output_file)
    
    input_file = open(input_fn, mode='r', newline='')
    input_reader = csv.reader(input_file)
    before_flag = True
    # for i, row in enumerate(input_reader):  #if-else based on i
    for row in input_reader: 
        if before_flag:
            new_row = ''.join(row).split(' ')
            if new_row[0] == 'Data':
                before_flag = False
                output_writer.writerow('')
                new_row = ''.join(row).split(':')
        else:
            new_row = ''.join(row).split(':')
        output_writer.writerow(new_row)
    
    input_file.close()  #must close after running or else must close Spyder to move file
    output_file.close()

for file in os.listdir(directory):
    fn = file.split('.')[0]
    newFolder = fn.split('__')[0]
    newFolder = newFolder.replace('_ALL', '')
    if not os.path.exists(newFolder):
        os.makedirs(newFolder)
    
    output_goodcol(fn, newFolder)

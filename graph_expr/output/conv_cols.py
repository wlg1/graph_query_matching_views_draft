import pandas as pd
import numpy as np
import csv, os, pdb

directory = r'D:\Documents\_prog\prog_cust\eclipse-workspace\graph_expr\output\badcol_outputs'

def output_goodcol(fn):
    input_fn = 'badcol_outputs/' + fn + '.csv'
    output_fn = 'goodcol_outputs/'+fn+'_newcols.csv'
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
    output_goodcol(file.split('.')[0])

import pandas as pd
import numpy as np
import csv, os, pdb

directory = r'D:\Documents\_prog\prog_cust\eclipse-workspace\graph_expr\output\__badcol_outputs'

def output_goodcol(fn):
    input_fn = '__badcol_outputs/' + fn + '.csv'
    output_fn = '__goodcol_outputs/'+fn+'_newcols.csv'
    output_file = open(output_fn, mode='w', newline='')
    output_writer = csv.writer(output_file)
    
    input_file = open(input_fn, mode='r', newline='')
    input_reader = csv.reader(input_file)
    before_flag = True
    
    total_times_byRow = []
    calc_avg_flag = True
    
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
        if len(new_row) > 1:
            if new_row[0] == 'id':
                temp_row = new_row[2:]
                if len(new_row) > 10:
                    header = [''] + [temp_row[0]] + temp_row[2:-2] + [temp_row[-1]]
                else:
                    header = [''] + temp_row
            if new_row[0] == 'Average':
                calc_avg_flag = False
            if calc_avg_flag and new_row[1] == 'success':
                temp_row = [float(x) for x in new_row[2:]]
                if len(new_row) > 10:
                    temp_row = [temp_row[0]] + temp_row[2:-2] + [temp_row[-1]]
                total_times_byRow.append(temp_row)
    
    # if len(total_times_byRow[0]) > 10:
    #     total_times_byRow.sort(key=lambda x: x[4])
    # else:
    #     total_times_byRow.sort(key=lambda x: x[3])
    total_times_byRow.sort(key=lambda x: x[3])
    
    # pruned_total_times = total_times_byRow[5:25]
    # pruned_total_times = total_times_byRow[10:20]
    pruned_total_times = total_times_byRow[10:40]
    # pruned_total_times = total_times_byRow[15:45]
    
    output_writer.writerow(header)
    for new_row in pruned_total_times:
        output_writer.writerow([' '] + new_row)
    
    def mean(a):
        return sum(a) / len(a)
    pruned_avg_totTime = map(mean, zip(*pruned_total_times))
    
    new_row = ['pruned_avg_totTime: '] + list(pruned_avg_totTime)
    
    output_writer.writerow(header)
    output_writer.writerow(new_row)
    
    input_file.close()  #must close after running or else must close Spyder to move file
    output_file.close()

for file in os.listdir(directory):
    output_goodcol(file.split('.')[0])

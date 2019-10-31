#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2019/1/13 下午9:30'
from sklearn import preprocessing
import logging
import os
from random import choice
import sqlite3
import numpy as np
import networkx as nx
import re
from collections import defaultdict
from collections import Counter
import pandas as pd

import random
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='findneighbors_data.log',
                    filemode='w')
db_path='/home/yqq/DBpedia/dbpedia_en.sqlite'
typelist=[]
def settypelist():
    global typelist
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("SELECT DISTINCT(entity_type_id) FROM property_triples")
    value = cursor.fetchall()
    alltype_id = [m[0] for m in value]
    typelist = alltype_id
    
def write_dict(file_path,dict):
    with open(file_path, "w") as f:
        for k in dict.keys():
            s=str(k)
            if dict[k]:
                for i in dict[k]:
                    s=s+' '+str(i)
            s=s+'\n'
            f.writelines(s)
    f.close()
def find_neighbors(t):
    # read file
    logging.info("%s type number", t)
    dict_node_neigh = defaultdict(list)
    datalist = []
    dict_nodes = defaultdict(list)
    mapping_nodes = []

    f = open('/home/yqq/DBpedia/HAS_master/data/adata_number/' + str(t) + '.txt', "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        #m=list(map(float, m))
        mapping_nodes.append(m[0])
        l = m[1:]
        datalist.append(l)
    data = np.array(datalist)
    if data.size == 0:
        return dict_node_neigh

    num_elements, dim = data.shape
    data_new = []
    for i in range(num_elements):
        data_new.append(list(map(float, data[i, :])))
    data = np.array(data_new)

    for i in range(len(data)):
        dict_nodes[tuple(data[i, :])].append(mapping_nodes[i])

    hypercube_radius=[]
    for i in range(dim):
        feature = []
        for value in data[:, i]:
            if value!=0:
                feature.append(value)
        median=np.median(feature)
        mad = np.median(np.abs(feature - median))  # * b
        lower_limit = median - (3 * mad)
        upper_limit = median + (3 * mad)
        validate_nodes = [i for i in feature if lower_limit <= i <= upper_limit]
        validate_nodes_len=len(validate_nodes)
        #print(validate_nodes)
        cr=(max(validate_nodes)- min(validate_nodes))/validate_nodes_len
        hypercube_radius.append(cr)
        
    '''
    得到hypercube中最近邻节点
    :param data_scaled:
    :param cr:
    :return:
    '''
    #得到每维特征排序后的data_array，方便后续查找数据，先排序，复杂度是O(dim*n*logn),
    #print(cr)
    data_sorted = np.sort(data, axis=0)  # 按列排序
    index_sorted = np.argsort(data, axis=0)  # 按列排序得到变化后的索引
    #排序之后顺序换了样本顺序换了，需要保存下来故有了index_sorted
    #cr=(data_sorted[validate_nodes][0]-data_sorted[0][0])/validate_nodes

    for i in range(num_elements):
        # 第i个样本对应的真实节点序号，含有许多重复点
        nodes = []
        nodes = dict_nodes[tuple(data[i, :])]
        # logging.info("%s sample %s nodes", str(i), nodes)
        # 求第i个样本点的近邻点
        res_array = []
        for j in range(dim):
            if data[i][j]==0:
                continue
            cr=hypercube_radius[j]
            min_value = data[i][j] - cr
            max_value = data[i][j] + cr
            temp_D = data_sorted[:, j]
            temp_I = index_sorted[:, j]
            res = BinarySearch_interval(temp_D, temp_I, min_value, max_value)
            res_array.append(res)

        # 求交集intersection，得到索引邻近点序号
        if len(res_array)==0:
            continue
        s = set(res_array[0])
        for r in range(1, len(res_array)):
            s = s.intersection(set(res_array[r]))
        # 计算真实临近点序号加上打在自身同一点序号，真实临近点中也有重复点
        if i in s:
            s.remove(i)

        neighbor = []
        for p in s:
            for q in dict_nodes[tuple(data[p, :])]:
                neighbor.append(q)
        if len(nodes) == 1:
            dict_node_neigh[nodes[0]] = neighbor
        else:
            finalres = []
            finalres = neighbor + nodes
            for n in nodes:
                dict_node_neigh[n] = finalres
    
    write_dict('/home/yqq/DBpedia/HAS_master/data/'+str(t)+'.txt',dict_node_neigh)
    logging.info("%s type number finished", t)
    
def BinarySearch_interval(list1, list_Index, min, max):
    '''
    二分查找得到区间(f-r，f+r)的样本,只是要找到区间中的值，二分找而不是从前往后O(n)
    找出该维度特征上对应区间的（min,max）的点，返回这些点（点最原始的索引值）
    :param list:
    :param list_Index:
    :param min:
    :param max:
    :return:
    '''
    res = []
    start = 0
    end = len(list1)

    while start <= end:
        mid = int((start + end) / 2)
        if list1[mid] >= min and list1[mid] <= max:
            break
        if list1[mid] < min:
            start = mid + 1
        if list1[mid] > max:
            end = mid - 1

    i = mid
    j = mid
    while list1[i] >= min and list1[i] <= max:
        res.append(list_Index[i])
        i = i - 1
        if i < 0:
            break

    while list1[j] >= min and list1[j] <= max:
        res.append(list_Index[j])
        j = j + 1
        if j >= len(list1):
            break
    return res

if __name__ == '__main__':
    
#    settypelist()
#    for t in typelist:
#        
        find_neighbors(8)
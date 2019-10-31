#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/12/5 上午9:06'

import sqlite3
import numpy as np
from sklearn import preprocessing
from collections import defaultdict
import logging
import os
from random import choice
from collections import defaultdict
from collections import Counter
import random


db_path='/gpfssan1/home/xiangzhang/yqq/dbpedia_en.sqlite'
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='HAS_S.log',
                    filemode='a+')
def write_dict(file_path,dict):
    with open(file_path, "a") as f:
        for k in dict.keys():
            s=str(k)
            if dict[k]:
                for i in dict[k]:
                    s=s+' '+str(i)
            s=s+'\n'
            f.writelines(s)
    f.close()


def statistics():
    global typelist
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("select distinct(entity1_type_id) from relation_triples")
    value = cursor.fetchall()
    type= [m[0] for m in value]
    typelist=type

    dict_add = defaultdict(list)
    cursor.execute("SELECT entity1_id,entity2_type_id from relation_triples")
    value = cursor.fetchall()
    for v in value:
        dict_add[v[0]].append(v[1])

    for t in type:
        # 该类型下的宾语的种类数
        cursor.execute("select distinct(entity2_type_id) from relation_triples where entity1_type_id="+str(t))
        value = cursor.fetchall()
        num_entity2 = [m[0] for m in value]

        # 该类型下的所有实体
        cursor.execute("select DISTINCT(entity1_id) from relation_triples where entity1_type_id="+str(t))
        value = cursor.fetchall()
        num_entity1 = [m[0] for m in value]

        #查找该类型下的所有的实体的宾语种类数
        dictentity = {}
        for e1 in num_entity1:
            res=Counter(dict_add[e1])
            list1=[]
            for e2 in num_entity2:
                list1.append(res[e2])
            dictentity[e1]=list1
        logging.info('finished %s', t)
        write_dict('../data/sdata/' + str(t) + '.txt', dictentity)
        dictentity.clear()
    cursor.close()
    connection.close()

def writedict(file_path,dict):
    with open(file_path, "a") as f:
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
    dict_node_neigh={}
    datalist = []
    dict_nodes = defaultdict(list)
    mapping_nodes = []

    f = open('../data/sdata/'+str(t)+'.txt', "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        mapping_nodes.append(m[0])
        l = m[1:]
        datalist.append(l)
    data = np.array(datalist)
    '''
    min-max and save some same nodes,rmeove deduplication
    '''
    # min-max
    min_max_scaler = preprocessing.MinMaxScaler()
    data_original = min_max_scaler.fit_transform(data)
    # dict{} d record some nodes which have same features. after min_max_scaler
    for i in range(len(data_original)):
        dict_nodes[tuple(data_original[i, :])].append(mapping_nodes[i])

    # data_dedup[] deduplication，all operation is based on data_dedup
    data_scaled = np.array(list(set([tuple(t) for t in data_original])))

    m, n = data_scaled.shape
    dim = n
    num_elements = m
    logging.info("%s dim: %s num_elements", n, m)

    '''
    得到hypercube中最近邻节点
    :param data_scaled:
    :param cr:
    :return:
    '''
    # 得到每维特征排序后的data_array，方便后续查找数据，先排序，复杂度是O(dim*n*logn),
    cr = 1 / num_elements

    data_sorted = np.sort(data_scaled, axis=0)  # 按列排序
    index_sorted = np.argsort(data_scaled, axis=0)  # 按列排序的到变化后的索引
    # 排序之后顺序换了样本顺序换了，需要保存下来故有了index_sorted
    result = []
    for i in range(num_elements):
        # 第i个样本对应的真实节点序号，含有许多重复点
        nodes=[]
        nodes = dict_nodes[tuple(data_scaled[i, :])]
        #logging.info("%s sample %s nodes", str(i), nodes)
        # 求第i个样本点的近邻点
        res_array = []
        for j in range(dim):
            min = data_scaled[i][j] - cr
            max = data_scaled[i][j] + cr
            temp_D = data_sorted[:, j]
            temp_I = index_sorted[:, j]
            res = BinarySearch_interval(temp_D, temp_I, min, max)
            res_array.append(res)

        # 求交集intersection，得到索引邻近点序号
        new_res_array = []
        for r in res_array:
            if len(r) != 0:
                new_res_array.append(r)

        if len(new_res_array) > 0:
            s = new_res_array[0]
            for r in new_res_array:
                s = set(s).intersection(r)

        #logging.info("%s sample %s samenodes", str(i), list(s))
        # 计算真实临近点序号加上打在自身同一点序号，真实临近点中也有重复点
        if i in s:
            s.remove(i)
        neighbor = []
        for p in s:
            for q in dict_nodes[tuple(data_scaled[p, :])]:
                neighbor.append(q)
        #logging.info("%s sample %s neighbor", str(i), neighbor)
        if len(nodes)==1:
            dict_node_neigh[nodes[0]] = neighbor
        else:
            finalres=[]
            finalres=neighbor+nodes
            for n in nodes:
                dict_node_neigh[n]=finalres
    #writedict("../data/sneigh/"+str(t)+".txt",dict_node_neigh)
    return dict_node_neigh


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


def write_txt(file_path,list):
    with open(file_path, "a") as f:
        for li in list:
            s = ''
            for l in li:
                s = s + str(l) + ' '
            s += '\n'
            f.writelines(s)
    f.close()

def random_walk(dict_node_neigh,path_length, alpha=0, rand=random.Random(), start=None):
    """ Returns a truncated random walk.
        path_length: Length of the random walk.
        alpha: probability of restarts.
        start: the start node of the random walk.
    """
    if start:
      path = [start]
    else:
      # Sampling is uniform w.r.t V, and not w.r.t E
      path = [rand.choice(list(dict_node_neigh.keys()))]

    while len(path) < path_length:
      cur = path[-1]
      if len(dict_node_neigh[cur]) > 0:
        if rand.random() >= alpha:
          path.append(rand.choice(dict_node_neigh[cur]))
        else:
          path.append(path[0])
      else:
        break
    return [str(node) for node in path]

def build_deepwalk_corpus(dict_node_neigh,nodes):
    num_paths=10
    path_length=8
    alpha = 0
    rand = random.Random(0)
    walks = []

    for cnt in range(num_paths):
        rand.shuffle(nodes)
        for node in nodes:
            '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
            walks.append(random_walk(dict_node_neigh,path_length, rand=rand, alpha=alpha, start=node))
    return walks


if __name__ == '__main__':
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("select distinct(entity1_type_id) from relation_triples")
    value = cursor.fetchall()
    type= [m[0] for m in value]
    typelist=type
    #typelist=[106,2188,1887,17,7112]
    seq=[]
    for t in typelist:
        logging.info('type========= %s',t)
        dict_node_neigh=find_neighbors(t)
        nodes=[]
        for k in dict_node_neigh.keys():
            nodes.append(k)
        walks=build_deepwalk_corpus(dict_node_neigh, nodes)
        seq=seq+walks
    write_txt('../data/S_walk_DBpedia_10.txt', seq)





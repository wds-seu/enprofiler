#!/usr/bin/env python
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2019/1/23 下午9:16'
import sqlite3
from collections import defaultdict
import random
db_path='/gpfssan1/home/xiangzhang/yqq/dbpedia_en.sqlite'
import os
path="../data/aneigh_number_5r"
def random_walk(dict_node_neigh, path_length, alpha=0, rand=random.Random(), start=None):
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
def build_deepwalk_corpus(dict_node_neigh, nodes):
    num_paths = 33
    path_length = 8
    alpha = 0
    rand = random.Random(0)
    walks = []

    for cnt in range(num_paths):
        rand.shuffle(nodes)
        for node in nodes:
            '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
            walks.append(random_walk(dict_node_neigh, path_length, rand=rand, alpha=alpha, start=node))
    return walks

def write_txt(file_path,list):
    with open(file_path, "a") as f:
        for li in list:
            s = ''
            for l in li:
                s = s + str(l) + ' '
            s += '\n'
            f.writelines(s)
    f.close()

if __name__ == '__main__':
    seq=[]
    files = os.listdir(path)
    for file in files:
        if not file.startswith('.'):
            filepath = path + "/" + file
            dict_node_neigh=defaultdict(list)
            f = open(filepath, "r")
            lines = f.readlines()
            for line in lines:
                m = line.strip().split(' ')
                dict_node_neigh[m[0]]=m[1:]
                if m[0] in dict_node_neigh[m[0]]:
                    dict_node_neigh[m[0]].remove(m[0])
            nodes = []
            for k in dict_node_neigh.keys():
                nodes.append(k)
            walks = build_deepwalk_corpus(dict_node_neigh, nodes)
            seq = seq + walks
    write_txt('../data/A_number_5r_walk_DBpedia_w10.txt', seq)





#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/12/5 下午3:38'


import logging
import sys
from io import open
from os import path
from time import time
from glob import glob
from six.moves import range, zip, zip_longest
from six import iterkeys
from collections import defaultdict, Iterable
import random
from random import shuffle
from itertools import product,permutations
from scipy.io import loadmat
from scipy.sparse import issparse

logger = logging.getLogger("deepwalk")
LOGFORMAT = "%(asctime).19s %(levelname)s %(filename)s: %(lineno)s %(message)s"
class Graph(defaultdict):
    """Efficient basic implementation of nx `Graph' â€“ Undirected graphs with self loops"""

    def __init__(self):
        super(Graph, self).__init__(list)

    def nodes(self):
        return self.keys()

    def adjacency_iter(self):
        return self.iteritems()

    def subgraph(self, nodes={}):
        subgraph = Graph()

        for n in nodes:
            if n in self:
                subgraph[n] = [x for x in self[n] if x in nodes]

        return subgraph

    def make_undirected(self):

        t0 = time()

        for v in self.keys():
            for other in self[v]:
                if v != other:
                    self[other].append(v)

        t1 = time()
        logger.info('make_directed: added missing edges {}s'.format(t1 - t0))

        self.make_consistent()
        return self

    def make_consistent(self):
        t0 = time()
        for k in iterkeys(self):
            self[k] = list(sorted(set(self[k])))

        t1 = time()
        logger.info('make_consistent: made consistent in {}s'.format(t1 - t0))

        self.remove_self_loops()

        return self

    def remove_self_loops(self):

        removed = 0
        t0 = time()

        for x in self:
            if x in self[x]:
                self[x].remove(x)
                removed += 1

        t1 = time()

        logger.info('remove_self_loops: removed {} loops in {}s'.format(removed, (t1 - t0)))
        return self

    def check_self_loops(self):
        for x in self:
            for y in self[x]:
                if x == y:
                    return True

        return False

    def has_edge(self, v1, v2):
        if v2 in self[v1] or v1 in self[v2]:
            return True
        return False

    def degree(self, nodes=None):
        if isinstance(nodes, Iterable):
            return {v: len(self[v]) for v in nodes}
        else:
            return len(self[nodes])

    def order(self):
        "Returns the number of nodes in the graph"
        return len(self)

    def number_of_edges(self):
        "Returns the number of nodes in the graph"
        return sum([self.degree(x) for x in self.keys()]) / 2


def random_walk(self, path_length, alpha=0, rand=random.Random(), start=None):
    """ Returns a truncated random walk.
'''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点
简单的dfs找序列'''
        path_length: Length of the random walk.
        alpha: probability of restarts.
        start: the start node of the random walk.
    """
    G = self
    if start:
        path = [start]
    else:
        # Sampling is uniform w.r.t V, and not w.r.t E
        path = [rand.choice(list(G.keys()))]

    while len(path) < path_length:
        cur = path[-1]
        if len(G[cur]) > 0:
            if rand.random() >= alpha:
                path.append(rand.choice(G[cur]))
            else:
                path.append(path[0])
        else:
            break
    return [str(node) for node in path]


# TODO add build_walks in here
def build_corpus(G, num_paths, path_length, alpha=0, rand=random.Random(0)):
    walks = []
    nodes = list(G.nodes())
    for cnt in range(num_paths):
        rand.shuffle(nodes)
        for node in nodes:
            '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
            walks.append(G.random_walk(path_length, rand=rand, alpha=alpha, start=node))
    return walks


def load_edgelist_with_nodetype(file_, undirected=False):
    entity1=[]
    entity2=[]
    entity1_type=[]
    entity2_type=[]

    with open(file_) as f:
        for l in f:
            e1, e2,e1_type,e2_type = l.strip().split()[:4]
            entity1.append(e1)
            entity2.append(e2)
            entity1_type.append(e1_type)
            entity2_type.append(e2_type)
    f.close()
    entity2_type=list(set(entity2_type))

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


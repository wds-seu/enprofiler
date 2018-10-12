#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/10/7 下午7:22'

import argparse
import networkx as nx
from src import SPVec
from gensim.models import Word2Vec
import random

def parse_args():
    """
    解析 SP2vec 参数.
    """
    parser = argparse.ArgumentParser(description="Run node2vec.")
    # nargs:参数的数量
    parser.add_argument('--input', nargs='?', default='', help='Input graph path')
    parser.add_argument('--output', nargs='?', default='', help='Embeddings path')
    # 特征维度：默认为128维
    parser.add_argument('--dimensions', type=int, default=128, help='Number of dimensions. Default is 128.')
    # 每个节点步行长度：默认为80
    parser.add_argument('--walk-length', type=int, default=80, help='Length of walk per source. Default is 80.')
    # 每个节点的步行数量：默认值为10
    parser.add_argument('--num-walks', type=int, default=10, help='Number of walks per source. Default is 10.')
    # 滑动窗口大小：默认值为10
    parser.add_argument('--window-size', type=int, default=10, help='Context size for optimization. Default is 10.')
    # SGD的迭代次数：默认值为1
    parser.add_argument('--iter', default=1, type=int, help='Number of epochs in SGD')
    parser.add_argument('--workers', type=int, default=8, help='Number of parallel workers. Default is 8.')
    # 是否有权值：默认无权值
    parser.add_argument('--weighted', dest='weighted', action='store_true', help='Boolean specifying (un)weighted. Default is unweighted.')
    parser.add_argument('--unweighted', dest='unweighted', action='store_false')
    parser.set_defaults(weighted=False)
    # 默认为无向图
    parser.add_argument('--directed', dest='directed', action='store_true', help='Graph is (un)directed. Default is undirected.')
    parser.add_argument('--undirected', dest='undirected', action='store_false')
    parser.set_defaults(directed=True)
    return parser.parse_args()

def read_graph(filepath):
    """ 读取networkx中的输入网络."""
    if args.weighted:
        G = nx.read_edgelist(filepath, nodetype=int, data=(('weight', float),), create_using=nx.DiGraph())
    else:
        G = nx.read_edgelist(filepath, nodetype=int, create_using=nx.DiGraph())
        for edge in G.edges():
            G[edge[0]][edge[1]]['weight'] = 1

    if not args.directed:
        G = G.to_undirected()
    return G

def get_dim_numnodes():
    '''
    得到类型种类数，即dim
    :return:
    '''
    f = open(args.input, "r")
    lines = f.readlines()
    typelist=[]
    nodeslist=[]
    dict_tailnode={}
    for line in lines:
       m = line.strip().split(' ')
       head = m[0]
       tail = m[1]
       tail_type = m[2]

       nodeslist.append(head)
       typelist.append(tail_type)
       dict_tailnode[tail] = tail_type

    nodesset=set(nodeslist)
    typeset=set(typelist)

    print("number of type of tailnodes(dim): %s",len(typeset))
    print("number of nodes(num_elements) : %s", len(nodesset))

    return list(typeset),list(nodesset),dict_tailnode

def learn_embeddings(walks):
    """Learn embeddings by optimizing the Skipgram objective using SGD.通过使用SGD优化Skipgram目标来学习嵌入"""
    walks = [list(map(str, walk)) for walk in walks]
    model = Word2Vec(walks, size=args.dimensions, window=args.window_size, min_count=0, sg=1, workers=args.workers, iter=args.iter)
    model.wv.save_word2vec_format(args.output)
    return

def main(args):
    nx_G = read_graph(args.input)#读G
    typelist,nodelist,dict_tailnode=get_dim_numnodes()
    SP=SPVec.SPVecGraph(nx_G,typelist,nodelist,dict_tailnode)
    r=SP.get_r()
    SP.find_neighbor(r)

    # 生成新的图开始随机游走，walks是随机游走生成的多个节点序列
    SP.new_Graph()
    new_G=read_graph("data/new_edgelist.txt")
    walks=SP.build_corpus(new_G,num_paths=args.number_walks,path_length=args.walk_length, alpha=0, rand=random.Random(args.seed))


if __name__ == '__main__':
    args = parse_args()
    main(args)

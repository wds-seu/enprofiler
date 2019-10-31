#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/10/12 下午5:06'
import os
import sys
import random
from argparse import ArgumentParser, FileType, ArgumentDefaultsHelpFormatter
import logging
from gensim.models import Word2Vec



file_A_number='/gpfssan1/home/xiangzhang/yqq/A_number_5r_walk_DBpedia.txt'
file_A_string='/gpfssan1/home/xiangzhang/yqq/A_string_walk_DBpedia.txt'
file_S='/gpfssan1/home/xiangzhang/yqq/S_walk_DBpedia.txt'
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='/gpfssan1/home/xiangzhang/yqq/HSP/src/S_seq.log',
                    filemode='w')
def combine_seq(args):    
    walks=[]
#    f = open(file_A_number, "r")
#    lines = f.readlines()
#    for line in lines:
#        m = line.strip().split(' ')
#        walks.append(m)
#    logging.info('file_A_number finished')
#    f.close()
#
    f = open(file_A_string, "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        walks.append(m)
    logging.info('file_A_string finished')
    f.close()

#    f = open(file_S, "r")
#    lines = f.readlines()
#    for line in lines:
#        m = line.strip().split(' ')
#        walks.append(m)
#    logging.info('file_S finished')
#    f.close()
    model = Word2Vec(walks, size=args.representation_size, window=args.window_size, min_count=0, sg=1, hs=1,
                     workers=args.workers)
    model.wv.save_word2vec_format(args.output)
    
def build_corpus(G, num_paths,path_length, alpha=0, rand=random.Random(0)):
        walks = []
        nodes = list(G.nodes())
        for cnt in range(num_paths):
            rand.shuffle(nodes)
            for node in nodes:
                '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
                walks.append(G.random_walk(path_length, rand=rand, alpha=alpha, start=node))
        return walks

def main():
  parser = ArgumentParser("deepwalk",
                          formatter_class=ArgumentDefaultsHelpFormatter,
                          conflict_handler='resolve')

  parser.add_argument("--debug", dest="debug", action='store_true', default=False,
                      help="drop a debugger if an exception is raised.")

  parser.add_argument('--format', default='adjlist',
                      help='File format of input file')

  parser.add_argument("-l", "--log", dest="log", default="INFO",
                      help="log verbosity level")

  parser.add_argument('--matfile-variable-name', default='network',
                      help='variable name of adjacency matrix inside a .mat file.')

  parser.add_argument('--max-memory-data-size', default=1000000000, type=int,
                      help='Size to start dumping walks to disk, instead of keeping them in memory.')

  parser.add_argument('--number-walks', default=100, type=int,
                      help='Number of random walks to start at each node')

  parser.add_argument('--output', default='/gpfssan1/home/xiangzhang/yqq/dbpedia_w100_v200_d8_Amodel_string.embeddings',
                      help='Output representation file')

  parser.add_argument('--representation-size', default=200, type=int,
                      help='Number of latent dimensions to learn for each node.')

  parser.add_argument('--seed', default=0, type=int,
                      help='Seed for random walk generator.')

  parser.add_argument('--undirected', default=True, type=bool,
                      help='Treat graph as undirected.')

  parser.add_argument('--vertex-freq-degree', default=False, action='store_true',
                      help='Use vertex degree to estimate the frequency of nodes '
                           'in the random walks. This option is faster than '
                           'calculating the vocabulary.')

  parser.add_argument('--walk-length', default=8, type=int,
                      help='Length of the random walk started at each node')

  parser.add_argument('--window-size', default=5, type=int,
                      help='Window size of skipgram model.')

  parser.add_argument('--workers', default=32, type=int,
                      help='Number of parallel processes.')
  args = parser.parse_args()
  combine_seq(args)

if __name__ == "__main__":
  sys.exit(main())


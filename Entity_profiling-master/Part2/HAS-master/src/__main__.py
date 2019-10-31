#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/12/5 下午2:47'

import sqlite3
import os
import sys
import random
from argparse import ArgumentParser, FileType, ArgumentDefaultsHelpFormatter
import logging
from gensim.models import Word2Vec
from . import graph
import psutil
from multiprocessing import cpu_count
p = psutil.Process(os.getpid())
try:
    p.set_cpu_affinity(list(range(cpu_count())))
except AttributeError:
    try:
        p.cpu_affinity(list(range(cpu_count())))
    except AttributeError:
        pass

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='HAS.log',
                    filemode='w')

def mix_path(args):
    G = graph.load_edgelist(file_H, undirected=args.undirected)
    walks = graph.build_corpus(G, num_paths=args.number_walks,
                         path_length=args.walk_length, alpha=0, rand=random.Random(args.seed))
    logging.info('H finished')

    f = open(file_S, "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        walks.append(m)
    logging.info('S finished')
    f.close()

    f = open(file_P, "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        walks.append(m)
    logging.info('P finished')
    f.close()

    model = Word2Vec(walks, size=args.representation_size, window=args.window_size, min_count=0, sg=1, hs=1,
                     workers=args.workers)
    model.wv.save_word2vec_format(args.output)

def main():
    parser = ArgumentParser("HAS",
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

    parser.add_argument('--number-walks', default=10, type=int,
                        help='Number of random walks to start at each node')

    parser.add_argument('--output', default='../data/HAS_vec',
                        help='Output representation file')

    parser.add_argument('--representation-size', default=64, type=int,
                        help='Number of latent dimensions to learn for each node.')

    parser.add_argument('--seed', default=0, type=int,
                        help='Seed for random walk generator.')

    parser.add_argument('--undirected', default=True, type=bool,
                        help='Treat graph as undirected.')

    parser.add_argument('--vertex-freq-degree', default=False, action='store_true',
                        help='Use vertex degree to estimate the frequency of nodes '
                             'in the random walks. This option is faster than '
                             'calculating the vocabulary.')

    parser.add_argument('--walk-length', default=10, type=int,
                        help='Length of the random walk started at each node')

    parser.add_argument('--window-size', default=5, type=int,
                        help='Window size of skipgram model.')

    parser.add_argument('--workers', default=32, type=int,
                        help='Number of parallel processes.')
    args = parser.parse_args()
    mix_path(args)

if __name__ == "__main__":
    sys.exit(main())


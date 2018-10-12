#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/10/7 下午7:23'

import numpy as np
import random
from sklearn import preprocessing
from collections import Counter
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='SPvec.log',
                    filemode='a+')

class SPVecGraph():

    def __init__(self,nx_G,typelist,nodelist,dict_tailnode):
        self.nx_G = nx_G
        self.typelist=typelist
        self.nodelist=nodelist
        self.dict_tailnode=dict_tailnode

        self.data= self.read_feature(self)
        self.data_scaled =self.preprocessing_data()

        self.num_elements = len(self.nodelist)
        self.dim= len(self.typelist)

        self.inputs_sorted, self.inputs_sorted_index = self.get_inputs_sorted()

    def preprocessing_data(self):
        #data = np.random.randint(0, 20, size=[1000, 6])
        min_max_scaler = preprocessing.MinMaxScaler()
        data_scaled = min_max_scaler.fit_transform(self.data)
        return data_scaled


    def build_input_matrix(self):
        '''
        得到样本矩阵，真正的输入
        :return:
        '''
        matrix = []
        for n in self.nodelist:
            neighbors = self.nx_G.neighbors(n)
            mapping_type = []
            for n in neighbors:
                mapping_type.append(self.dict_tailnode[n])
            c = Counter(mapping_type)
            feature = []
            for t in self.typelist:
                feature.append(c[t])
            matrix.append(feature)

        write_txt("input_matrix",matrix)

        mappingg_index=[]
        i=0
        for n in neighbors:
            mappingg_index.append([i,n])
            i+=1
        write_txt("input_matrix_indexmapping", mappingg_index)

        input_matrix=np.array(matrix)
        return input_matrix

    def get_inputs_sorted(self):
        inputs=self.data_scaled
        inputs_sorted = np.sort(inputs, axis=0)
        inputs_sorted_index = np.argsort(inputs, axis=0)
        return inputs_sorted, inputs_sorted_index


    def get_r(self, n):
        """
        get the r
        :param n: the number of elements
        """
        return 1 / n

    def change_r(self, r):
        """
        :param r: origin r
        :return: scaled r
        """
        d_mean = self.get_average_degree()
        dim = self.inputs_sorted.shape[1]
        r_new = pow(d_mean, 1 / dim) * r
        return r_new

    def get_average_degree(self):
        """
        get the average degree of the graph
        """
        pass
        #return nx.degree(self.G) / self.G.number_of_nodes()

    def find_neighbor(self, cr):
        """
        得到hypercube中最近邻节点
        :param data_scaled:
        :param cr:
        :return:
        """
        # 得到每维特征排序后的data_array，方便后续查找数据，先排序，复杂度是O(dim*n*logn),
        # 排序之后顺序换了样本顺序换了，需要保存下来故有了index_sorted
        result = []
        for i in range(self.num_elements):
            # 求第i个样本点的近邻点
            res_array = []
            dim = self.inputs_sorted.shape[1]
            for j in range(dim):
                min = self.data_scaled[i][j] - cr
                max = self.data_scaled[i][j] + cr
                inputs_sorted_j = self.inputs_sorted[:, j]
                inputs_sorted_index_j = self.inputs_sorted_index[:, j]
                res = self.BinarySearch_interval(inputs_sorted_j, inputs_sorted_index_j, min, max)
                logging.info("%s样本: %s近邻", str(i), res)
                res_array.append(res)
            # 求交集intersection
            l = []
            l.append(i)
            s = set(res_array[0])
            for i in range(1, len(res_array)):
                s = s.intersection(set(res_array[i]))
            m = l + list(s)
            logging.info(m)
            result.append(m)

        write_txt("output",result)

    def BinarySearch_interval(self, feature_j, feature_j_index, min, max):
        """
        二分查找得到区间(f-r，f+r)的样本,只是要找到区间中的值，二分找而不是从前往后O(n)
        找出该维度特征上对应区间的（min,max）的点，返回这些点（点最原始的索引值）
        :param feature_j:
        :param feature_j_index:
        :param min:
        :param max:
        :return:
        """
        res = []
        start = 0
        end = len(feature_j)
        while start <= end:
            # mid = int((start + end) / 2)  # 存在越界风险
            mid  = int(start + (end - start) / 2)
            if feature_j[mid] >= min and feature_j[mid] <= max:
                break
            if feature_j[mid] < min:
                start = mid + 1
            if feature_j[mid] > max:
                end = mid - 1

        i = mid
        j = mid
        while feature_j[i] >= min and feature_j[i] <= max:
            res.append(feature_j_index[i])
            i = i - 1
            if i < 0:
                break

        while feature_j[j] >= min and feature_j[j] <= max:
            res.append(feature_j_index[j])
            j = j + 1
            if j >= self.num_elements:
                break
        return res

    def new_Graph(self):
        edgelist = []
        f = open('data/output.txt', "r")
        lines = f.readlines()
        for line in lines:
            m = line.strip().split(' ')
            node = m[0]
            for i in m[1:, ]:
                edgelist.append([node, i])
        f.close()
        write_txt("new_edgelist",edgelist)

    def build_corpus(G, num_paths, path_length, alpha=0,
                              rand=random.Random(0)):
        walks = []

        nodes = list(G.nodes())

        for cnt in range(num_paths):
            rand.shuffle(nodes)
            for node in nodes:
                '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
                walks.append(G.random_walk(path_length, rand=rand, alpha=alpha, start=node))
        write_txt("walks",walks)

        return walks


def write_txt(name,list):
    with open("data/"+name+".txt", "a") as f:
        for li in list:
            s = ''
            for l in li:
                s = s + str(l) + ' '
            s += '\n'
            f.writelines(s)
    f.close()



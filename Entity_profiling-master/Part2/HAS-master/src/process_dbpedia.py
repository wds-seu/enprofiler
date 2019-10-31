#!/usr/bin/env python  
# -*- coding:utf-8 -*-
__author__ = 'yangqingqing'
__time__ = '2018/12/4 下午4:16'
import sqlite3
import logging
from collections import defaultdict
import random
import os
import nltk
from nltk.corpus import stopwords
import sys
from collections import Counter
import codecs
import numpy as np
from sklearn import preprocessing


def update_table_node_type():
    connection = sqlite3.connect('/Users/yangqingqing/Desktop/dataset-rdf/dbpedia_en.sqlite')
    cursor = connection.cursor()

    cursor.execute("SELECT node_id, type_id from nodes_type")
    value=cursor.fetchall()
    dicttype=defaultdict(list)

    res=[]

    for v in value:
        dicttype[v[0]].append(v[1])

    for k in dicttype.keys():
        if len(dicttype[k])==1:
            if dicttype[k][0]!=3:
               res.append((k,dicttype[k][0]))

    cursor.execute("CREATE TABLE node_type_new (id INTEGER PRIMARY KEY,node_id INTEGER,type_id INTEGER)")
    cursor.executemany("INSERT INTO node_type_new(node_id,type_id) VALUES(?,?)",res
                       )
    connection.commit()  # 建立node节点和Matrix_A的关系表
    cursor.close()
    connection.close()

def build_relation_triples_table():
    connection = sqlite3.connect('/Users/yangqingqing/Desktop/dataset-rdf/dbpedia_en.sqlite')
    cursor = connection.cursor()
    cursor.execute("SELECT subject_id, predicate_id, object_id FROM triples_all")
    triples = cursor.fetchall()

    cursor.execute("select node_id,type_id from nodes_type_new")
    node_type= cursor.fetchall()
    dict_nodes = defaultdict(list)
    for v in node_type:
        dict_nodes[v[0]].append(v[1])

    res=[]
    for t in triples:
        if len(dict_nodes[t[0]])==0:
            continue
        if len(dict_nodes[t[2]])==0:
            continue
        # subject_id_type=dict_nodes[t[0]]
        # object_id_type=dict_nodes[t[2]]
        # for s in subject_id_type:
        #     for o in object_id_type:

        res.append((t[0],t[1],t[2],dict_nodes[t[0]][0],dict_nodes[t[2]][0]))

    cursor.execute("CREATE TABLE relation_triples (id INTEGER PRIMARY KEY,entity1_id INTEGER,predicate_id INTEGER,entity2_id INTEGER,entity1_type_id INTEGER,entity2_type_id INTEGER)")
    cursor.executemany("INSERT INTO relation_triples(entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id) VALUES(?,?,?,?,?)", res)
    connection.commit()
    cursor.close()
    connection.close()

def build_property_triples_table():
    connection = sqlite3.connect('/Users/yangqingqing/Desktop/dataset-rdf/dbpedia_en.sqlite')
    cursor = connection.cursor()
    cursor.execute("SELECT subject_id, predicate_id, object_id FROM triples_all WHERE  (SELECT string_type_id FROM mapping WHERE id=triples_all.object_id )!=1")
    triples = cursor.fetchall()
    '''
    find entity_type_id
    '''
    cursor.execute("select node_id,type_id from nodes_type_new")
    node_type= cursor.fetchall()
    dict_nodes = defaultdict(list)
    for v in node_type:
        dict_nodes[v[0]].append(v[1])
    '''
    find object_type_id
    '''
    dict_mapping={}
    cursor.execute("SELECT id,string_type_id FROM mapping")
    value = cursor.fetchall()
    for v in value:
        dict_mapping[v[0]]=v[1]

    res=[]
    for t in triples:
        if len(dict_nodes[t[0]])==0:
            continue
        res.append((t[0],t[1],t[2],dict_nodes[t[0]][0],dict_mapping[t[2]]))

    cursor.execute("CREATE TABLE property_triples (id INTEGER PRIMARY KEY,entity_id INTEGER,predicate_id INTEGER,object_id INTEGER,entity_type_id INTEGER,object_type_id INTEGER)")
    cursor.executemany("INSERT INTO property_triples(entity_id,predicate_id,object_id,entity_type_id,object_type_id) VALUES(?,?,?,?,?)", res)
    connection.commit()
    cursor.close()
    connection.close()


def testtxt():
    # f=open('/Users/yangqingqing/PycharmProjects/HAS_master/data/sdata/1834.txt')
    # lines=f.readlines()
    # l=[]
    #
    # for line in lines:
    #     m = line.strip().split(' ')
    #     l.append(m[-2])
    # for i in range(len(l)):
    #     if l[i] != '0':
    #         print(l[i])
    #         print(i)
    #         print("have value")
    # data=np.array([[1.6085E8,2,0],[1.6085E8,1,0],[1.6085E8,3,0],[8.5E7,0,3]])
    # print(data)
    # print(type(data[0][0]))
    # min_max_scaler = preprocessing.MinMaxScaler()
    # data_scaled = min_max_scaler.fit_transform(data)
    # print(data_scaled)

    a=float("1.6085E8")
    print(a)




if __name__ == '__main__':
    #update_table_node_type()
    #build_relation_triples_table()
    #build_property_triples_table()
    testtxt()


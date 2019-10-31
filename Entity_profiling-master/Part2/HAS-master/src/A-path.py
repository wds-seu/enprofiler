from sklearn import preprocessing
import logging
import os
from random import choice
from collections import defaultdict
from collections import Counter
import sqlite3
import numpy as np
import networkx as nx
#from . import graph
import random
import re
logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO,
                    filename='HSP_A.log',
                    filemode='w')
db_path='/home/yqq/DBpedia/dbpedia_en.sqlite'
'''
for literal ,we have num and string
'''
typelist=[]
def settypelist():
    global typelist
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("SELECT DISTINCT(entity_type_id) FROM property_triples")
    value = cursor.fetchall()
    alltype_id = [m[0] for m in value]
    typelist = alltype_id
    
def find_property_string():
    global typelist
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("SELECT DISTINCT(entity_type_id) FROM property_triples")
    value = cursor.fetchall()
    alltype_id = [m[0] for m in value]
    typelist = alltype_id
    '''
    for literal,consider string
    '''
    for t in alltype_id:
        cursor.execute("select entity_id,object_id from property_triples where (object_type_id=2 or object_type_id=4 or object_type_id=13 ) and entity_type_id="+str(t))
        value = cursor.fetchall()
        write_txt('../data/adata_string/'+str(t)+'.txt',value)
    cursor.close()
    connection.close()

def find_neighbors_string(t):
    dict_neigh_string = defaultdict(list)
    logging.info("%s type", t)
    G = nx.read_edgelist('../data/adata_string/' + str(t) + '.txt',
                         create_using=nx.DiGraph())
    G = G.to_undirected()
    sub = []
    obj = []
    f = open('../data/adata_string/' + str(t) + '.txt', "r")
    lines = f.readlines()
    for line in lines:
        m = line.strip().split(' ')
        sub.append(m[0])
        obj.append(m[1])
    f.close()
    obj = list(set(obj))
    sub = list(set(sub))

    neigh_temp = []
    len_sub=len(sub)
    for o in obj:
#        if o==5044706:
#            continue
        len_neigh_o = len(list(G.neighbors(o)))

        if len_neigh_o< 0.8 * len_sub and len_neigh_o > 1:
            neigh_temp.append(G.neighbors(o))
        else:
            pass

    for ne in neigh_temp:
        for n in ne:
            for m in ne:
                dict_neigh_string[n].append(m)
    for k in dict_neigh_string.keys():
        if k in dict_neigh_string[k]:
           dict_neigh_string[k].remove(k)
        dict_neigh_string[k] = list(set(dict_neigh_string[k]))
    write_dict('../data/aneigh_string/' + str(t) + '.txt',dict_neigh_string)
    return dict_neigh_string 
    

def find_property_number():
    global typelist
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("SELECT DISTINCT(entity_type_id) FROM property_triples")
    value = cursor.fetchall()
    alltype_id = [m[0] for m in value]

    # mapping dict
    dict_mapping = {}
    cursor.execute("SELECT id,content FROM mapping")
    value1 = cursor.fetchall()
    for v in value1:
        dict_mapping[v[0]] = v[1]

    for t in alltype_id:
        logging.info("%s start", t)
        cursor.execute(
            "select entity_id,predicate_id,object_id,object_type_id from property_triples where (object_type_id!=2 and object_type_id!=4 and object_type_id!=13) and entity_type_id=" + str(
                t))
        value = cursor.fetchall()
        dict_ent_pred = defaultdict(list)  # save the entity_id and predicate_id

        for v in value:
            if len(dict_ent_pred[(v[0], v[1])]) == 0:
                dict_ent_pred[(v[0], v[1])].append(v[2])
                dict_ent_pred[(v[0], v[1])].append(dict_mapping[v[2]])
            else:
                continue

        predicate_num = list(set([v[1] for v in value]))  # the number of predicate_id,equal to the dimension
        entity_num = list(set([v[0] for v in value]))

        result = []
        for en in entity_num:
          r = [en]
          for p in predicate_num:
            if len(dict_ent_pred[(en, p)]) == 0:
                r.append(np.nan)
            else:
                try:
                    if re.match('[0-9]+-[0-9]+-[0-9]+',  dict_ent_pred[(en, p)][1]):
                        dict_ent_pred[(en, p)][1] = dict_ent_pred[(en, p)][1].strip().split('-')[0]
                    if re.match('[1-9].[0-9]+E[+-]*[0-9]+',  dict_ent_pred[(en, p)][1]):
                        dict_ent_pred[(en, p)][1] = float(dict_ent_pred[(en, p)][1])
                    if re.match('-[0-9]+-[0-9]+',  dict_ent_pred[(en, p)][1]):
                        dict_ent_pred[(en, p)][1] = dict_ent_pred[(en, p)][1].strip().split('-')[1]    
                    r.append(dict_ent_pred[(en, p)][1])
                except Exception as e:
                    logging.info("entity %s",en)
                    logging.info(e)
          result.append(r)
        write_txt('../data/adata_number_csv/' + str(t) + '.txt', result)
        logging.info("%s finish", t)

        
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
    num_paths = 10
    path_length = 10
    alpha = 0
    rand = random.Random(0)
    walks = []

    for cnt in range(num_paths):
        rand.shuffle(nodes)
        for node in nodes:
            '''随机游走参数是游走的长度，随机方式rand，alpha是可能性一个参数，start是开始节点'''
            walks.append(random_walk(dict_node_neigh, path_length, rand=rand, alpha=alpha, start=node))
    return walks
def write_txt(file_path, list):
    with open(file_path, "w") as f:
        for li in list:
            s = ''
            for i in range(0,len(li)):
                if i==len(li)-1:
                    s = s + str(li[i])
                    break
                s = s + str(li[i]) + '\t'
            s += '\n'
            f.writelines(s)
    f.close()


def find_neighbors(t):
    # read file
    logging.info("%s type number", t)
    dict_node_neigh = defaultdict(list)
    datalist = []
    dict_nodes = defaultdict(list)
    mapping_nodes = []

    f = open('../data/adata_number/' + str(t) + '.txt', "r")
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

    delete_col=[]
    row, col = data.shape
    for i in range(col):
        res=Counter(data[:, i])
        if int(res['0'])>row/2:
            delete_col.append(i)
    data=np.delete(data,delete_col, axis=1)
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
    cr = (1 / num_elements)

    data_sorted = np.sort(data_scaled, axis=0)  # 按列排序
    index_sorted = np.argsort(data_scaled, axis=0)  # 按列排序的到变化后的索引
    # 排序之后顺序换了样本顺序换了，需要保存下来故有了index_sorted

    for i in range(num_elements):
        # 第i个样本对应的真实节点序号，含有许多重复点
        nodes = []
        nodes = dict_nodes[tuple(data_scaled[i, :])]
        # logging.info("%s sample %s nodes", str(i), nodes)
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
        s = set(res_array[0])
        for r in range(1, len(res_array)):
            s = s.intersection(set(res_array[r]))

        # logging.info("%s sample %s samenodes", str(i), list(s))
        # 计算真实临近点序号加上打在自身同一点序号，真实临近点中也有重复点
        if i in s:
            s.remove(i)
        neighbor = []
        for p in s:
            for q in dict_nodes[tuple(data_scaled[p, :])]:
                neighbor.append(q)
        # logging.info("%s sample %s neighbor", str(i), neighbor)

        if len(nodes) == 1:
            dict_node_neigh[nodes[0]] = neighbor
        else:
            finalres = []
            finalres = neighbor + nodes
            for n in nodes:
                dict_node_neigh[n] = finalres

    write_dict('../data/aneigh_number/' + str(t) + '.txt',dict_node_neigh)
    
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
    # find_property_string()
#    find_property_number()
#    typehaved=[]
    settypelist()
#    seq=[]
    for t in typelist:
       dict_neigh_string=find_neighbors_string(t)
#        nodes=list(dict_neigh_string.keys())
#        walks=build_deepwalk_corpus(dict_neigh_string,nodes)
#        seq=seq+walks
#    write_txt("../data/P_string_walk.txt",seq)
    
    #     cursor.execute("SELECT DISTINCT(entity_id) FROM property_triples where entity_type_id=" + str(t))
    #     value = cursor.fetchall()
    #     allentity = [str((m[0])) for m in value]
    #     logging.info('type %s', t)
    #     dict_neigh = defaultdict(list)
    #     dict_neigh_string = find_neighbors_string(t)
    #     dict_neigh_number = find_neighbors(t)
    #     for e in allentity:
    #         dict_neigh[e] = dict_neigh_string[e] + dict_neigh_number[e]
    #         logging.info('============entity %s', e)
    #         logging.info(dict_neigh_string[e])
    #         logging.info(dict_neigh_number[e])
    #     nodes = allentity
    #     walks = build_deepwalk_corpus(dict_neigh, nodes)
    #     seq = seq + walks
    #     dict_neigh.clear()
    #     walks.clear()
    # write_txt('../data/P_walk.txt', seq)
    #find_property_string()
    # find_property_number()
#    settypelist()
#    connection = sqlite3.connect(db_path)
#    cursor = connection.cursor()
    #for t in typelist:
    #     cursor.execute("SELECT DISTINCT(entity_id) FROM property_triples where entity_type_id=" + str(t))
    #     value = cursor.fetchall()
    #     allentity = [str((m[0])) for m in value]
    #     logging.info('type %s', t)
         #dict_neigh = defaultdict(list)
    #    find_neighbors_string(t)
    #find_property_number()
    #     dict_neigh_number = find_neighbors(t)
    #     for e in allentity:
    #         dict_neigh[e] = dict_neigh_string[e] + dict_neigh_number[e]
    #         logging.info('============entity %s', e)
    #         logging.info(dict_neigh_string[e])
    #         logging.info(dict_neigh_number[e])
    #     nodes = allentity
    #     walks = build_deepwalk_corpus(dict_neigh, nodes)
    #     seq = seq + walks
    #     dict_neigh.clear()
    #     walks.clear()
    # write_txt('../data/P_walk.txt', seq)
     

















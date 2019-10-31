import os
import networkx as nx
import sqlite3
# 创建entity_centrality表存储实体的中心度
conn = sqlite3.connect('\\dbpedia_en.sqlite')
c = conn.cursor()
c.execute('''CREATE TABLE entity_centrality
       (ID INT PRIMARY KEY     NOT NULL,
        centrality         REAL);''')
conn.commit()
conn.close()

# 通过pagerank算法计算实体的中心度
os.chdir('..\\DBpedia\\')
filename = 'dbpedia_relation.txt'
G = nx.DiGraph()
with open(filename) as file:
    for line in file:
        head, tail = [int(x) for x in line.split()]
        G.add_edge(head, tail)
pr = nx.pagerank(G, alpha=0.85)

# 将实体中心度的值存到数据库entity_centrality表中
conn = sqlite3.connect('..\\dbpedia_en.sqlite')
c = conn.cursor()
# '''insert语句 把一个新的行插入到表中'''
# node = 1
# centrality = 0.21
sql = ''' insert into entity_centrality (ID, centrality)
              values
              (:entity_id, :centrality)'''

for node, value in pr.items():
    c.execute(sql, {'entity_id': node, 'centrality': value})

conn.commit()
conn.close()

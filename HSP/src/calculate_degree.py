import sqlite3
db_path='/Users/yangqingqing/Documents/Entity_Profiling/linkedmdb_filter_support.sqlite'

def statistic_degree():
    connection = sqlite3.connect(db_path)
    cursor = connection.cursor()
    cursor.execute("select distinct(entity1_id) from relations_triples")
    value=cursor.fetchall()
    entity1 = [m[0] for m in value]
    cursor.execute("select distinct(entity2_id) from relations_triples")
    value = cursor.fetchall()
    entity2 = [m[0] for m in value]
    e=set(entity1)
    entity=list(e.intersection(set(entity2)))

    #将其遍历一遍得到所有的
    cursor.execute("select entity1_id from relations_triples")
    value=cursor.fetchall()
    entity1 = [m[0] for m in value]

    cursor.execute("select entity2_id from relations_triples")
    value = cursor.fetchall()
    entity2 = [m[0] for m in value]

    dict={}
    for e in entity:
        dict[e]=0
        dict[e]=entity1.count(e)+entity2.count(e)








if __name__ == '__main__':
    statistic_degree()

you can ingore the file called "HSP/src",which is unuseful;

core code is entity_profiling-master

具体包含两个部分的内容：

# Part1（java，内部含有相关的模块函数的具体说明，关键部分有以下几小点）：

- 1、数据集处理的操作，解析RDF数据集，按照一定规则将其插入sqlite表保存;
- 2、区间离散化操作;
- 3、过滤标签;
- 4、后续得到实体的向量结果后进行cos计算操作，估量标签;
- 5、排序得到标签结果;

# Part2 (python关键部分有以下几小点)：

- 1、H路径：H-path 用于进行基于同质性的路径生成，单独的H方法生成实体向量；
- 2、A路径：A-path 用于进行基于属性相似性的路径生成，以及单独的A方法生成实体向量；
- 3、S路径：S-path 用于进行基于结构相似性的路径生成，以及单独的S方法生成实体向量；
- 4、HAS联合路径：HSP_combine_seq.py 该文件用于进行将所有实体节点的路进行融合，而后使用语言模型的方式；
- 5、findneighbors.py:用于找cube中的邻居；
- 6、其余的process用于处理sqlite表中数据信息，划分为了string类型标签和数值类型标签；

本方法针对rdf/n3/nt...等一些数据格式的数据集进行的数据解析存储操作，其他训练相关相关信息可参见deepwalk；

you can ingore the file called "HSP/src",which is unuseful;

core code is entity_profiling-master

具体包含两个部分的内容：
It contains two parts

## Part1（java，内部含有相关的模块函数的具体说明，关键部分有以下几小点）：
There are specific instructions for related module functions inside:

- 1、数据集处理的操作，解析RDF数据集，按照一定规则将其插入sqlite表保存;
- 2、区间离散化操作;
- 3、过滤标签;
- 4、后续得到实体的向量结果后进行cos计算操作，估量标签;
- 5、排序得到标签结果;

=================
- operations of processing  data sets: parse the RDF data set, and save it into the sqlite table according to certain rules;
- Interval discretization operation;
- Filter labels;
- perform the cos calculation operation to estimate the label after generating the vector result of the entity, ;
- Sort the label result and obtain the result;

## Part2 (python关键部分有以下几小点)：

- 1、H路径：H-path 用于进行基于同质性的路径生成，单独的H方法生成实体向量；
- 2、A路径：A-path 用于进行基于属性相似性的路径生成，以及单独的A方法生成实体向量；
- 3、S路径：S-path 用于进行基于结构相似性的路径生成，以及单独的S方法生成实体向量；
- 4、HAS联合路径：HAS_combine_seq.py 该文件用于进行将所有实体节点的路进行融合，而后使用语言模型的方式；
- 5、findneighbors.py:用于找cube中的邻居；
- 6、其余的process用于处理sqlite表中数据信息，划分为了string类型标签和数值类型标签；

==================
- H path:generating H-path based on homogeneity, and H method generates entity vectors;
- A path:generating A-path based on attribute similarity, and A method to generate entity vectors;
- S path:generating S-path based on structural similarity, and S method to generate entity vectors;
- HAS combine path: HAS_combine_seq.py This file is used to merge the paths of all entities, and then use the language model;
- findneighbors.py: used to find neighbors in the cube of a entity;
- The rest of the files is used to process the data information in the sqlite table.For example,dividing labels into string type labels and numeric type labels;

本方法针对rdf/n3/nt...等一些数据格式的数据集进行的数据解析存储操作，其他训练相关相关信息可参见deepwalk；(HAS ->HSP,change the name)
This method performs data analysis and storage operations for some data formats such as rdf/n3/nt..., and other training related information can be found in deepwalk
https://github.com/phanein/deepwalk

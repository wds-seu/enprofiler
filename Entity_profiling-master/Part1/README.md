
# PageRank
Calculate the center of the entity by the pagerank algorithm

#java
Experimental framework of Entity profiling

1.java/RDFDatabase
Parsing raw data(eg:.rdf/.nt/.ttl), the experimental data set is drugbank.nt and building five tables：
    (1)mapping table: Map strings and integers
    (2)types_string table: the type of string_id
    (3)triples_all table: Store all triples
    (4)nodes_type table: Store all  nodes and the type of the node
    (5)types_node table: stroe the type of the all entity

2.java/creat_base_table
Build 3 tables: property_triples_table,property_mid_support,relations_triples.
property_triples_table: Statistics Attribute triple:Subject and object must have type
property_mid_support: Summary table of attribute information,Convenient for later inquiry
relations_triples: Statistics relation triple:Subject and object must have type

3.java/make_support
statistical  support value of attribute (the type of the attribute is string)

4.java/Numerical_discretization
Continuous numerical discretization：
  1.Equal width division
  2.Local density division
statistical  support value of attribute (the type of the attribute is numerical)

5.java/filter_support
Filter attribute values of support(According to your respective needs)

6.java/cosine_similarity
Obtain the vector of the entity through the HAS(H,A,S,HAS) model,
Counting the cosine similarity of an entity with a property set

7.java/salience
Use the pagerank algorithm to find the center of each entity,
Use the center of the entity to find the first relationship label

8.java/filter_relations
Filter relations according to the values of the salience(According to your respective needs)

9.java/relation_property
creat the relation_property labels

10.java/relation_similarity
Obtain the vector of the entity through the HAS(H,A,S,HAS) model,
Counting the cosine similarity of an entity with a relational set

11.java/resort_labels
Label-set reordering,distinctive labels are listed in front,make final tags set

12.java/attach_tags
Label the entity after creating the final tag set



import java.sql.*;
import java.util.*;

public class relation_similarity {
    private static String sqlitePath = "\\link_1.sqlite";
    private static Map<Integer, Double> valueMap = new HashMap<Integer, Double>();
    //relation:计算正类间的相似度
    private static void relation_positive_sim(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建relation_positive_sim表: 节点相似度
            stmt.execute("CREATE TABLE HS_relation_positive_sim (entity_1_type INTEGER NOT NULL,predicate_id  INTEGER NOT NULL," +
                    "entity2_id INTEGER NOT NULL, similarity DOUBLE NOT NULL);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX HS_en1_type ON HS_relation_positive_sim(entity_1_type)");
        }
        db.commit();
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<ArrayList<Integer>, HashSet<Integer>> node_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();

        int  vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        try(
                PreparedStatement relation_Stmt = db.prepareStatement("SELECT entity1_id, predicate_id, entity2_id, entity1_type_id FROM filter_relation; ");
                PreparedStatement relations_Stmt = db.prepareStatement("SELECT DISTINCT predicate_id, entity2_id, entity1_type_id FROM filter_relation; ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_HS; ");
                PreparedStatement insert_relation_sim_Stmt = db.prepareStatement("INSERT INTO HS_relation_positive_sim " +
                        "(entity_1_type,predicate_id,entity2_id,similarity) VALUES (?,?,?,?);")
        )
        {
            ResultSet rel_str = relation_Stmt.executeQuery();
            while(rel_str.next()) {
                HashSet<Integer> node_set = new HashSet<Integer>();
                ArrayList<Integer> node_list = new ArrayList<Integer>();
                Integer en1_id = rel_str.getInt(1);
                Integer pro_id = rel_str.getInt(2);
                Integer en2_id = rel_str.getInt(3);
                Integer en1_type = rel_str.getInt(4);
                node_list.add(pro_id);
                node_list.add(en2_id);
                node_list.add(en1_type);
                if (!node_Map.containsKey(node_list)) {
                    node_set.add(en1_id);
                    node_Map.put(node_list, node_set);
                } else {
                    node_set = node_Map.get(node_list);
                    node_set.add(en1_id);
                    node_Map.put(node_list, node_set);
                }
            }
            //[entity_id]--[vector]
            ResultSet vec_value = en_vec_Stmt.executeQuery();
            while (vec_value.next()) {
                Integer en_id = vec_value.getInt(1);
                vec = vec_value.getString(2);
                strarray = vec.split(" ");
                int size = strarray.length;
                array = new double[size];
                for (int i = 0; i < size; i++) {
                    array[i] = Double.parseDouble(strarray[i]);
                }
                vec_size = size;
                vectorMap.put(en_id, array);
            }

            ResultSet relation = relations_Stmt.executeQuery();
            while(relation.next()){
                Integer pro_id = relation.getInt(1);
                Integer en2_id = relation.getInt(2);
                Integer en1_type = relation.getInt(3);

                //正类的实体
                ArrayList<Integer> node_list_1 = new ArrayList<Integer>();
                HashSet<Integer> node_set_1 = new HashSet<Integer>();
                node_list_1.add(pro_id);
                node_list_1.add(en2_id);
                node_list_1.add(en1_type);
                node_set_1 = node_Map.get(node_list_1);
                if(node_set_1.size()<=10)
                    re = 0.0;
                else
                    re = cosine_positive(node_set_1, vectorMap, vec_size); //计算余弦相似度
                System.out.println(re);
                insert_relation_sim_Stmt.setInt(1,en1_type);
                insert_relation_sim_Stmt.setInt(2,pro_id);
                insert_relation_sim_Stmt.setInt(3,en2_id);
                insert_relation_sim_Stmt.setDouble(4,re);
                insert_relation_sim_Stmt.execute();
            }
        }
        db.commit();
    }
    //relation:计算正负类间的相似度
    private static void relation_negative_sim(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建relation_negative_sim表: 节点相似度
            stmt.execute("CREATE TABLE HS_relation_negative_sim (entity_1_type INTEGER NOT NULL,predicate_id  INTEGER NOT NULL," +
                    "entity2_id INTEGER NOT NULL, similarity DOUBLE NOT NULL);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX HS_en1_type_1 ON HS_relation_negative_sim(entity_1_type)");
        }
        db.commit();
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> node_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();

        int  vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        try(
                PreparedStatement relation_Stmt = db.prepareStatement("SELECT entity1_id, predicate_id, entity2_id, entity1_type_id FROM filter_relation; ");
                PreparedStatement relations_Stmt = db.prepareStatement("SELECT DISTINCT predicate_id, entity2_id, entity1_type_id FROM filter_relation; ");
                PreparedStatement entity_negative_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type WHERE (type_id=1858106); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_HS; ");
                PreparedStatement insert_relation_sim_Stmt = db.prepareStatement("INSERT INTO HS_relation_negative_sim " +
                        "(entity_1_type,predicate_id,entity2_id,similarity) VALUES (?,?,?,?);")
        )
        {
            ResultSet rel_str = relation_Stmt.executeQuery();
            while(rel_str.next()) {
                HashSet<Integer> node_set = new HashSet<Integer>();
                ArrayList<Integer> node_list = new ArrayList<Integer>();
                Integer en1_id = rel_str.getInt(1);
                Integer pro_id = rel_str.getInt(2);
                Integer en2_id = rel_str.getInt(3);
                Integer en1_type = rel_str.getInt(4);
                node_list.add(pro_id);
                node_list.add(en2_id);
                node_list.add(en1_type);
                if (!node_Map.containsKey(node_list)) {
                    node_set.add(en1_id);
                    node_Map.put(node_list, node_set);
                } else {
                    node_set = node_Map.get(node_list);
                    node_set.add(en1_id);
                    node_Map.put(node_list, node_set);
                }
            }
            //[entity_id]--[vector]
            ResultSet vec_value = en_vec_Stmt.executeQuery();
            while (vec_value.next()) {
                Integer en_id = vec_value.getInt(1);
                vec = vec_value.getString(2);
                strarray = vec.split(" ");
                int size = strarray.length;
                array = new double[size];
                for (int i = 0; i < size; i++) {
                    array[i] = Double.parseDouble(strarray[i]);
                }
                vec_size = size;
                vectorMap.put(en_id, array);
            }
            //[type]--[node]
            ResultSet type_node = entity_negative_Stmt.executeQuery();
            while (type_node.next()) {
                HashSet<Integer> node_set = new HashSet<Integer>();
                Integer node_id = type_node.getInt(1);
                Integer node_type = type_node.getInt(2);

                if (!nodeMap.containsKey(node_type)){
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
                else{
                    node_set = nodeMap.get(node_type);
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
            }

            ResultSet relation = relations_Stmt.executeQuery();
            while(relation.next()){
                Integer pro_id = relation.getInt(1);
                Integer en2_id = relation.getInt(2);
                Integer en1_type = relation.getInt(3);

                //正类的实体
                ArrayList<Integer> node_list_1 = new ArrayList<Integer>();
                HashSet<Integer> node_positive_set = new HashSet<Integer>();
                HashSet<Integer> node_all_set = new HashSet<Integer>();
                HashSet<Integer> node_negetive_set = new HashSet<Integer>();
                node_list_1.add(pro_id);
                node_list_1.add(en2_id);
                node_list_1.add(en1_type);
                node_positive_set = node_Map.get(node_list_1);
                //负类的实体
                node_all_set = nodeMap.get(en1_type);
                node_negetive_set.clear();
                node_negetive_set.addAll(node_all_set);
                node_negetive_set.removeAll(node_positive_set);
                if(node_positive_set.size()<=10)
                    re = 0.0;
                else
                    re = cosine_negative(node_all_set,node_positive_set,node_negetive_set, vectorMap, vec_size); //计算余弦相似度
                System.out.println(re);
                insert_relation_sim_Stmt.setInt(1,en1_type);
                insert_relation_sim_Stmt.setInt(2,pro_id);
                insert_relation_sim_Stmt.setInt(3,en2_id);
                insert_relation_sim_Stmt.setDouble(4,re);
                insert_relation_sim_Stmt.execute();
            }
        }
        db.commit();
    }
    //relation_property:计算属性值类型为string的相似度
   private static void rel_pro_str_sim(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建rel_pro_sim_str表: 节点相似度
            stmt.execute("CREATE TABLE H_relation_string_sim (entity1_type INT NOT NULL ,predicate_id INT,property_id INT ,property_value INT," +
                    "s_s_sim DOUBLE NOT NULL, s_w_sim DOUBLE NOT NULL,s_c_sim DOUBLE NOT NULL);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX H_e_type ON H_relation_string_sim(entity1_type)");
        }
        db.commit();

        String vec = null;
        String []strarray;
        double []array;
        double s_s_sim, s_w_sim, s_c_sim;
        int  vec_size = 0;
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, Integer> en_m_Map = new HashMap<ArrayList<Integer>, Integer>();
        Map<ArrayList<Integer>, Integer> en_n_Map = new HashMap<ArrayList<Integer>, Integer>();
        try(    PreparedStatement relation_pro_Stmt = db.prepareStatement("SELECT DISTINCT entity1_type_id, entity1_id,predicate_id,entity2_id, property_id,property_value FROM H_relation_property_str; ");
                PreparedStatement re_pro_Stmt = db.prepareStatement("SELECT DISTINCT entity1_type_id,predicate_id,property_id,property_value FROM H_relation_property_str; ");
                PreparedStatement entity_negative_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type_new where (type_id=17 OR type_id=106 OR type_id=2188 OR type_id=1887 OR type_id=7112); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_H; ");
                PreparedStatement insert_relation_sim_Stmt = db.prepareStatement("INSERT INTO H_relation_string_sim " +
                        "(entity1_type,predicate_id,property_id,property_value,s_s_sim,s_w_sim,s_c_sim) VALUES (?,?,?,?,?,?,?);")
        )
        {
            //[en]
            ResultSet entity = relation_pro_Stmt.executeQuery();
            while(entity.next()) {
                HashSet<Integer> en_set = new HashSet<Integer>();
                ArrayList<Integer> en_list = new ArrayList<Integer>();
                ArrayList<Integer> en_m_list = new ArrayList<Integer>();
                ArrayList<Integer> en_n_list = new ArrayList<Integer>();

                Integer entity1_type_id = entity.getInt(1);
                Integer entity1_id = entity.getInt(2);
                Integer predicate_id = entity.getInt(3);
                Integer entity2_id = entity.getInt(4);
                Integer property_id = entity.getInt(5);
                Integer property_value = entity.getInt(6);

                en_list.add(entity1_type_id);
                en_list.add(predicate_id);
                en_list.add(property_id);
                en_list.add(property_value);
                if (!en_Map.containsKey(en_list)) {
                    en_set.add(entity1_id);
                    en_Map.put(en_list, en_set);
                } else {
                    en_set = en_Map.get(en_list);
                    en_set.add(entity1_id);
                    en_Map.put(en_list, en_set);
                }
                en_m_list.add(entity1_type_id);
                en_m_list.add(entity1_id);
                en_m_list.add(predicate_id);
                en_m_list.add(property_id);
                en_m_list.add(property_value);
                if (!en_m_Map.containsKey(en_m_list)) {
                    en_m_Map.put(en_m_list, 1);
                } else {
                    en_m_Map.put(en_m_list, en_m_Map.get(en_m_list)+1);
                }
                en_n_list.add(entity1_type_id);
                en_n_list.add(entity1_id);
                en_n_list.add(predicate_id);
                en_n_list.add(property_id);
                if (!en_n_Map.containsKey(en_n_list)) {
                    en_n_Map.put(en_n_list, 1);
                } else {
                    en_n_Map.put(en_n_list, en_n_Map.get(en_n_list)+1);
                }

            }
            //[type]--[node]
            ResultSet type_node = entity_negative_Stmt.executeQuery();
            while (type_node.next()) {
                HashSet<Integer> node_set = new HashSet<Integer>();
                Integer node_id = type_node.getInt(1);
                Integer node_type = type_node.getInt(2);

                if (!nodeMap.containsKey(node_type)){
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
                else{
                    node_set = nodeMap.get(node_type);
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
            }
            //[entity_id]--[vector]
            ResultSet vec_value = en_vec_Stmt.executeQuery();
            while (vec_value.next()) {
                Integer en_id = vec_value.getInt(1);
                vec = vec_value.getString(2);
                strarray = vec.split(" ");
                int size = strarray.length;
                array = new double[size];
                for (int i = 0; i < size; i++) {
                    array[i] = Double.parseDouble(strarray[i]);
                }
                vec_size = size;
                vectorMap.put(en_id, array);
            }

            ResultSet relation_pro_str = re_pro_Stmt.executeQuery();
            while(relation_pro_str.next()){
                s_c_sim=s_w_sim=s_s_sim=0.0;
                HashSet<Integer> strong_node_set = new HashSet<Integer>();
                HashSet<Integer> weak_node_set = new HashSet<Integer>();
                HashSet<Integer> node_negetive_set = new HashSet<Integer>();
                HashSet<Integer> node_all_set = new HashSet<Integer>();
                ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();
                HashSet<Integer> en1_set = new HashSet<Integer>();
                int type = relation_pro_str.getInt(1);
                int pre = relation_pro_str.getInt(2);
                int pro = relation_pro_str.getInt(3);
                int pro_value = relation_pro_str.getInt(4);
                type_pre_pro.add(type);
                type_pre_pro.add(pre);
                type_pre_pro.add(pro);
                type_pre_pro.add(pro_value);
                en1_set = en_Map.get(type_pre_pro);
                for (Integer en:en1_set){
                    int m = 0, n = 0;
                    ArrayList<Integer> en_m = new ArrayList<Integer>();
                    en_m.add(type);
                    en_m.add(en);
                    en_m.add(pre);
                    en_m.add(pro);
                    en_m.add(pro_value);
                    if (!en_m_Map.containsKey(en_m))
                        continue;
                    else {
                        m = en_m_Map.get(en_m);
                    }
                    ArrayList<Integer> en_n = new ArrayList<Integer>();
                    en_n.add(type);
                    en_n.add(en);
                    en_n.add(pre);
                    en_n.add(pro);
                    if (!en_n_Map.containsKey(en_n))
                        continue;
                    else {
                        n = en_n_Map.get(en_n);
                    }
                    if ((m*1.0)/(n*1.0)>=0.5)
                        strong_node_set.add(en);
                    else
                        weak_node_set.add(en);
                }
                //负类实体
                node_all_set = nodeMap.get(type);
                node_negetive_set.clear();
                node_negetive_set.addAll(node_all_set);
                node_negetive_set.removeAll(strong_node_set);
                node_negetive_set.removeAll(weak_node_set);
                //计算余弦相似度
                if (strong_node_set.size()==0) {
                    continue;
                }
                if (weak_node_set.size()==0 && node_negetive_set.size()==0){
                    //全是强正类，没有弱正类，也没有负类
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = 0.0;
                    s_c_sim = 0.0;
                }
                else if (weak_node_set.size()==0 && node_negetive_set.size()!=0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = 0.0;
                    s_c_sim = cosine_negative(node_all_set,strong_node_set,node_negetive_set,vectorMap,vec_size);
                }
                else if (weak_node_set.size()!=0 && node_negetive_set.size()==0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = cosine_negative(node_all_set,strong_node_set,weak_node_set,vectorMap,vec_size);
                    s_c_sim = 0.0;
                }
                else if (weak_node_set.size()!=0 && node_negetive_set.size()!=0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = cosine_negative(node_all_set,strong_node_set,weak_node_set,vectorMap,vec_size);
                    s_c_sim = cosine_negative(node_all_set,strong_node_set,node_negetive_set,vectorMap,vec_size);
                }
                System.out.println("s_s" +s_s_sim +"s_w" + s_w_sim + "s_c" + s_c_sim);
                insert_relation_sim_Stmt.setInt(1,type);
                insert_relation_sim_Stmt.setInt(2,pre);
                insert_relation_sim_Stmt.setInt(3,pro);
                insert_relation_sim_Stmt.setInt(4,pro_value);
                insert_relation_sim_Stmt.setDouble(5,s_s_sim);
                insert_relation_sim_Stmt.setDouble(6,s_w_sim);
                insert_relation_sim_Stmt.setDouble(7,s_c_sim);
                insert_relation_sim_Stmt.execute();
            }
        }
        db.commit();
    }
    //relation_property:计算属性值类型为num的相似度
    private static void rel_pro_num_sim(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建relation_numerical_sim表: 节点相似度
            stmt.execute("CREATE TABLE H_relation_numerical_sim (entity1_type INT NOT NULL ,predicate_id INT,property_id INT ,property_value_range CHAR ," +
                    "s_s_sim DOUBLE NOT NULL, s_w_sim DOUBLE NOT NULL,s_c_sim DOUBLE NOT NULL);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX H_rela_pro_num_e_type ON H_relation_numerical_sim(entity1_type)");
        }
        db.commit();

        String vec = null;
        String []strarray;
        double []array;
        double s_s_sim, s_w_sim, s_c_sim;
        int  vec_size = 0;
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        Map<ArrayList<String>, HashSet<Integer>> en_Map = new HashMap<ArrayList<String>, HashSet<Integer>>();
        Map<ArrayList<String>, Integer> en_m_Map = new HashMap<ArrayList<String>, Integer>();
        Map<ArrayList<Integer>, Integer> en_n_Map = new HashMap<ArrayList<Integer>, Integer>();
        try(    PreparedStatement relation_pro_Stmt = db.prepareStatement("SELECT DISTINCT entity1_type, entity1_id,predicate_id,entity2_id, property_id,property_value_range FROM H_relation_property_num; ");
                PreparedStatement re_pro_Stmt = db.prepareStatement("SELECT DISTINCT entity1_type,predicate_id,property_id,property_value_range FROM H_relation_property_num; ");
                PreparedStatement entity_negative_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type_new where (type_id=17 OR type_id=106 OR type_id=2188 OR type_id=1887 OR type_id=7112); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_H; ");
                PreparedStatement insert_relation_sim_Stmt = db.prepareStatement("INSERT INTO H_relation_numerical_sim " +
                        "(entity1_type,predicate_id,property_id,property_value_range,s_s_sim,s_w_sim,s_c_sim) VALUES (?,?,?,?,?,?,?);")
        )
        {
            //[en]
            ResultSet entity = relation_pro_Stmt.executeQuery();
            while(entity.next()) {
                HashSet<Integer> en_set = new HashSet<Integer>();
                ArrayList<String> en_list = new ArrayList<String>();
                ArrayList<String> en_m_list = new ArrayList<String>();
                ArrayList<Integer> en_n_list = new ArrayList<Integer>();

                Integer entity1_type_id = entity.getInt(1);
                Integer entity1_id = entity.getInt(2);
                Integer predicate_id = entity.getInt(3);
                Integer entity2_id = entity.getInt(4);
                Integer property_id = entity.getInt(5);
                String property_value_range = entity.getString(6);

                en_list.add((entity1_type_id).toString());
                en_list.add((predicate_id).toString());
                en_list.add((property_id).toString());
                en_list.add(property_value_range);
                if (!en_Map.containsKey(en_list)) {
                    en_set.add(entity1_id);
                    en_Map.put(en_list, en_set);
                } else {
                    en_set = en_Map.get(en_list);
                    en_set.add(entity1_id);
                    en_Map.put(en_list, en_set);
                }
                en_m_list.add((entity1_type_id).toString());
                en_m_list.add((entity1_id).toString());
                en_m_list.add((predicate_id).toString());
                en_m_list.add((property_id).toString());
                en_m_list.add(property_value_range);
                if (!en_m_Map.containsKey(en_m_list)) {
                    en_m_Map.put(en_m_list, 1);
                } else {
                    en_m_Map.put(en_m_list, en_m_Map.get(en_m_list)+1);
                }
                en_n_list.add(entity1_type_id);
                en_n_list.add(entity1_id);
                en_n_list.add(predicate_id);
                en_n_list.add(property_id);
                if (!en_n_Map.containsKey(en_n_list)) {
                    en_n_Map.put(en_n_list, 1);
                } else {
                    en_n_Map.put(en_n_list, en_n_Map.get(en_n_list)+1);
                }
            }
            //[type]--[node]
            ResultSet type_node = entity_negative_Stmt.executeQuery();
            while (type_node.next()) {
                HashSet<Integer> node_set = new HashSet<Integer>();
                Integer node_id = type_node.getInt(1);
                Integer node_type = type_node.getInt(2);

                if (!nodeMap.containsKey(node_type)){
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
                else{
                    node_set = nodeMap.get(node_type);
                    node_set.add(node_id);
                    nodeMap.put(node_type, node_set);
                }
            }
            //[entity_id]--[vector]
            ResultSet vec_value = en_vec_Stmt.executeQuery();
            while (vec_value.next()) {
                Integer en_id = vec_value.getInt(1);
                vec = vec_value.getString(2);
                strarray = vec.split(" ");
                int size = strarray.length;
                array = new double[size];
                for (int i = 0; i < size; i++) {
                    array[i] = Double.parseDouble(strarray[i]);
                }
                vec_size = size;
                vectorMap.put(en_id, array);
            }

            ResultSet relation_pro_num = re_pro_Stmt.executeQuery();
            while(relation_pro_num.next()){
                s_c_sim=s_w_sim=s_s_sim=0.0;
                HashSet<Integer> strong_node_set = new HashSet<Integer>();
                HashSet<Integer> weak_node_set = new HashSet<Integer>();
                HashSet<Integer> node_negetive_set = new HashSet<Integer>();
                HashSet<Integer> node_all_set = new HashSet<Integer>();
                ArrayList<String> type_pre_pro = new ArrayList<String>();
                HashSet<Integer> en1_set = new HashSet<Integer>();
                Integer type = relation_pro_num.getInt(1);
                Integer pre = relation_pro_num.getInt(2);
                Integer pro = relation_pro_num.getInt(3);
                String pro_value_range = relation_pro_num.getString(4);
                type_pre_pro.add((type).toString());
                type_pre_pro.add((pre).toString());
                type_pre_pro.add((pro).toString());
                type_pre_pro.add(pro_value_range);
                en1_set = en_Map.get(type_pre_pro);
                for (Integer en:en1_set){
                    int m = 0, n = 0;
                    ArrayList<String> en_m = new ArrayList<String>();
                    en_m.add((type).toString());
                    en_m.add((en).toString());
                    en_m.add((pre).toString());
                    en_m.add((pro).toString());
                    en_m.add(pro_value_range);
                    if (!en_m_Map.containsKey(en_m))
                        continue;
                    else {
                        m = en_m_Map.get(en_m);
                    }
                    ArrayList<Integer> en_n = new ArrayList<Integer>();
                    en_n.add(type);
                    en_n.add(en);
                    en_n.add(pre);
                    en_n.add(pro);
                    if (!en_n_Map.containsKey(en_n))
                        continue;
                    else {
                        n = en_n_Map.get(en_n);
                    }
                    if ((m*1.0)/(n*1.0)>=0.5)
                        strong_node_set.add(en);
                    else
                        weak_node_set.add(en);
                }
                //负类实体
                node_all_set = nodeMap.get(type);
                node_negetive_set.clear();
                node_negetive_set.addAll(node_all_set);
                node_negetive_set.removeAll(strong_node_set);
                node_negetive_set.removeAll(weak_node_set);
                //计算余弦相似度
                if (strong_node_set.size()==0) {
                    continue;
                }
                if (weak_node_set.size()==0 && node_negetive_set.size()==0){
                    //全是强正类，没有弱正类，也没有负类
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = 0.0;
                    s_c_sim = 0.0;
                }
                else if (weak_node_set.size()==0 && node_negetive_set.size()!=0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = 0.0;
                    s_c_sim = cosine_negative(node_all_set,strong_node_set,node_negetive_set,vectorMap,vec_size);
                }
                else if (weak_node_set.size()!=0 && node_negetive_set.size()==0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = cosine_negative(node_all_set,strong_node_set,weak_node_set,vectorMap,vec_size);
                    s_c_sim = 0.0;
                }
                else if (weak_node_set.size()!=0 && node_negetive_set.size()!=0){
                    s_s_sim = cosine_positive(strong_node_set,vectorMap,vec_size);
                    s_w_sim = cosine_negative(node_all_set,strong_node_set,weak_node_set,vectorMap,vec_size);
                    s_c_sim = cosine_negative(node_all_set,strong_node_set,node_negetive_set,vectorMap,vec_size);
                }
                System.out.println("s_s" +s_s_sim +"s_w" + s_w_sim + "s_c" + s_c_sim);
                insert_relation_sim_Stmt.setInt(1,type);
                insert_relation_sim_Stmt.setInt(2,pre);
                insert_relation_sim_Stmt.setInt(3,pro);
                insert_relation_sim_Stmt.setString(4,pro_value_range);
                insert_relation_sim_Stmt.setDouble(5,s_s_sim);
                insert_relation_sim_Stmt.setDouble(6,s_w_sim);
                insert_relation_sim_Stmt.setDouble(7,s_c_sim);
                insert_relation_sim_Stmt.execute();
            }
        }
        db.commit();
    }
    //正类间余弦相似性
    private static Double cosine_positive(HashSet<Integer> type_pre_pro_en_1, Map<Integer, double[]> vectorMap, int vec_size){
        ArrayList<Integer> en_list = new ArrayList<Integer>(type_pre_pro_en_1);
        valueMap.clear();
        int length = en_list.size();
        int num = 0, entity_1, entity_2;
        double result = 0.0, sum, sum_1;
        double temp_1[], temp_2[], temp_3[];
        for (Integer anEn_list : en_list) {
            if (!vectorMap.containsKey(anEn_list)) {
                continue;
            }
            temp_1 = vectorMap.get(anEn_list);
            sum = 0.0;
            for (int i = 0; i < vec_size; i++) {
                sum += Math.pow(temp_1[i], 2);
            }
            valueMap.put(anEn_list, Math.sqrt(sum));
        }
        if (valueMap.size()==0){
            return 0.0;
        }
        if (length<=10){
            num++;
            result = 0.0;
        }
        else{
            for (int i =0;i<length-1;i++) {
                entity_1 = en_list.get(i);
                if (!vectorMap.containsKey(entity_1)) {
                    continue;
                }
                temp_2 = vectorMap.get(entity_1);
                for (int j =i+1;j<length;j++){
                    sum_1 = 0.0;
                    entity_2 = en_list.get(j);
                    if (!vectorMap.containsKey(entity_2)) {
                        continue;
                    }
                    temp_3 = vectorMap.get(entity_2);
                    for (int k=0; k < vec_size; k++) {
                        sum_1 += temp_2[k] * temp_3[k];
                    }
                    if ((!valueMap.containsKey(entity_1))&&(!valueMap.containsKey(entity_2)))
                        continue;
                    result += sum_1/(valueMap.get(entity_1) * valueMap.get(entity_2));
                    num++;
                }
            }
        }
        en_list.clear();
        if (num==0){
            return 0.0;
        }
        else {
            return (result / num);
        }
    }
    //正负类间余弦相似性
    private static Double cosine_negative(HashSet<Integer>node_all_set,HashSet<Integer>node_positive_set,HashSet<Integer>node_negetive_set, Map<Integer, double[]> vectorMap, int vec_size){
        valueMap.clear();
        int num = 0;
        double result = 0.0, sum, sum_1;
        double temp_1[], temp_2[], temp_3[];
        for (Integer node_all : node_all_set) {
            if (!vectorMap.containsKey(node_all))
                continue;
            temp_1 = vectorMap.get(node_all);
            sum = 0.0;
            for (int i = 0; i < vec_size; i++) {
                sum += Math.pow(temp_1[i], 2);
            }
            valueMap.put(node_all, Math.sqrt(sum));
        }
        if (valueMap.size()==0){
            return 0.0;
        }

        if (node_negetive_set.size()==0){
            num++;
            result = 0.0;
        }
        else{
            for (Integer node_positive: node_positive_set) {
                if (!vectorMap.containsKey(node_positive))
                    continue;
                temp_2 = vectorMap.get(node_positive);
                for (Integer node_negative: node_negetive_set){
                    sum_1 = 0.0;
                    if (!vectorMap.containsKey(node_negative))
                        continue;
                    temp_3 = vectorMap.get(node_negative);
                    for (int k=0; k < vec_size; k++) {
                        sum_1 += temp_2[k] * temp_3[k];
                    }
                    if ((!valueMap.containsKey(node_negative))&&(!valueMap.containsKey(node_positive)))
                        continue;
                    result += sum_1/(valueMap.get(node_positive) * valueMap.get(node_negative));
                    num++;
                }
            }
        }
        if (num==0){
            return 0.0;
        }
        else {
            return (result / num);
        }
    }
    //relation:正类间的相似度减去正负类间的相似度
    private static void relation_similarity(Connection db) throws Exception{
        //创建relation_similarity表：关系标签的正类间的相似度与正负类间的相似度的差值
        try(Statement stmt = db.createStatement()){
            stmt.execute("CREATE TABLE HS_relation_similarity (entity_1_type INTEGER NOT NULL,predicate_id  INTEGER NOT NULL," +
                    "entity2_id INTEGER NOT NULL, similarity DOUBLE NOT NULL)");
            stmt.execute("CREATE INDEX HS_rel_sim_dif ON HS_relation_similarity(similarity)");
        }
        db.commit();
        try(PreparedStatement stmt_1 = db.prepareStatement("SELECT entity_1_type,predicate_id,entity2_id, similarity FROM HS_relation_positive_sim");
            PreparedStatement stmt_2 = db.prepareStatement("SELECT similarity FROM HS_relation_negative_sim WHERE entity_1_type=? AND predicate_id=? AND entity2_id=?");
            PreparedStatement stmt_3 = db.prepareStatement("INSERT INTO HS_relation_similarity(entity_1_type,predicate_id,entity2_id,similarity) VALUES(?,?,?,?);")

        ) {
            ResultSet sim_1 = stmt_1.executeQuery();
            double s = 0.0;
            while(sim_1.next()){
                Integer en1_type = sim_1.getInt(1);
                Integer pro_id = sim_1.getInt(2);
                Integer en2_id = sim_1.getInt(3);
                double sim = sim_1.getDouble(4);

                stmt_2.setInt(1,en1_type);
                stmt_2.setInt(2,pro_id);
                stmt_2.setInt(3,en2_id);
                ResultSet en_true_id = stmt_2.executeQuery();
                if (en_true_id.next()){
                    s = en_true_id.getDouble(1);
                }
                double dif = sim - s;

                stmt_3.setInt(1,en1_type);
                stmt_3.setInt(2,pro_id);
                stmt_3.setInt(3,en2_id);
                stmt_3.setDouble(4,dif);
                stmt_3.execute();
            }
        }
        db.commit();
    }
    //relation_property:string
    private static void relation_string_similarity(Connection db) throws Exception{
        //创建relation_string_similarity表
        try(Statement stmt = db.createStatement()){
            stmt.execute("CREATE TABLE relation_string_similarity (entity1_type INTEGER NOT NULL,predicate_id  INTEGER NOT NULL," +
                    "property_id INTEGER NOT NULL,property_value INTEGER NOT NULL, similarity DOUBLE)");
            stmt.execute("CREATE INDEX rel_pro_str_sim_dif ON relation_string_similarity(similarity)");
        }
        db.commit();
        try(PreparedStatement stmt_1 = db.prepareStatement("SELECT entity1_type,predicate_id,property_id,property_value,s_s_sim,s_w_sim,s_c_sim FROM relation_string_sim");
            PreparedStatement stmt_2 = db.prepareStatement("INSERT INTO relation_string_similarity(entity1_type,predicate_id,property_id,property_value,similarity) VALUES(?,?,?,?,?);")
        ) {
            ResultSet sim_1 = stmt_1.executeQuery();
            while(sim_1.next()){
                double dif = 0.0;
                int flag = 0; //s_s_sim>s_w_sim>s_c_sim:flag=1
                Integer en_type = sim_1.getInt(1);
                Integer pre = sim_1.getInt(2);
                Integer pro = sim_1.getInt(3);
                Integer pro_value = sim_1.getInt(4);
                double s_s_sim = sim_1.getDouble(5);
                double s_w_sim = sim_1.getDouble(6);
                double s_c_sim = sim_1.getDouble(7);

                if (s_w_sim==0.0&&s_c_sim==0.0) {
                    dif = s_s_sim;
                    flag = 1;
                }
                else if (s_w_sim == 0.0&&s_c_sim!=0.0){
                    dif = s_s_sim - s_c_sim;
                    if (dif>0)
                        flag = 1;
                }
                else if (s_w_sim != 0&&s_c_sim==0.0){
                    dif = s_s_sim -s_w_sim;
                    if (dif>0)
                        flag = 1;
                }
                else if (s_w_sim != 0.0&&s_c_sim!=0.0){
                    dif = (2.0*s_s_sim -s_w_sim- s_c_sim)/2;
                    if ((s_s_sim -s_w_sim>0)&&(s_w_sim -s_c_sim>0))
                        flag = 1;
                }
                if (flag==1){
                    stmt_2.setInt(1,en_type);
                    stmt_2.setInt(2,pre);
                    stmt_2.setInt(3,pro);
                    stmt_2.setInt(4,pro_value);
                    stmt_2.setDouble(5,dif);
                    stmt_2.execute();
                }
            }
        }
        db.commit();
    }
    //relation_property:numerical
    private static void relation_numerical_similarity(Connection db) throws Exception{
        //创建relation_numerical_similarity表
        try(Statement stmt = db.createStatement()){
            stmt.execute("CREATE TABLE relation_numerical_similarity (entity1_type INTEGER NOT NULL,predicate_id  INTEGER NOT NULL," +
                    "property_id INTEGER NOT NULL,property_value_range CHAR , similarity DOUBLE )");
            stmt.execute("CREATE INDEX rel_pro_int_sim_dif ON relation_numerical_similarity(similarity)");
        }
        db.commit();
        try(PreparedStatement stmt_1 = db.prepareStatement("SELECT entity1_type,predicate_id,property_id,property_value_range,s_s_sim,s_w_sim,s_c_sim FROM relation_numerical_sim");
            PreparedStatement stmt_2 = db.prepareStatement("INSERT INTO relation_numerical_similarity(entity1_type,predicate_id,property_id,property_value_range,similarity) VALUES(?,?,?,?,?);")
        ) {
            ResultSet sim_1 = stmt_1.executeQuery();
            while(sim_1.next()){
                double dif = 0.0;
                int flag = 0; //s_s_sim>s_w_sim>s_c_sim:flag=1
                Integer en_type = sim_1.getInt(1);
                Integer pre = sim_1.getInt(2);
                Integer pro = sim_1.getInt(3);
                String pro_value_range = sim_1.getString(4);
                double s_s_sim = sim_1.getDouble(5);
                double s_w_sim = sim_1.getDouble(6);
                double s_c_sim = sim_1.getDouble(7);

                if (s_w_sim==0.0&&s_c_sim==0.0) {
                    dif = s_s_sim;
                    flag = 1;
                }
                else if (s_w_sim == 0.0&&s_c_sim!=0.0){
                    dif = s_s_sim - s_c_sim;
                    if (dif>0)
                        flag = 1;
                }
                else if (s_w_sim != 0.0&&s_c_sim!=0.0){
                    dif = (2.0*s_s_sim -s_w_sim- s_c_sim)/2;
                    if ((s_s_sim -s_w_sim>0)&&(s_w_sim -s_c_sim>0))
                        flag = 1;
                }
                else if (s_w_sim != 0&&s_c_sim==0.0){
                    dif = s_s_sim -s_w_sim;
                    if (dif>0)
                        flag = 1;
                }
                if (flag==1){
                    stmt_2.setInt(1,en_type);
                    stmt_2.setInt(2,pre);
                    stmt_2.setInt(3,pro);
                    stmt_2.setString(4,pro_value_range);
                    stmt_2.setDouble(5,dif);
                    stmt_2.execute();
                }
            }
        }
        db.commit();
    }

    public static void main(String[] args)throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        relation_positive_sim(db);
        relation_negative_sim(db);
        relation_similarity(db);
    }
}

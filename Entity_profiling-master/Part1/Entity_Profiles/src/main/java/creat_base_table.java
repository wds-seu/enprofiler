
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**此文件代码为了创建需要得表
 * 建立6个基础表：(1)property_triples表: 属性表： 主语、谓语、宾语、主语的类型、宾语的类型
 *              (2)创建property_mid_support表： 类型、该类型下实体的个数、该类型下的属性的个数、该类型下的属性、该属性的主语个数，该属性不同取值的个数、该属性的值，该属性值的个数，support(属性)，support(属性值)
 *             */


public class creat_base_table {

    private static String sqlitePath = "\\linkedmdb_filter_support.sqlite";

    private static void build_property_triples_table(Connection db) throws Exception{
        try(Statement stmt = db.createStatement()){
            //创建property_triples表: 属性三元组表
            stmt.execute("CREATE TABLE property_triples (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,entity_id INTEGER NOT NULL, predicate_id  INTEGER NOT NULL, object_id INTEGER NOT NULL, entity_type_id  INTEGER NOT NULL, object_type_id INTEGER NOT NULL)");
        }

        try(
                //triples_all表中宾语不是uri
                PreparedStatement property_Triples_Stmt = db.prepareStatement("SELECT subject_id, predicate_id, object_id FROM triples_all WHERE  (SELECT string_type_id FROM mapping WHERE id=triples_all.object_id )!=1;");
                PreparedStatement subject_type_Stmt = db.prepareStatement("SELECT type_id FROM nodes_type WHERE  node_id=?;");
                PreparedStatement object_type_Stmt = db.prepareStatement("SELECT string_type_id FROM mapping WHERE  id=?;");
                PreparedStatement stmt = db.prepareStatement("INSERT INTO property_triples (entity_id,predicate_id,object_id,entity_type_id,object_type_id) VALUES (?,?,?,?,?)"))
        {
            ResultSet triples = property_Triples_Stmt.executeQuery();
            while(triples.next()){
                Integer subjectID = triples.getInt(1);
                Integer predicateID = triples.getInt(2);
                Integer objectID = triples.getInt(3);

                //主语的类型
                int sub_type_id;
                subject_type_Stmt.setInt(1, subjectID);
                ResultSet sub_type = subject_type_Stmt.executeQuery();
                if (!sub_type.next()){
                    //sub_type_id=-1;
                    continue;
                }
                else{
                    sub_type_id = sub_type.getInt(1);
                }

                //宾语的类型

                object_type_Stmt.setInt(1, objectID);
                ResultSet object_type = object_type_Stmt.executeQuery();
                int obj_type_id = object_type.getInt(1);

                stmt.setInt(1,subjectID);
                stmt.setInt(2,predicateID);
                stmt.setInt(3,objectID);
                stmt.setInt(4,sub_type_id);
                stmt.setInt(5,obj_type_id);
                stmt.execute();
            }
        }
        db.commit();

        try (
                Statement indexingStmt = db.createStatement();
        ) {
            indexingStmt.execute("CREATE INDEX subject_property_index ON property_triples(entity_id)");
            indexingStmt.execute("CREATE INDEX object_property_index ON property_triples(object_id)");
            indexingStmt.execute("CREATE INDEX entity_type_index ON property_triples(entity_type_id)");
            indexingStmt.execute("CREATE INDEX pre_property_index ON property_triples(predicate_id)");
        }
        db.commit();
    }

    private static void support_property_table(Connection db) throws Exception {
        Map<Integer, HashSet<Integer>> type_entity_Map = new HashMap<Integer, HashSet<Integer>>();
        Map<Integer, HashSet<Integer>> type_pre_Map = new HashMap<Integer, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_entity_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_pro_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_pro_en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();

        try(Statement stmt = db.createStatement()){
            //创建property_mid_support表: 类型、该类型下实体的个数、该类型下的属性的个数、该类型下的属性、该属性的主语个数，该属性不同取值的个数、该属性的值，该属性值的个数，support(属性)，support(属性值)
            /**
             * type_id
             * entity_num
             * property_num
             * property_id
             * property_en_num
             * property_value_num
             * property_value
             * num_property_value
             * support_property
             * support_property_value*/
            stmt.execute("CREATE TABLE property_mid_support (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "type_id INTEGER NOT NULL, entity_num INTEGER NOT NULL, property_num INTEGER NOT NULL, " +
                    "property_id  INTEGER NOT NULL, property_en_num INTEGER NOT NULL, property_value_num INTEGER NOT NULL," +
                    "property_value  INTEGER NOT NULL, num_property_value INTEGER NOT NULL,support_property INTEGER NOT NULL," +
                    "support_property_value INTEGER NOT NULL)");
        }
        db.commit();
        try (PreparedStatement Stmt = db.prepareStatement("SELECT DISTINCT entity_id, predicate_id, object_id,entity_type_id FROM property_triples;");
                //插入property_mid_support表
                PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO property_mid_support (type_id,entity_num,property_num,property_id,property_en_num,property_value_num,property_value,num_property_value,support_property,support_property_value) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ResultSet pro_triples = Stmt.executeQuery();
            while (pro_triples.next()) {
                // property_triples表的所有信息
                Integer entity_typeID = pro_triples.getInt(4);
                Integer entityID = pro_triples.getInt(1);
                Integer predicateID = pro_triples.getInt(2);
                Integer objectID = pro_triples.getInt(3);

                HashSet<Integer> type_entity_set = new HashSet<Integer>();
                HashSet<Integer> type_pre_set = new HashSet<Integer>();
                HashSet<Integer> type_pre_en_set = new HashSet<Integer>();

                HashSet<Integer> type_pre_pro_set = new HashSet<Integer>();
                HashSet<Integer> type_pre_pro_en_set = new HashSet<Integer>();
                ArrayList<Integer> type_pre = new ArrayList<Integer>();
                ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();
                //Map存储[类型--主语]
                if (!type_entity_Map.containsKey(entity_typeID)) {
                    type_entity_set.add(entityID);
                    type_entity_Map.put(entity_typeID, type_entity_set);
                } else {
                    type_entity_set = type_entity_Map.get(entity_typeID);
                    type_entity_set.add(entityID);
                    type_entity_Map.put(entity_typeID, type_entity_set);
                }
                //Map存储[类型--谓语]
                if (!type_pre_Map.containsKey(entity_typeID)) {
                    type_pre_set.add(predicateID);
                    type_pre_Map.put(entity_typeID, type_pre_set);
                } else {
                    type_pre_set = type_pre_Map.get(entity_typeID);
                    type_pre_set.add(predicateID);
                    type_pre_Map.put(entity_typeID, type_pre_set);
                }
                //Map存储[类型_谓语--主语]
                type_pre.add(entity_typeID);
                type_pre.add(predicateID);
                if (!type_pre_entity_Map.containsKey(type_pre)) {
                    type_pre_en_set.add(entityID);
                    type_pre_entity_Map.put(type_pre, type_pre_en_set);
                } else {
                    type_pre_en_set = type_pre_entity_Map.get(type_pre);
                    type_pre_en_set.add(entityID);
                    type_pre_entity_Map.put(type_pre, type_pre_en_set);
                }
                //Map存储[类型_谓语--宾语]
                if (!type_pre_pro_Map.containsKey(type_pre)) {
                    type_pre_pro_set.add(objectID);
                    type_pre_pro_Map.put(type_pre, type_pre_pro_set);
                } else {
                    type_pre_pro_set = type_pre_pro_Map.get(type_pre);
                    type_pre_pro_set.add(objectID);
                    type_pre_pro_Map.put(type_pre, type_pre_pro_set);
                }
                //Map存储[类型_谓语_宾语--主语]
                type_pre_pro.add(entity_typeID);
                type_pre_pro.add(predicateID);
                type_pre_pro.add(objectID);
                if (!type_pre_pro_en_Map.containsKey(type_pre_pro)) {
                    type_pre_pro_en_set.add(entityID);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                } else {
                    type_pre_pro_en_set = type_pre_pro_en_Map.get(type_pre_pro);
                    type_pre_pro_en_set.add(entityID);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                }
            }

            int type, en_num, pro_num, pro_id, pro_en_num, pro_obj_num, pro_value_id, num_pro_value;

            for (Map.Entry<Integer, HashSet<Integer>> entry : type_entity_Map.entrySet()) {
                HashSet<Integer> type_pre_set_1 = new HashSet<Integer>();
                type = entry.getKey(); //统计类型
                //统计该类型下实体的个数
                en_num = entry.getValue().size();
                //统计该类型下属性的个数
                type_pre_set_1 = type_pre_Map.get(type);
                pro_num = type_pre_set_1.size();
                for (Integer pro : type_pre_set_1) {
                    HashSet<Integer> type_pre_pro_set_1 = new HashSet<Integer>();
                    ArrayList<Integer> type_pre_1 = new ArrayList<Integer>();
                    //统计该类型下所有的属性
                    pro_id = pro;
                    //统计该类型下每个属性的实体个数
                    type_pre_1.add(type);
                    type_pre_1.add(pro_id);
                    pro_en_num = type_pre_entity_Map.get(type_pre_1).size();
                    //统计该类型下每个属性有多少不同的取值
                    type_pre_pro_set_1 = type_pre_pro_Map.get(type_pre_1);
                    pro_obj_num = type_pre_pro_set_1.size();
                    for (Integer obj : type_pre_pro_set_1) {
                        ArrayList<Integer> type_pre_pro_1 = new ArrayList<Integer>();
                        //统计该类型下每个属性的具体属性取值
                        pro_value_id = obj;
                        //筛选该类型下的每个属性的不同取值的个数
                        type_pre_pro_1.add(type);
                        type_pre_pro_1.add(pro_id);
                        type_pre_pro_1.add(pro_value_id);
                        num_pro_value = type_pre_pro_en_Map.get(type_pre_pro_1).size();

                        float support_pro = ((float) (pro_en_num) / (float) (en_num));
                        float support_pro_value = ((float) (num_pro_value) / (float) (en_num));
                        //将数据插入property_mide_support表中
                        insert_Stmt.setInt(1,type);
                        insert_Stmt.setInt(2,en_num);
                        insert_Stmt.setInt(3,pro_num);
                        insert_Stmt.setInt(4,pro_id);
                        insert_Stmt.setInt(5,pro_en_num);
                        insert_Stmt.setInt(6,pro_obj_num);
                        insert_Stmt.setInt(7,pro_value_id);
                        insert_Stmt.setInt(8,num_pro_value);
                        insert_Stmt.setFloat(9,support_pro);
                        insert_Stmt.setFloat(10,support_pro_value);
                        insert_Stmt.execute();
                    }
                }
            }
        }
        db.commit();
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX type_id_sup_index ON property_mid_support(type_id)");
            indexingStmt.execute("CREATE INDEX property_id_sup_index ON property_mid_support(property_id)");
            indexingStmt.execute("CREATE INDEX property_value_sup_index ON property_mid_support(property_value)");
            indexingStmt.execute("CREATE INDEX support_property_value_index ON property_mid_support(support_property_value)");
        }
        db.commit();
    }

    private static void build_relation_triples_table(Connection db) throws Exception{
        try(Statement stmt = db.createStatement()){
            //创建relation_triples表: 关系三元组表
            stmt.execute("CREATE TABLE relations_triples (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,entity1_id INTEGER NOT NULL, predicate_id  INTEGER NOT NULL, entity2_id INTEGER NOT NULL, entity1_type_id  INTEGER NOT NULL, entity2_type_id INTEGER NOT NULL)");
        }

        try(
                //筛选triples_all表中三元组，其中主语，谓语，宾语全是uri，且谓语不是rdf/owl/rdfs，谓语不是类型
                PreparedStatement relation_Triples_Stmt = db.prepareStatement("SELECT subject_id, predicate_id, object_id FROM triples_all " +
                        "WHERE (predicate_id NOT IN (SELECT id FROM mapping WHERE content like '%rdf%' OR content like '%owl%' " +
                        "OR content = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'))");
                PreparedStatement type_Stmt = db.prepareStatement("SELECT type_id FROM nodes_type WHERE  node_id=?;");
                PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO relations_triples (entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id) VALUES (?,?,?,?,?)"))
        {
            ResultSet triple = relation_Triples_Stmt.executeQuery();
            while(triple.next()){
                Integer subjectID = triple.getInt(1);
                Integer predicateID = triple.getInt(2);
                Integer objectID = triple.getInt(3);

                //主语的类型
                int sub_type_id;
                type_Stmt.setInt(1, subjectID);
                ResultSet sub_type = type_Stmt.executeQuery();
                if (!sub_type.next()){
                    //sub_type_id=-1;
                    continue;
                }
                else{
                    sub_type_id = sub_type.getInt(1);
                }

                //宾语的类型
                int obj_type_id;
                type_Stmt.setInt(1, objectID);
                ResultSet object_type = type_Stmt.executeQuery();
                if (!object_type.next()){
                    //obj_type_id=-1;
                    continue;
                }
                else{
                    obj_type_id = object_type.getInt(1);
                }

                insert_Stmt.setInt(1,subjectID);
                insert_Stmt.setInt(2,predicateID);
                insert_Stmt.setInt(3,objectID);
                insert_Stmt.setInt(4,sub_type_id);
                insert_Stmt.setInt(5,obj_type_id);
                insert_Stmt.execute();
            }
        }
        db.commit();

        try (
                Statement indexingStmt = db.createStatement();
        ) {
            indexingStmt.execute("CREATE INDEX entity1_relations_index ON relations_triples(entity1_id)");
            indexingStmt.execute("CREATE INDEX entity1_type_relations_index ON relations_triples(entity1_type_id)");
            indexingStmt.execute("CREATE INDEX entity2_relation_index ON relations_triples(entity2_id)");
            indexingStmt.execute("CREATE INDEX predicate_relation_index ON relations_triples(predicate_id)");

        }
        db.commit();
    }

    public static void main(String[] args) throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        //build_property_triples_table(db);
        support_property_table(db);
        //build_relation_triples_table(db);

    }
}


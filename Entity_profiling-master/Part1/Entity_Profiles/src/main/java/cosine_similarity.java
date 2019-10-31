
import java.sql.*;
import java.util.*;

//计算相似度
public class cosine_similarity {

    private static String sqlitePath = "**.sqlite";
    private static Map<Integer, Double> valueMap = new HashMap<Integer, Double>();

    //string:计算正类间的相似度
    private static void str_sim_positive(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建pro_str_similarity表: 节点相似度
            stmt.execute("CREATE TABLE A_str_positive_sim (entity_type INT NOT NULL ,property_id INT,property_value INT,similarity DOUBLE);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX A_simi_id ON A_str_positive_sim(similarity)");
        }
        db.commit();

        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_pro_en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();

        int vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        try(
                PreparedStatement pro_str_Stmt = db.prepareStatement("SELECT type_id, property_id, property_value FROM filter_pro_string_sup_new; ");
                PreparedStatement entity_Stmt = db.prepareStatement("SELECT DISTINCT entity_type_id, entity_id, predicate_id, object_id FROM property_triples " +
                        "WHERE (entity_type_id=974 or entity_type_id=151 or entity_type_id=319 or entity_type_id=1245 or entity_type_id=603 or entity_type_id=30 or entity_type_id=61 or entity_type_id=414 or entity_type_id=628 or entity_type_id=2330 ) AND (object_type_id=2 or object_type_id=4); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_A; ");
                PreparedStatement insert_simi_Stmt = db.prepareStatement("INSERT INTO A_str_positive_sim (entity_type,property_id,property_value,similarity) VALUES (?,?,?,?);")
        )
        {
            //将数据库信息存储到内存(提速)
            //[entity_type_id,predicate_id,object_id]--[entity_id]
            ResultSet entity = entity_Stmt.executeQuery();
            while(entity.next()) {
                HashSet<Integer> type_pre_pro_en_set = new HashSet<Integer>();
                ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();

                Integer entity_typeid = entity.getInt(1);
                Integer entityid = entity.getInt(2);
                Integer predicateid = entity.getInt(3);
                Integer objectid = entity.getInt(4);

                type_pre_pro.add(entity_typeid);
                type_pre_pro.add(predicateid);
                type_pre_pro.add(objectid);
                if (!type_pre_pro_en_Map.containsKey(type_pre_pro)) {
                    type_pre_pro_en_set.add(entityid);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                } else {
                    type_pre_pro_en_set = type_pre_pro_en_Map.get(type_pre_pro);
                    type_pre_pro_en_set.add(entityid);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
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
            //计算相似度
            ResultSet pro_str = pro_str_Stmt.executeQuery();
            while(pro_str.next()){
               // vector_test_Map.clear();
                Integer en_type = pro_str.getInt(1);
                Integer pro_id = pro_str.getInt(2);
                Integer pro_value = pro_str.getInt(3);

                //正类的实体
                ArrayList<Integer> type_pre_pro_en_1 = new ArrayList<Integer>();
                HashSet<Integer> type_pre_pro_en_set_1 = new HashSet<Integer>();
                type_pre_pro_en_1.add(en_type);
                type_pre_pro_en_1.add(pro_id);
                type_pre_pro_en_1.add(pro_value);
                type_pre_pro_en_set_1 = type_pre_pro_en_Map.get(type_pre_pro_en_1);
                re = cosine_positive(type_pre_pro_en_set_1, vectorMap, vec_size); //计算余弦相似度
                System.out.println(re);
                insert_simi_Stmt.setInt(1,en_type);
                insert_simi_Stmt.setInt(2,pro_id);
                insert_simi_Stmt.setInt(3,pro_value);
                insert_simi_Stmt.setDouble(4,re);
                insert_simi_Stmt.execute();
            }
        }
        db.commit();
    }
    //string:计算正负类间的相似度
    private static void str_sim_negative(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建pro_str_similarity表: 节点相似度
            stmt.execute("CREATE TABLE A_str_negative_sim (entity_type INT NOT NULL ,property_id INT,property_value INT,similarity DOUBLE NOT NULL);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX A_simi_id_1 ON A_str_negative_sim(similarity)");
        }
        db.commit();

        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_pro_en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();

        int vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        try(
                PreparedStatement pro_str_Stmt = db.prepareStatement("SELECT type_id, property_id, property_value FROM filter_pro_string_sup_new; ");
                PreparedStatement entity_Stmt = db.prepareStatement("SELECT DISTINCT entity_type_id, entity_id, predicate_id, object_id FROM property_triples " +
                        "WHERE  (entity_type_id=974 or entity_type_id=151 or entity_type_id=319 or entity_type_id=1245 or entity_type_id=603 or entity_type_id=30 or entity_type_id=61 or entity_type_id=414 or entity_type_id=628 or entity_type_id=2330 ) AND (object_type_id=2 or object_type_id=4); ");
                PreparedStatement entity_negative_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type_new WHERE  (type_id=974 or type_id=151 or type_id=319 or type_id=1245 or type_id=603 or type_id=30 or type_id=61 or type_id=414 or type_id=628 or type_id=2330); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_A; ");
                PreparedStatement insert_simi_Stmt = db.prepareStatement("INSERT INTO A_str_negative_sim (entity_type,property_id,property_value,similarity) VALUES (?,?,?,?);")
        )
        {
            //将数据库信息存储到内存(提速)
            //[entity_type_id,predicate_id,object_id]--[entity_id]
            ResultSet entity = entity_Stmt.executeQuery();
            while(entity.next()) {
                HashSet<Integer> type_pre_pro_en_set = new HashSet<Integer>();
                ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();

                Integer entity_typeid = entity.getInt(1);
                Integer entityid = entity.getInt(2);
                Integer predicateid = entity.getInt(3);
                Integer objectid = entity.getInt(4);

                type_pre_pro.add(entity_typeid);
                type_pre_pro.add(predicateid);
                type_pre_pro.add(objectid);
                if (!type_pre_pro_en_Map.containsKey(type_pre_pro)) {
                    type_pre_pro_en_set.add(entityid);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                } else {
                    type_pre_pro_en_set = type_pre_pro_en_Map.get(type_pre_pro);
                    type_pre_pro_en_set.add(entityid);
                    type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
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
            //计算相似度
            ResultSet pro_str = pro_str_Stmt.executeQuery();
            while(pro_str.next()){
                Integer en_type = pro_str.getInt(1);
                Integer pro_id = pro_str.getInt(2);
                Integer pro_value = pro_str.getInt(3);

                ArrayList<Integer> type_pre_pro_en_1 = new ArrayList<Integer>();
                HashSet<Integer> node_positive_set = new HashSet<Integer>();
                HashSet<Integer> node_all_set = new HashSet<Integer>();
                HashSet<Integer> node_negetive_set = new HashSet<Integer>();
                type_pre_pro_en_1.add(en_type);
                type_pre_pro_en_1.add(pro_id);
                type_pre_pro_en_1.add(pro_value);
                //正类的实体
                node_positive_set = type_pre_pro_en_Map.get(type_pre_pro_en_1);
                //负类的实体
                node_all_set = nodeMap.get(en_type);
                node_negetive_set.clear();
                node_negetive_set.addAll(node_all_set);
                node_negetive_set.removeAll(node_positive_set);

                re = cosine_negative(node_all_set,node_positive_set,node_negetive_set, vectorMap, vec_size); //计算余弦相似度
                System.out.println(re);
                insert_simi_Stmt.setInt(1,en_type);
                insert_simi_Stmt.setInt(2,pro_id);
                insert_simi_Stmt.setInt(3,pro_value);
                insert_simi_Stmt.setDouble(4,re);
                insert_simi_Stmt.execute();
            }
        }
        db.commit();
    }
    //numerical:计算正类间的相似度
    private static void numerical_sim_positive(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建pro_int_double_similarity表: 节点相似度
            stmt.execute("CREATE TABLE A_numerical_positive_sim (entity_type INT NOT NULL ,property_id INT,property_value_range CHAR ,similarity DOUBLE);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX A_int_simi_id ON A_numerical_positive_sim(similarity)");
        }
        db.commit();
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, Double> mappingMap = new HashMap<Integer, Double>();
        int vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        double value_min;
        double value_max;
        double value;
        int flg; //判断是闭区间还是开区间
        try(
                PreparedStatement pro_int_double_Stmt = db.prepareStatement("SELECT type_id, property_id, property_value_range FROM filter_pro_numerical_sup_new; ");
                PreparedStatement value_Stmt = db.prepareStatement("SELECT id, content FROM mapping " +
                        "WHERE (string_type_id!=4 AND string_type_id!=2 AND string_type_id!=6 AND string_type_id!=13 AND string_type_id!=16 AND string_type_id!=1 AND string_type_id!=63)");
                PreparedStatement en_true_Stmt = db.prepareStatement("SELECT entity_id,object_id FROM property_triples WHERE entity_type_id=? AND predicate_id=? " +
                        "AND (object_type_id!=4 AND object_type_id!=2 AND object_type_id!=6 AND object_type_id!=13 AND object_type_id!=16 AND object_type_id!=63); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_A; ");
                PreparedStatement insert_simi_Stmt = db.prepareStatement("INSERT INTO A_numerical_positive_sim (entity_type,property_id,property_value_range,similarity) VALUES (?,?,?,?);");

        )
        {
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

            ResultSet mapping = value_Stmt.executeQuery();
            while (mapping.next()) {
                Integer id = mapping.getInt(1);
                Double content = mapping.getDouble(2);
                mappingMap.put(id,content);
            }

            ResultSet pro_int = pro_int_double_Stmt.executeQuery();
            while(pro_int.next()){
                flg = 0;
                HashSet<Integer> en_positive_set = new HashSet<Integer>();
                Integer en_type = pro_int.getInt(1);
                Integer pro_id = pro_int.getInt(2);
                String pro_value_range = pro_int.getString(3);
                //将值域转化为数值
                String g[];
                String c[] = pro_value_range.split("\\[",2);
                String d[] = c[1].split(",",2);
                if (d[1].contains("]")){
                    flg = 1;
                    g = d[1].split("]",2);
                }
                else{
                    g= d[1].split("\\)",2);
                }
                value_min = Double.parseDouble(d[0]);
                value_max = Double.parseDouble(g[0]);

                //正类的实体
                en_true_Stmt.setInt(1,en_type);
                en_true_Stmt.setInt(2,pro_id);
                ResultSet en_true_id = en_true_Stmt.executeQuery();
                while(en_true_id.next()){
                    //entity_true：正类的实体
                    Integer entity_true = en_true_id.getInt(1);
                    //取值
                    Integer obj_id = en_true_id.getInt(2);
                    value = mappingMap.get(obj_id);
                    if ((flg == 0 && value >= value_min && value < value_max) || (flg == 1 && value >= value_min && value <= value_max)){
                        en_positive_set.add(entity_true);
                    }
                }
                re = cosine_positive(en_positive_set,vectorMap,vec_size); //计算余弦相似度
                System.out.println(re);
                insert_simi_Stmt.setInt(1,en_type);
                insert_simi_Stmt.setInt(2,pro_id);
                insert_simi_Stmt.setString(3,pro_value_range);
                insert_simi_Stmt.setDouble(4,re);
                insert_simi_Stmt.execute();
            }
        }
        db.commit();
    }
    //numerical:计算正负类间的相似度
    private static void numerical_sim_negative(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            //创建pro_int_double_similarity表: 节点相似度
            stmt.execute("CREATE TABLE A_numerical_negative_sim (entity_type INT NOT NULL ,property_id INT,property_value_range CHAR ,similarity DOUBLE);");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX A_int_simi_id_1 ON A_numerical_negative_sim(similarity)");
        }
        db.commit();
        Map<Integer, double[]> vectorMap = new HashMap<Integer, double[]>();
        Map<Integer, Double> mappingMap = new HashMap<Integer, Double>();
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        int vec_size = 0;
        String vec = null;
        String []strarray;
        double []array;
        double re = 0.0;
        double value_min;
        double value_max;
        double value;
        int flg; //判断是闭区间还是开区间
        try(
                PreparedStatement pro_int_double_Stmt = db.prepareStatement("SELECT type_id, property_id, property_value_range FROM filter_pro_numerical_sup_new; ");
                PreparedStatement value_Stmt = db.prepareStatement("SELECT id, content FROM mapping " +
                        " WHERE (string_type_id!=4 AND string_type_id!=2 AND string_type_id!=6 AND string_type_id!=13 AND string_type_id!=16 AND string_type_id!=1 AND string_type_id!=63)");
                PreparedStatement entity_negative_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type_new WHERE  (type_id=974 or type_id=151 or type_id=319 or type_id=1245 or type_id=603 or type_id=30 or type_id=61 or type_id=414 or type_id=628 or type_id=2330); ");
                PreparedStatement en_true_Stmt = db.prepareStatement("SELECT entity_id,object_id FROM property_triples WHERE entity_type_id=? AND predicate_id=? AND (object_type_id!=4 AND object_type_id!=2 AND object_type_id!=6 AND object_type_id!=13 AND object_type_id!=16  AND object_type_id!=63); ");
                PreparedStatement en_vec_Stmt = db.prepareStatement("SELECT entity_id, vector FROM vec_A; ");
                PreparedStatement insert_simi_Stmt = db.prepareStatement("INSERT INTO A_numerical_negative_sim (entity_type,property_id,property_value_range,similarity) VALUES (?,?,?,?);");
        )
        {
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

            ResultSet mapping = value_Stmt.executeQuery();
            while (mapping.next()) {
                Integer id = mapping.getInt(1);
                Double content = mapping.getDouble(2);
                mappingMap.put(id,content);
            }

            ResultSet pro_int = pro_int_double_Stmt.executeQuery();
            while(pro_int.next()){
                flg = 0;
                HashSet<Integer> node_positive_set = new HashSet<Integer>();
                HashSet<Integer> node_all_set = new HashSet<Integer>();
                HashSet<Integer> node_negetive_set = new HashSet<Integer>();
                Integer en_type = pro_int.getInt(1);
                Integer pro_id = pro_int.getInt(2);
                String pro_value_range = pro_int.getString(3);
                //将值域转化为数值
                String g[];
                String c[] = pro_value_range.split("\\[",2);
                String d[] = c[1].split(",",2);
                if (d[1].contains("]")){
                    flg = 1;
                    g = d[1].split("]",2);
                }
                else{
                    g= d[1].split("\\)",2);
                }
                value_min = Double.parseDouble(d[0]);
                value_max = Double.parseDouble(g[0]);

                //正类的实体
                en_true_Stmt.setInt(1,en_type);
                en_true_Stmt.setInt(2,pro_id);
                ResultSet en_true_id = en_true_Stmt.executeQuery();
                while(en_true_id.next()){
                    //entity_true：正类的实体
                    Integer entity_true = en_true_id.getInt(1);
                    //取值
                    Integer obj_id = en_true_id.getInt(2);
                    value = mappingMap.get(obj_id);
                    if ((flg == 0 && value >= value_min && value < value_max) || (flg == 1 && value >= value_min && value <= value_max)){
                        node_positive_set.add(entity_true);
                    }
                }
                //负类的实体
                node_all_set = nodeMap.get(en_type);
                node_negetive_set.clear();
                node_negetive_set.addAll(node_all_set);
                node_negetive_set.removeAll(node_positive_set);
                re = cosine_negative(node_all_set,node_positive_set,node_negetive_set, vectorMap, vec_size); //计算余弦相似度
                System.out.println(re);
                insert_simi_Stmt.setInt(1,en_type);
                insert_simi_Stmt.setInt(2,pro_id);
                insert_simi_Stmt.setString(3,pro_value_range);
                insert_simi_Stmt.setDouble(4,re);
                insert_simi_Stmt.execute();
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
        if (length==1){
            num++;
            result = 1.0;
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
    //String:正类间的相似度减去正负类间的相似度
    private static void str_similarity(Connection db) throws Exception{
        //创建str_similarity表：值为string类型的属性的正类间的相似度与正负类间的相似度的差值
        try(Statement stmt = db.createStatement()){
            stmt.execute("CREATE TABLE A_str_similarity (entity_type INT NOT NULL ,property_id INT,property_value INT ,similarity DOUBLE NOT NULL )");
            stmt.execute("CREATE INDEX A_sim_dif ON A_str_similarity(similarity)");
        }
        db.commit();
        try(  PreparedStatement stmt_1 = db.prepareStatement("SELECT entity_type,property_id,property_value, similarity FROM A_str_positive_sim");
              PreparedStatement stmt_2 = db.prepareStatement("SELECT similarity FROM A_str_negative_sim WHERE entity_type=? AND property_id=? AND property_value=?");
              PreparedStatement stmt_3 = db.prepareStatement("INSERT INTO A_str_similarity(entity_type,property_id,property_value,similarity) VALUES(?,?,?,?);")

        ) {
            ResultSet sim_1 = stmt_1.executeQuery();
            double s = 0.0;
            while(sim_1.next()){
                Integer en_type = sim_1.getInt(1);
                Integer pro_id = sim_1.getInt(2);
                Integer pro_value = sim_1.getInt(3);
                double sim = sim_1.getDouble(4);

                stmt_2.setInt(1,en_type);
                stmt_2.setInt(2,pro_id);
                stmt_2.setInt(3,pro_value);
                ResultSet en_true_id = stmt_2.executeQuery();
                if (en_true_id.next()){
                    s = en_true_id.getDouble(1);
                }
                double dif = sim - s;

                stmt_3.setInt(1,en_type);
                stmt_3.setInt(2,pro_id);
                stmt_3.setInt(3,pro_value);
                stmt_3.setDouble(4,dif);
                stmt_3.execute();
            }
        }
        db.commit();
    }
    //numerical:正类间的相似度减去正负类间的相似度
    private static void numerical_similarity(Connection db) throws Exception{
        //创建numerical_similarity表：值为numerical_similarity类型的属性的正类间的相似度与正负类间的相似度的差值
        try(Statement stmt = db.createStatement()){
            stmt.execute("CREATE TABLE A_numerical_similarity (entity_type INT NOT NULL,property_id INT,property_value_range CHAR,similarity DOUBLE NOT NULL )");
            stmt.execute("CREATE INDEX A_int_sim_dif ON A_numerical_similarity(similarity)");
        }
        db.commit();
        try(  PreparedStatement stmt_1 = db.prepareStatement("SELECT entity_type,property_id,property_value_range, similarity FROM A_numerical_positive_sim");
              PreparedStatement stmt_2 = db.prepareStatement("SELECT similarity FROM A_numerical_negative_sim WHERE entity_type=? AND property_id=? AND property_value_range=?");
              PreparedStatement stmt_3 = db.prepareStatement("INSERT INTO A_numerical_similarity(entity_type,property_id,property_value_range,similarity) VALUES(?,?,?,?);")

        ) {
            ResultSet sim_1 = stmt_1.executeQuery();
            double s = 0.0;
            while(sim_1.next()){
                Integer en_type = sim_1.getInt(1);
                Integer pro_id = sim_1.getInt(2);
                String pro_value_range = sim_1.getString(3);
                double sim = sim_1.getDouble(4);

                stmt_2.setInt(1,en_type);
                stmt_2.setInt(2,pro_id);
                stmt_2.setString(3,pro_value_range);
                ResultSet en_true_id = stmt_2.executeQuery();
                if (en_true_id.next()){
                    s = en_true_id.getDouble(1);
                }
                double dif = sim - s;

                stmt_3.setInt(1,en_type);
                stmt_3.setInt(2,pro_id);
                stmt_3.setString(3,pro_value_range);
                stmt_3.setDouble(4,dif);
                stmt_3.execute();
            }
        }
        db.commit();
    }
    public static void main(String[] args)throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        str_sim_positive(db); //string:计算正类间的相似度
        str_sim_negative(db);  //string:计算正负类间的相似度
        numerical_sim_positive(db); //numerical:计算正类间的相似度
        numerical_sim_negative(db); //numerical:计算正负类间的相似度
        str_similarity(db); //String:正类间的相似度减去正负类间的相似度
        numerical_similarity(db);//numerical:正类间的相似度减去正负类间的相似度
    }
}


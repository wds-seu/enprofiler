
import java.sql.*;
import java.util.*;
//只排属性关系标签
public class resort_labels {
    private static String sqlitePath = "\\5type_bp.sqlite";
    private static void restore(Connection db) throws Exception{
        int count = 0;

        ArrayList<Double> simi = new ArrayList<Double>();
        ArrayList<Double> simi_backup = new ArrayList<Double>();
        ArrayList<Integer> simi_label = new ArrayList<Integer>();
        Map<Integer, ArrayList<String>> tags_map = new HashMap<Integer, ArrayList<String>>();
        try(
            PreparedStatement string_tags_Stmt = db.prepareStatement("SELECT entity_type, property_id, property_value,similarity FROM HAS_str_tags; ");
            PreparedStatement num_tags_Stmt = db.prepareStatement("SELECT entity_type, property_id, property_value_range,similarity FROM HAS_numerical_tags; ");
            PreparedStatement relations_tags_Stmt = db.prepareStatement("SELECT entity1_type,predicate_id, entity2_id,similarity FROM HAS_relation_tags; ");
        ) {
            ResultSet string_tags = string_tags_Stmt.executeQuery();
            while (string_tags.next()) {
                ArrayList<String> list = new ArrayList<String>();
                Integer entity_type = string_tags.getInt(1);
                Integer property_id = string_tags.getInt(2);
                Integer property_value = string_tags.getInt(3);
                Double similarity = string_tags.getDouble(4);
                list.add("1");
                list.add((entity_type).toString());
                list.add((property_id).toString());
                list.add((property_value).toString());
                tags_map.put(count,list);
                count++;
                simi.add(similarity);
            }
            ResultSet num_tags = num_tags_Stmt.executeQuery();
            while (num_tags.next()) {
                ArrayList<String> list = new ArrayList<String>();
                Integer entity_type = num_tags.getInt(1);
                Integer property_id = num_tags.getInt(2);
                String property_value_range = num_tags.getString(3);
                Double similarity = num_tags.getDouble(4);
                list.add("2");
                list.add((entity_type).toString());
                list.add((property_id).toString());
                list.add(property_value_range);
                tags_map.put(count,list);
                count++;
                simi.add(similarity);
            }
            ResultSet relations_tags = relations_tags_Stmt.executeQuery();
            while (relations_tags.next()) {
                ArrayList<String> list = new ArrayList<String>();
                Integer entity_type = relations_tags.getInt(1);
                Integer predicate_id = relations_tags.getInt(2);
                Integer entity2_id = relations_tags.getInt(3);
                Double similarity = relations_tags.getDouble(4);
                list.add("3");
                list.add((entity_type).toString());
                list.add((predicate_id).toString());
                list.add((entity2_id).toString());
                tags_map.put(count,list);
                count++;
                simi.add(similarity);
            }
            //排序：降序
            simi_backup.addAll(simi);
            // 排列
            simi.sort(Collections.reverseOrder());
            HashSet<Double> set = new HashSet<Double>();
            ArrayList<Double> newList = new ArrayList<Double>();
            for (Double sim:simi) {
                if (set.add(sim))
                    newList.add(sim);
            }
            simi.clear();
            simi.addAll(newList);
            for(int i=0; i<simi.size(); i++){
                double res = simi.get(i);
                for(int j=0; j<simi_backup.size();j++){
                    if (simi_backup.get(j)==res){
                        simi_label.add(j);
                    }
                }
            }
            //标签库重新排序
            sort_label(db,tags_map,simi_label,simi_backup);
        }
        db.commit();
    }
    private static void sort_label(Connection db, Map<Integer, ArrayList<String>> tags_map,ArrayList<Integer> simi_label,ArrayList<Double> simi_backup) throws Exception{
        Map<ArrayList<String>,Double> map = new HashMap<ArrayList<String>,Double>();
        HashSet<Integer> node_set = new HashSet<Integer>();
        Map<ArrayList<Integer>, HashSet<Integer>> string_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<Integer, String> mapping_map = new HashMap<Integer, String>();
        Map<Integer, Integer> nodes_all_map = new HashMap<Integer, Integer>();
        Map<ArrayList<Integer>, HashSet<Integer>> node_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<String>, HashSet<Integer>> en_num_Map = new HashMap<ArrayList<String>, HashSet<Integer>>();
        ArrayList<Double> result_list = new ArrayList<Double>();

        double similarily = 0.0;
        //int node_number = 1; //有类型的实体总个数
        Map<Integer, Integer> node_number = new HashMap<Integer, Integer>();
        double  p = 0.5;
        try (Statement stmt = db.createStatement()) {
            //创建string_tags表: 属性标签库
            stmt.execute("CREATE TABLE HAS_tags_sort (classes INT ,types INT,predicate INT,object CHAR ,score DOUBLE,ranking INT);");
        }
        db.commit();
        try(PreparedStatement node_Stmt = db.prepareStatement("SELECT DISTINCT node_id FROM nodes_type_new where (type_id=17 or type_id=7112 or type_id=106 or type_id=2188 or type_id=1887) ; ");
            PreparedStatement property_Stmt = db.prepareStatement("SELECT DISTINCT entity_id, predicate_id, object_id,entity_type_id FROM property_triples " +
                    "WHERE (entity_type_id=17 or entity_type_id=7112 or entity_type_id=106 or entity_type_id=2188 or entity_type_id=1887 ) AND (object_type_id=2 or object_type_id=4); ");
            PreparedStatement entity_Stmt = db.prepareStatement("SELECT distinct node_id, type_id FROM nodes_type_new where (type_id=17 or type_id=7112 or type_id=106 or type_id=2188 or type_id=1887); ");
            PreparedStatement value_Stmt = db.prepareStatement("SELECT id, content FROM mapping " +
                    "WHERE (string_type_id!=4 AND string_type_id!=2 AND string_type_id!=6 AND string_type_id!=13 AND string_type_id!=16 AND string_type_id!=1 AND string_type_id!=63)");
            PreparedStatement relation_Stmt = db.prepareStatement("SELECT entity1_id, predicate_id, entity2_id, entity1_type_id FROM filter_relation; ");
            PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO HAS_tags_sort (classes,types,predicate,object,score,ranking) VALUES (?,?,?,?,?,?);");
            PreparedStatement en_true_Stmt = db.prepareStatement("SELECT entity_id,object_id FROM property_triples WHERE entity_type_id=? AND predicate_id=? AND" +
                    " (object_type_id!=4 AND object_type_id!=2 AND object_type_id!=6 AND object_type_id!=13 AND object_type_id!=16 AND object_type_id!=63); ");
        ){
            ResultSet node_all = node_Stmt.executeQuery();
            while(node_all.next()) {
                Integer entityid = node_all.getInt(1);
                nodes_all_map.put(entityid,0);
            }
            int pp = 0;
            ResultSet node = entity_Stmt.executeQuery();
            while (node.next()){
                int node_id = node.getInt(1);
                int type_id = node.getInt(2);
                if (!node_number.containsKey(type_id)) {
                    node_number.put(type_id, 1);
                } else {
                    pp = node_number.get(type_id);
                    node_number.put(type_id, pp+1);
                }
            }
            ResultSet mapping = value_Stmt.executeQuery();
            while (mapping.next()) {
                Integer id = mapping.getInt(1);
                String content = mapping.getString(2);
                mapping_map.put(id,content);
            }

            ResultSet property = property_Stmt.executeQuery();
            while(property.next()) {
                HashSet<Integer> type_pre_pro_en_set = new HashSet<Integer>();
                ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();

                Integer entityid = property.getInt(1);
                Integer predicateid = property.getInt(2);
                Integer objectid = property.getInt(3);
                Integer entity_typeid = property.getInt(4);

                type_pre_pro.add(entity_typeid);
                type_pre_pro.add(predicateid);
                type_pre_pro.add(objectid);
                if (!string_Map.containsKey(type_pre_pro)) {
                    type_pre_pro_en_set.add(entityid);
                    string_Map.put(type_pre_pro, type_pre_pro_en_set);
                } else {
                    type_pre_pro_en_set = string_Map.get(type_pre_pro);
                    type_pre_pro_en_set.add(entityid);
                    string_Map.put(type_pre_pro, type_pre_pro_en_set);
                }
            }
            ResultSet rel_str = relation_Stmt.executeQuery();
            while(rel_str.next()) {
                HashSet<Integer> rel_node_set = new HashSet<Integer>();
                ArrayList<Integer> node_list = new ArrayList<Integer>();
                Integer en1_id = rel_str.getInt(1);
                Integer pro_id = rel_str.getInt(2);
                Integer en2_id = rel_str.getInt(3);
                Integer en1_type = rel_str.getInt(4);
                node_list.add(en1_type);
                node_list.add(pro_id);
                node_list.add(en2_id);
                if (!node_Map.containsKey(node_list)) {
                    rel_node_set.add(en1_id);
                    node_Map.put(node_list, rel_node_set);
                } else {
                    rel_node_set = node_Map.get(node_list);
                    rel_node_set.add(en1_id);
                    node_Map.put(node_list, rel_node_set);
                }
            }
            for (int i = 0; i < simi_label.size(); i++) {
                ArrayList<String> list = new ArrayList<String>();
                ArrayList<Integer> str_list = new ArrayList<Integer>();
                ArrayList<String> res_list = new ArrayList<String>();
                ArrayList<String> rel_num_list = new ArrayList<String>();
                int label = simi_label.get(i);
                list = tags_map.get(label);
                int type = Integer.parseInt(list.get(1));
                Integer flag = Integer.parseInt(list.get(0));
                res_list.add((flag).toString());
                res_list.add(list.get(1));
                res_list.add(list.get(2));
                res_list.add(list.get(3));

                rel_num_list.add(list.get(1));
                rel_num_list.add(list.get(2));
                rel_num_list.add(list.get(3));

                similarily = simi_backup.get(label);

                str_list.add(Integer.parseInt(list.get(1)));
                str_list.add(Integer.parseInt(list.get(2)));

                if (flag==1){
                    HashSet<Integer> en_set = new HashSet<Integer>();
                    HashSet<Integer> en_set_1 = new HashSet<Integer>();
                    str_list.add(Integer.parseInt(list.get(3)));
                    double reward,penalty;
                    int num_re_node = 0;
                    en_set = string_Map.get(str_list);
                    for (Integer en_node:en_set){
                        nodes_all_map.put(en_node,nodes_all_map.get(en_node)+1);
                        num_re_node = num_re_node + nodes_all_map.get(en_node);
                    }
                    en_set_1.clear();
                    en_set_1.addAll(node_set);
                    en_set_1.retainAll(en_set);
                    if (node_set.size()==0){
                        penalty = 0.0;
                    }
                    else{
                        penalty = (num_re_node*1.0)/(node_number.get(type)*(i+1));
                    }
                    node_set.addAll(en_set);
                    reward = node_set.size()*1.0/node_number.get(type);
                    double result = similarily + p*reward - (1-p)*penalty;
                    map.put(res_list,result);
                    result_list.add(result);
                }
                else if (flag==2){
                    HashSet<Integer> en_set = new HashSet<Integer>();
                    HashSet<Integer> en_set_1 = new HashSet<Integer>();
                    int flg =0;
                    double value_min,value_max;
                    String range = list.get(3);
                    //将值域转化为数值
                    String g[];
                    String c[] = range.split("\\[",2);
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
                    en_true_Stmt.setInt(1,Integer.parseInt(list.get(1)));
                    en_true_Stmt.setInt(2,Integer.parseInt(list.get(2)));
                    ResultSet en_true_id = en_true_Stmt.executeQuery();
                    while(en_true_id.next()){
                        //entity_true：正类的实体
                        Integer entity_true = en_true_id.getInt(1);
                        //取值
                        Integer obj_id = en_true_id.getInt(2);
                        double value = Double.parseDouble(mapping_map.get(obj_id));
                        if ((flg == 0 && value >= value_min && value < value_max) || (flg == 1 && value >= value_min && value <= value_max)){
                            en_set.add(entity_true);
                        }
                    }
                    double reward,penalty;
                    int num_re_node = 0;
                    for (Integer en_node:en_set){
                        nodes_all_map.put(en_node,nodes_all_map.get(en_node)+1);
                        num_re_node = num_re_node + nodes_all_map.get(en_node);
                    }
                    en_set_1.clear();
                    en_set_1.addAll(node_set);
                    en_set_1.retainAll(en_set);
                    if (node_set.size()==0){
                        penalty = 0.0;
                    }
                    else{
                        penalty = (num_re_node*1.0)/(node_number.get(type)*(i+1));
                    }
                    node_set.addAll(en_set);
                    reward = node_set.size()*1.0/node_number.get(type);
                    double result = similarily + p*reward - (1-p)*penalty;
                    map.put(res_list,result);
                    result_list.add(result);
                }
                else if (flag==3){
                    HashSet<Integer> en_set = new HashSet<Integer>();
                    HashSet<Integer> en_set_1 = new HashSet<Integer>();
                    str_list.add(Integer.parseInt(list.get(3)));
                    double reward,penalty;
                    en_set = node_Map.get(str_list);
                    int num_re_node = 0;
                    for (Integer en_node:en_set){
                        nodes_all_map.put(en_node,nodes_all_map.get(en_node)+1);
                        num_re_node = num_re_node + nodes_all_map.get(en_node);
                    }
                    en_set_1.clear();
                    en_set_1.addAll(node_set);
                    en_set_1.retainAll(en_set);
                    if (node_set.size()==0){
                        penalty = 0.0;
                    }
                    else{
                        penalty = (num_re_node*1.0)/(node_number.get(type)*(i+1));
                    }
                    node_set.addAll(en_set);
                    reward = node_set.size()*1.0/node_number.get(type);
                    double result = similarily + p*reward - (1-p)*penalty;
                    map.put(res_list,result);
                    result_list.add(result);
                }
            }
            result_list.sort(Collections.reverseOrder());
            HashSet<Double> set = new HashSet<Double>();
            ArrayList<Double> newList = new ArrayList<Double>();
            for (Double sim:result_list) {
                if (set.add(sim))
                    newList.add(sim);
            }
            result_list.clear();
            result_list.addAll(newList);
            int score = 1,type=-1,pre=-1;
            String object=null;

            for(int i=0; i<result_list.size(); i++){
                double res = result_list.get(i);
                for (Map.Entry<ArrayList<String>,Double> entry : map.entrySet()) {
                    if (entry.getValue()==res){
                        ArrayList<String> list = entry.getKey();
                        int classes = Integer.parseInt(list.get(0));
                        if (classes==1){
                            type = Integer.parseInt(list.get(1));
                            pre = Integer.parseInt(list.get(2));
                            object = list.get(3);
                        }
                        else if (classes==2){
                            type = Integer.parseInt(list.get(1));
                            pre = Integer.parseInt(list.get(2));
                            object = list.get(3);
                        }
                        else if (classes==3){
                            type = Integer.parseInt(list.get(1));
                            pre = Integer.parseInt(list.get(2));
                            object = list.get(3);
                        }
                        insert_Stmt.setInt(1,classes);
                        insert_Stmt.setInt(2,type);
                        insert_Stmt.setInt(3,pre);
                        insert_Stmt.setString(4, object);
                        insert_Stmt.setDouble(5, res);
                        insert_Stmt.setInt(6, score);
                        insert_Stmt.execute();
                        score++;
                    }
                }
            }
        }
        db.commit();
    }

    public static void main(String[] args) throws Exception{
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        restore(db);
    }

}

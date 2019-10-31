/*Label the entity after creating the final tag set*/
import java.io.FileWriter;
import java.sql.*;
import java.util.*;

///*只贴属性关系标签*/
public class attach_tags {
    private static String sqlitePath = "\\5_labels.sqlite";
    private static void tags(Connection db) throws Exception{

        Map<ArrayList<Integer>, HashSet<Integer>> type_pre_pro_en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, ArrayList<String>> tags_all_map = new HashMap<ArrayList<Integer>, ArrayList<String>>();
        Map<ArrayList<Integer>, HashSet<Integer>> node_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<Integer>, HashSet<Integer>> en_Map = new HashMap<ArrayList<Integer>, HashSet<Integer>>();
        Map<ArrayList<String>, HashSet<Integer>> en_num_Map = new HashMap<ArrayList<String>, HashSet<Integer>>();
        double value_min;
        double value_max;
        double value;
        int flg; //判断是闭区间还是开区间
        try(PreparedStatement string_Stmt = db.prepareStatement("SELECT DISTINCT types,predicate,object FROM HAS_tags_sort WHERE classes=1; ");
            PreparedStatement rela_Stmt = db.prepareStatement("SELECT DISTINCT types,predicate,object FROM HAS_tags_sort WHERE classes=3; ");
            PreparedStatement property_Stmt = db.prepareStatement("SELECT DISTINCT entity_id FROM property_triples where (predicate_id=? and object_id=? and entity_type_id=? ); ");
            PreparedStatement entity_Stmt = db.prepareStatement("SELECT node_id, type_id  FROM nodes_type_new WHERE (type_id=106 or type_id=7112 or type_id=2188 or type_id=1887  or type_id=17); ");
            PreparedStatement relation_Stmt = db.prepareStatement("SELECT DISTINCT entity1_id FROM filter_relation where (predicate_id=? and entity2_id=? and entity1_type_id=?); ");
            PreparedStatement value_Stmt = db.prepareStatement("SELECT content FROM mapping where id=?");
            PreparedStatement en_true_Stmt = db.prepareStatement("SELECT entity_id,object_id FROM property_triples WHERE entity_type_id=? AND predicate_id=? AND (object_type_id!=4 AND object_type_id!=2 AND object_type_id!=6 AND object_type_id!=13 AND object_type_id!=16  AND object_type_id!=63); ");
            PreparedStatement tags_Stmt = db.prepareStatement("SELECT classes,types,predicate, object FROM HAS_tags_sort ORDER BY ranking; ");

        ) {
            ResultSet string =string_Stmt.executeQuery();
            while(string.next()){
                Integer types = string.getInt(1);
                Integer predicate = string.getInt(2);
                Integer object = string.getInt(3);
                property_Stmt.setInt(1,predicate);
                property_Stmt.setInt(2,object);
                property_Stmt.setInt(3,types);
                ResultSet property = property_Stmt.executeQuery();
                while(property.next()){
                    HashSet<Integer> type_pre_pro_en_set = new HashSet<Integer>();
                    ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();
                    Integer entityid = property.getInt(1);
                    type_pre_pro.add(types);
                    type_pre_pro.add(predicate);
                    type_pre_pro.add(object);
                    if (!type_pre_pro_en_Map.containsKey(type_pre_pro)) {
                        type_pre_pro_en_set.add(entityid);
                        type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                    } else {
                        type_pre_pro_en_set = type_pre_pro_en_Map.get(type_pre_pro);
                        type_pre_pro_en_set.add(entityid);
                        type_pre_pro_en_Map.put(type_pre_pro, type_pre_pro_en_set);
                    }
                }
            }
            ResultSet rela =rela_Stmt.executeQuery();
            while(rela.next()) {
                Integer types = rela.getInt(1);
                Integer predicate = rela.getInt(2);
                Integer object = rela.getInt(3);
                relation_Stmt.setInt(1, predicate);
                relation_Stmt.setInt(2, object);
                relation_Stmt.setInt(3,types);
                ResultSet rel_str = relation_Stmt.executeQuery();
                while(rel_str.next()) {
                    HashSet<Integer> node_set = new HashSet<Integer>();
                    ArrayList<Integer> node_list = new ArrayList<Integer>();
                    Integer en1_id = rel_str.getInt(1);
                    node_list.add(types);
                    node_list.add(predicate);
                    node_list.add(object);
                    if (!node_Map.containsKey(node_list)) {
                        node_set.add(en1_id);
                        node_Map.put(node_list, node_set);
                    } else {
                        node_set = node_Map.get(node_list);
                        node_set.add(en1_id);
                        node_Map.put(node_list, node_set);
                    }
                }
            }

            //贴标签
            ResultSet tags = tags_Stmt.executeQuery();
            while (tags.next()) {
                Integer classes = tags.getInt(1);
                Integer entity_type = tags.getInt(2);
                Integer predicate_id = tags.getInt(3);
                String object = tags.getString(4);
                //贴属性为string的标签
                if (classes==1){
                    HashSet<Integer> en_string_set = new HashSet<Integer>();
                    ArrayList<Integer> type_pre_pro = new ArrayList<Integer>();
                    int property_value = Integer.parseInt(object);
                    type_pre_pro.add(entity_type);
                    type_pre_pro.add(predicate_id);
                    type_pre_pro.add(property_value);
                    en_string_set = type_pre_pro_en_Map.get(type_pre_pro);

                    value_Stmt.setInt(1, predicate_id);
                    ResultSet value_real = value_Stmt.executeQuery();
                    String pro = value_real.getString(1);

                    value_Stmt.setInt(1, property_value);
                    ResultSet value_real_1 = value_Stmt.executeQuery();
                    String proper_value = value_real_1.getString(1);

                    String c[] = pro.split("/");
                    String str_tags = c[c.length-1]+" == "+proper_value;

                    for (Integer en:en_string_set){
                        ArrayList<Integer> node_list = new ArrayList<Integer>();
                        ArrayList<String> string_tags_list = new ArrayList<String>();
                        node_list.add(entity_type);
                        node_list.add(en);
                        if (!tags_all_map.containsKey(node_list)){
                            string_tags_list.add(str_tags);
                            tags_all_map.put(node_list,string_tags_list);
                        }
                        else{string_tags_list = tags_all_map.get(node_list);
                            string_tags_list.add("\t"+str_tags);
                            tags_all_map.put(node_list, string_tags_list);
                        }
                    }
                }
                else if (classes==2){
                    //贴属性为num的标签
                    flg = 0;
                    HashSet<Integer> en_num_set = new HashSet<Integer>();

                    //将值域转化为数值
                    String g[];
                    String c[] = object.split("\\[",2);
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
                    en_true_Stmt.setInt(1,entity_type);
                    en_true_Stmt.setInt(2,predicate_id);
                    ResultSet en_true_id = en_true_Stmt.executeQuery();
                    while(en_true_id.next()){
                        //entity_true：正类的实体
                        Integer entity_true = en_true_id.getInt(1);
                        //取值
                        Integer obj_id = en_true_id.getInt(2);

                        value_Stmt.setInt(1, obj_id);
                        ResultSet value_real_2 = value_Stmt.executeQuery();
                        value = value_real_2.getDouble(1);

                        if ((flg == 0 && value >= value_min && value < value_max) || (flg == 1 && value >= value_min && value <= value_max)){
                            en_num_set.add(entity_true);
                        }
                    }
                    value_Stmt.setInt(1, predicate_id);
                    ResultSet value_real_3 = value_Stmt.executeQuery();
                    String pro_num = value_real_3.getString(1);

                    String a[] = pro_num.split("/");
                    String str_tags = a[a.length-1]+" == "+ object;

                    for (Integer en:en_num_set){
                        ArrayList<Integer> node_list = new ArrayList<Integer>();
                        ArrayList<String> string_tags_list = new ArrayList<String>();
                        node_list.add(entity_type);
                        node_list.add(en);
                        if (!tags_all_map.containsKey(node_list)){
                            string_tags_list.add(str_tags);
                            tags_all_map.put(node_list,string_tags_list);
                        }
                        else{string_tags_list = tags_all_map.get(node_list);
                            string_tags_list.add("\t"+str_tags);
                            tags_all_map.put(node_list, string_tags_list);
                        }
                    }
                }
                else if(classes==3){
                    //贴relation标签
                    HashSet<Integer> en_relation_set = new HashSet<Integer>();
                    ArrayList<Integer> re_list = new ArrayList<Integer>();

                    Integer entity2_id = Integer.parseInt(object);
                    re_list.add(entity_type);
                    re_list.add(predicate_id);
                    re_list.add(entity2_id);
                    en_relation_set = node_Map.get(re_list);

                    value_Stmt.setInt(1, predicate_id);
                    ResultSet value_real_4 = value_Stmt.executeQuery();
                    String pre = value_real_4.getString(1);

                    value_Stmt.setInt(1, entity2_id);
                    ResultSet value_real_5 = value_Stmt.executeQuery();
                    String pro_value_5 = value_real_5.getString(1);

                    String c[] = pre.split("/");
                    String str_tags = c[c.length-1]+" == "+pro_value_5;

                    for (Integer en:en_relation_set){
                        ArrayList<Integer> node_list = new ArrayList<Integer>();
                        ArrayList<String> string_tags_list = new ArrayList<String>();
                        node_list.add(entity_type);
                        node_list.add(en);
                        if (!tags_all_map.containsKey(node_list)){
                            string_tags_list.add(str_tags);
                            tags_all_map.put(node_list,string_tags_list);
                        }
                        else{string_tags_list = tags_all_map.get(node_list);
                            string_tags_list.add("\t"+str_tags);
                            tags_all_map.put(node_list, string_tags_list);
                        }
                    }

                }
            }

            String filePath = "\\5_HAS_tags.txt";
            //构造函数中的第二个参数true表示以追加形式写文件
            FileWriter fw = new FileWriter(filePath,true);
            ResultSet entity_all = entity_Stmt.executeQuery();
            while(entity_all.next()) {
                ArrayList<Integer> entity_type = new ArrayList<Integer>();
                StringBuilder result = null;
                int p =0;

                Integer entityid = entity_all.getInt(1);
                Integer type = entity_all.getInt(2);

                entity_type.add(type);
                entity_type.add(entityid);

                value_Stmt.setInt(1, entityid);
                ResultSet value_real_6 = value_Stmt.executeQuery();
                String node = value_real_6.getString(1);
                value_Stmt.setInt(1, type);
                ResultSet value_real_7 = value_Stmt.executeQuery();
                String node_type = value_real_7.getString(1);

                String d[] = node_type.split("/");
                node_type = d[d.length-1];

                result = new StringBuilder((entityid).toString() + "\t" + node_type + "\t" + node);

                if (tags_all_map.containsKey(entity_type)){
                    p=1;
                    ArrayList<String> list =tags_all_map.get(entity_type);
                    for (int i = 0; i < list.size(); i++) {
                        result.append("\t").append(list.get(i));
                    }
                }
                if (p==1){
                    fw.write(result.toString());
                    fw.write("\n");
                }

            }
            fw.close();
        }
        db.commit();

    }

    public static void main(String[] args) throws Exception{
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        tags(db);
    }
}

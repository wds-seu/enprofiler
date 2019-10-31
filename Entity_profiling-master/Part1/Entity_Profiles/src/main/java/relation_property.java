import java.sql.*;
import java.util.*;

public class relation_property {
    private static String sqlitePath = "\\db.sqlite";
    private static void count_relation(Connection db) throws Exception{
        /*try (Statement stmt = db.createStatement()) {
            //创建final_tags表: 属性标签库
            stmt.execute("CREATE TABLE H_relation_top5 (types INT,predicate INT,object INT,object_type INT);");
        }
        db.commit();*/
        Map<Integer, HashSet<Integer>> nodeMap = new HashMap<Integer, HashSet<Integer>>();
        try (   PreparedStatement tags_stmt = db.prepareStatement("SELECT DISTINCT types,predicate,object FROM S_tags_sort WHERE (classes=3 AND types=106)ORDER BY ranking; ");
                PreparedStatement type_stmt = db.prepareStatement("SELECT type_id FROM nodes_type_new WHERE node_id=?; ");
                PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO S_relation_top5 (types,predicate,object,object_type) VALUES (?,?,?,?);");
        ) {
            ResultSet tags = tags_stmt.executeQuery();
            while (tags.next()) {
                Integer entity_type = tags.getInt(1);
                Integer predicate_id = tags.getInt(2);
                Integer object = tags.getInt(3);
                HashSet<Integer> node_set = new HashSet<Integer>();
                if (!nodeMap.containsKey(entity_type)){
                    node_set.add(predicate_id);
                    nodeMap.put(entity_type, node_set);
                }
                else{
                    node_set = nodeMap.get(entity_type);
                    node_set.add(predicate_id);
                    nodeMap.put(entity_type, node_set);
                }
                if (node_set.size()<=5) {
                    type_stmt.setInt(1,object);
                    ResultSet type = type_stmt.executeQuery();
                    int object_type=-1 ;
                    if(type.next()) {
                        object_type = type.getInt(1);
                    }
                    insert_Stmt.setInt(1, entity_type);
                    insert_Stmt.setInt(2, predicate_id);
                    insert_Stmt.setInt(3, object);
                    insert_Stmt.setInt(4, object_type);
                    insert_Stmt.execute();
                }
            }
        }
        db.commit();
    }
    public static void main(String[] args)throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        count_relation(db);
    }
}

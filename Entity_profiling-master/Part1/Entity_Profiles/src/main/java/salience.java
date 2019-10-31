import java.sql.*;
import java.util.*;

public class salience {
    private static String sqlitePath = "\\db.sqlite";
    //每个类型下实体的salience,并且只筛选salience值大于0.70且本身中心度排在前100作为候选关系标签
    private static void count_salience(Connection db) throws Exception{
        //创建salience表

        try (Statement stmt = db.createStatement()) {
            stmt.execute("CREATE TABLE salience (entity1_id INTEGER NOT NULL, predicate_id  INTEGER NOT NULL, " +
                    "entity2_id INTEGER NOT NULL, entity1_type_id  INTEGER NOT NULL, entity2_type_id INTEGER NOT NULL,entity2_salience DOUBLE NOT NULL )");
        }
        Map<Integer, Double> vectorMap = new HashMap<Integer, Double>();
        Map<Integer, Double> node_cen_map = new HashMap<Integer, Double>();

        try(PreparedStatement obj_Stmt = db.prepareStatement("SELECT DISTINCT entity2_id,entity2_type_id FROM relation_triples;");
            PreparedStatement rel_Stmt = db.prepareStatement("SELECT entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id FROM relation_triples " +
                    "WHERE entity1_type_id=974 or entity1_type_id=151 or entity1_type_id=319 or entity1_type_id=1245 or entity1_type_id=603 or entity1_type_id=30 or entity1_type_id=61 or entity1_type_id=414 or entity1_type_id=628 or entity1_type_id=2330;");
            PreparedStatement cen_stmt = db.prepareStatement("SELECT ID, centrality FROM entity_centrality");
            PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO salience (entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id,entity2_salience) VALUES (?,?,?,?,?,?)")){
            //node--centrality
            ResultSet node_centrality = cen_stmt.executeQuery();
            while (node_centrality.next()) {
                Integer id = node_centrality.getInt(1);
                Double content = node_centrality.getDouble(2);
                node_cen_map.put(id,content);
            }
            //vectorMap:存储实体2类型中最大的中心度
            vectorMap.clear();
            ResultSet obj = obj_Stmt.executeQuery();
            while(obj.next()) {
                Integer en2_id = obj.getInt(1);
                Integer en2_type_id = obj.getInt(2);
                double center = node_cen_map.get(en2_id);
                if (!vectorMap.containsKey(en2_type_id)){
                    vectorMap.put(en2_type_id,center);
                }
                else{
                    if (center > vectorMap.get(en2_type_id)){
                        vectorMap.put(en2_type_id,center);
                    }
                }
            }
            ResultSet relation_1 = rel_Stmt.executeQuery();
            while(relation_1.next()) {
                Integer en1_id = relation_1.getInt(1);
                Integer pro_id = relation_1.getInt(2);
                Integer en2_id = relation_1.getInt(3);
                Integer en1_type_id = relation_1.getInt(4);
                Integer en2_type_id = relation_1.getInt(5);
                double center = node_cen_map.get(en2_id);
                //sal=实体2的中心度值/同类型下实体中最大的中心度值
                double sal_type = center / vectorMap.get(en2_type_id);
                //筛选>=0.70的实体2作为候选标签
                //System.out.println(sal_type);

                insert_Stmt.setInt(1,en1_id);
                insert_Stmt.setInt(2,pro_id);
                insert_Stmt.setInt(3,en2_id);
                insert_Stmt.setInt(4,en1_type_id);
                insert_Stmt.setInt(5,en2_type_id);
                insert_Stmt.setDouble(6,sal_type);
                insert_Stmt.execute();
            }
        }
        db.commit();
    }
    public static void main(String[] args) throws Exception{
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        count_salience(db);
    }

}

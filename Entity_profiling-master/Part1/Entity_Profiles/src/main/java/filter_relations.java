import com.sun.org.apache.regexp.internal.RE;

import java.sql.*;
import java.util.*;

public class filter_relations {
    private static String sqlitePath = "\\db.sqlite";
    private static void make_relation_support(Connection db) throws Exception{
        try (Statement stmt = db.createStatement()) {
            stmt.execute("CREATE TABLE filter_relation_new (entity1_id INTEGER NOT NULL, predicate_id  INTEGER NOT NULL, " +
                    "entity2_id INTEGER NOT NULL, entity1_type_id  INTEGER NOT NULL, entity2_type_id INTEGER NOT NULL,entity2_salience DOUBLE NOT NULL )");
        }
        try (Statement indexingStmt = db.createStatement()) {
            indexingStmt.execute("CREATE INDEX filter_re_new ON filter_relation_new(entity2_salience)");
        }
        db.commit();
        Map<Integer, Double> vectorMap = new HashMap<Integer, Double>();
        Map<Integer, Double> node_cen_map = new HashMap<Integer, Double>();
        HashSet<Double> node_cen_set = new HashSet<Double>();
        try(PreparedStatement type_Stmt = db.prepareStatement("SELECT DISTINCT entity1_type_id FROM salience;");
            PreparedStatement pre_Stmt = db.prepareStatement("SELECT DISTINCT predicate_id FROM salience where entity1_type_id=?;");
            PreparedStatement en2_Stmt = db.prepareStatement("select distinct entity2_id FROM salience where entity1_type_id=? and predicate_id=? ORDER BY entity2_salience DESC;");
            PreparedStatement choose_Stmt = db.prepareStatement("SELECT entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id,entity2_salience FROM salience where entity1_type_id=? and predicate_id=? and entity2_id=? ;");
            PreparedStatement insert_Stmt = db.prepareStatement("INSERT INTO filter_relation_new (entity1_id,predicate_id,entity2_id,entity1_type_id,entity2_type_id,entity2_salience) VALUES (?,?,?,?,?,?)")
        ) {

            ResultSet en1_type = type_Stmt.executeQuery();
            while (en1_type.next()) {
                Integer type_id = en1_type.getInt(1);
                pre_Stmt.setInt(1, type_id);
                ResultSet pre = pre_Stmt.executeQuery();
                while(pre.next()){
                    Integer pre_id = pre.getInt(1);
                    en2_Stmt.setInt(1,type_id);
                    en2_Stmt.setInt(2,pre_id);
                    ResultSet en2 = en2_Stmt.executeQuery();
                    int num = 0;
                    while(en2.next()){
                        num++;
                        if(num>10)
                            break;
                        Integer en2_id = en2.getInt(1);
                        choose_Stmt.setInt(1,type_id);
                        choose_Stmt.setInt(2,pre_id);
                        choose_Stmt.setInt(3,en2_id);
                        ResultSet choose = choose_Stmt.executeQuery();
                        while(choose.next()){
                            Integer entity1_id = choose.getInt(1);
                            Integer predicate_id = choose.getInt(2);
                            Integer entity2_id = choose.getInt(3);
                            Integer entity1_type_id = choose.getInt(4);
                            Integer entity2_type_id = choose.getInt(5);
                            Double entity2_salience = choose.getDouble(6);
                            insert_Stmt.setInt(1,entity1_id);
                            insert_Stmt.setInt(2,predicate_id);
                            insert_Stmt.setInt(3,entity2_id);
                            insert_Stmt.setInt(4,entity1_type_id);
                            insert_Stmt.setInt(5,entity2_type_id);
                            insert_Stmt.setDouble(6,entity2_salience);
                            insert_Stmt.execute();
                        }
                    }
                }
            }
        }
        db.commit();
    }
    public static void main(String[] args) throws Exception{
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        make_relation_support(db);
    }

}

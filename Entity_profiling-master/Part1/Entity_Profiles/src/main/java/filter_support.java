import java.sql.*;

/**<属性，属性值>过滤support值*/
public class filter_support {
    //E:\Entity_Profiles_v1\dbpedia_en.sqlite
    private static String sqlitePath = "\\db.sqlite";

    //过滤属性值为string类型的属性
    private static void filter_String_support(Connection db) throws Exception{
        try(Statement stmt = db.createStatement()){
            //创建filter_pro_string_sup表: 属性为字符串的支持度（初步筛选范围支持度所在区间[0.01,0.90]）
            stmt.execute("CREATE TABLE filter_pro_string_sup_new (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "type_id INTEGER NOT NULL,  " +
                    "property_id  INTEGER NOT NULL, " +
                    "property_value  INTEGER NOT NULL, " +
                    "support_property_value DOUBLE NOT NULL)");
        }
        try(
                PreparedStatement count_str_Stmt = db.prepareStatement("SELECT count(*) FROM property_string_support WHERE type_id=?");
                PreparedStatement pro_filter_string_Stmt = db.prepareStatement("SELECT type_id,property_id,property_value,support_property_value FROM property_string_support WHERE type_id=? and support_property_value >= 0.005 ORDER BY support_property_value DESC");
                PreparedStatement insert_stmt = db.prepareStatement("INSERT INTO filter_pro_string_sup_new (type_id,property_id,property_value,support_property_value) VALUES (?,?,?,?)")
          )
        {
            int []array = {974,151,319,1245,603,30,61,414,628,2330};
            for(int i = 0;i<array.length;i++){
                int total_str = 0;
                count_str_Stmt.setInt(1,array[i]);
                ResultSet ans = count_str_Stmt.executeQuery();
                if(ans.next()) {
                    total_str = ans.getInt(1);
                }
                int num_str = total_str / 4;
                int count = 0;
                pro_filter_string_Stmt.setInt(1,array[i]);
                ResultSet pro_filter_string = pro_filter_string_Stmt.executeQuery();
                while(pro_filter_string.next()){
                    if(count > num_str)
                        break;
                    int a =pro_filter_string.getInt(1);
                    insert_stmt.setInt(1,a);
                    insert_stmt.setInt(2,pro_filter_string.getInt(2));
                    insert_stmt.setInt(3,pro_filter_string.getInt(3));
                    insert_stmt.setDouble(4,pro_filter_string.getDouble(4));
                    insert_stmt.execute();
                    count++;
                }
            }
        }
        db.commit();
    }
    //过滤属性值为int_double类型的属性
    private static void filter_int_double_support(Connection db) throws Exception{
        try(Statement creat_stmt = db.createStatement()){
            creat_stmt.execute("CREATE TABLE filter_pro_numerical_sup_new (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "type_id INTEGER NOT NULL,  " +
                    "property_id  INTEGER NOT NULL, " +
                    "property_value_range  CHAR NOT NULL, " +
                    "pro_value_range_support DOUBLE NOT NULL)");
        }
        db.commit();
        try(PreparedStatement count_num_Stmt = db.prepareStatement("SELECT count(*) FROM property_numerical_support WHERE type_id=?");
            PreparedStatement pro_int_stmt = db.prepareStatement("SELECT type_id,property_id,property_value_range,pro_value_range_support FROM property_numerical_support WHERE type_id=? and pro_value_range_support >= 0.005 ORDER BY pro_value_range_support DESC");
        PreparedStatement insert = db.prepareStatement("INSERT INTO filter_pro_numerical_sup_new(type_id,property_id,property_value_range,pro_value_range_support) VALUES (?,?,?,?)")){

            int []array = {974,151,319,1245,603,30,61,414,628,2330};
            for(int i = 0;i<array.length;i++){
                int total_str = 0;
                count_num_Stmt.setInt(1,array[i]);
                ResultSet ans = count_num_Stmt.executeQuery();
                if(ans.next()) {
                    total_str = ans.getInt(1);
                }
                int num_str = total_str / 4;
                int count = 0;
                pro_int_stmt.setInt(1,array[i]);
                ResultSet pro_int = pro_int_stmt.executeQuery();
                while(pro_int.next()){
                    if(count > num_str)
                        break;
                    double  support = pro_int.getDouble(4);
                    insert.setInt(1, pro_int.getInt(1));
                    insert.setInt(2, pro_int.getInt(2));
                    insert.setString(3, pro_int.getString(3));
                    insert.setDouble(4, support);
                    insert.execute();
                    count++;
                }
            }
        }
        db.commit();
    }

    public static void main(String[] args)throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        filter_String_support(db);
        filter_int_double_support(db);
    }
}

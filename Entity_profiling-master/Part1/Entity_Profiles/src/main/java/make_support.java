import java.sql.*;

/**1.创建property_string_support表：保存属性值的类型为String的支持度（类型、属性、属性值、属性值的支持度）
 * 2.创建property_int_double_mid_support表：保存属性值类型为int/double的属性表（类型、属性、有该属性的实体数量、该属性的最小值、该属性值的最大值）
 * 3.创建property_int_double_support表：将int/double类型的属性值划分为值域中，求各个值域的support(初步设定划分为3个域)
 * */
public class make_support {
    private static String sqlitePath = "\\linkedmdb_support.sqlite";
    //保存属性值为string类型的support
    private static void String_support(Connection db) throws Exception{
        try(Statement stmt = db.createStatement()){
            //创建property_string_support表: 属性为字符串的支持度
            stmt.execute("CREATE TABLE property_string_support (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "type_id INTEGER NOT NULL,  " +
                    "property_id  INTEGER NOT NULL, " +
                    "property_value  INTEGER NOT NULL, " +
                    "support_property_value DOUBLE NOT NULL)");
        }
        try(
                PreparedStatement property_string_Stmt = db.prepareStatement("SELECT type_id,property_id,property_value,support_property_value " +
                        "FROM property_mid_support WHERE (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)=2 OR (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)=3;");
                PreparedStatement stmt = db.prepareStatement("INSERT INTO property_string_support (type_id,property_id,property_value,support_property_value) VALUES (?,?,?,?)"))
        {
            ResultSet property_string = property_string_Stmt.executeQuery();
            while(property_string.next()){
                stmt.setInt(1,property_string.getInt(1));
                stmt.setInt(2,property_string.getInt(2));
                stmt.setInt(3,property_string.getInt(3));
                stmt.setDouble(4,property_string.getDouble(4));
                stmt.execute();
            }

        }
        db.commit();

    }
    public static void main(String[] args)throws Exception {
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        String_support(db);
    }
}

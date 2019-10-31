package small_goal;
 /*连续值离散化：
  1.等宽划分
  2.局部密度划分*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;


public class Numerical_discretization {
    private static String sqlitePath = "\\drugbank.sqlite";
    //写入数据库的表中
    private static void write_table(Connection db,int type_id,int property_id, String pro_value_range, double property_value_sup)throws Exception {
        try(PreparedStatement pro_insert_stmt = db.prepareStatement("INSERT INTO property_numerical_support (type_id,property_id,property_value_range,pro_value_range_support) VALUES (?,?,?,?)")){
            pro_insert_stmt.setInt(1, type_id);
            pro_insert_stmt.setInt(2, property_id);
            pro_insert_stmt.setString(3, pro_value_range);
            pro_insert_stmt.setDouble(4, property_value_sup);
            pro_insert_stmt.execute();
        }
        db.commit();
    }
    private static void range(Connection db,int type_id,int entity_num, int property_id, int list_low_size,List<Double> list_low,Map<Double, Integer> value_map)throws Exception{
        int num_property_value, value;
        double is_value, pro_value_range_sup;
        String pro_value_area;
        for (int i = 0; i < list_low_size -1; i++) {
            value = 0;
            if (i == list_low_size - 2){
                pro_value_area = "[" + list_low.get(i) + "," + list_low.get(i+1)+ "]";
                for (Map.Entry<Double, Integer> entry : value_map.entrySet()) {
                    is_value = entry.getKey();
                    num_property_value = entry.getValue();
                    if ((is_value>=list_low.get(i))&&(is_value<=list_low.get(i+1)))
                        value += num_property_value;
                }
            }
            else{
                pro_value_area = "[" + list_low.get(i) + "," + list_low.get(i+1)+ ")";
                for (Map.Entry<Double, Integer> entry : value_map.entrySet()) {
                    is_value = entry.getKey();
                    num_property_value = entry.getValue();
                    if ((is_value >= list_low.get(i))&&(is_value < list_low.get(i+1)))
                        value += num_property_value;
                }
            }
            pro_value_range_sup = (value * 1.0) / entity_num;
            write_table(db,type_id,property_id,pro_value_area,pro_value_range_sup);
        }
    }
    //等宽划分
    private static void Equal_width_division(Connection db,int type_id,int entity_num, int property_id, int property_value_num, Map<Double, Integer> value_map,List<Double> list)throws Exception{
        //System.out.print(list);
        int area = 10, num_property_value;
        double value_min, value_max, is_value, space, pro_value_range_sup;
        double [] arr_range = new double[area+1];
        int [] arr_count = new int[area];
        String pro_value_area;
        //如果property_value_num<10,划分为property_value_num个值域
        if (property_value_num < area){
            for (Map.Entry<Double, Integer> entry : value_map.entrySet()) {
                is_value = entry.getKey();
                num_property_value = entry.getValue();
                pro_value_area = "[" + is_value + "," + is_value + "]";
                pro_value_range_sup = (num_property_value * 1.0) / entity_num;
                write_table(db,type_id,property_id,pro_value_area,pro_value_range_sup);
            }
        }//如果property_value_num>=10,划分为10个值域
        else if (property_value_num >= area) {
            //属性值的最小值和最大值
            value_min = Collections.min(list);
            value_max = Collections.max(list);
            //space:每个值域的长度
            space = (value_max - value_min) / area;
            //属性值域区间
            arr_range[0] = value_min;
            for (int i=1; i < area; i++){
                arr_range[i] = arr_range[i-1] + space;
            }
            arr_range[area] = value_max;
            //属性值域区间的个数
            for (int i = 0; i < area; i++)
                arr_count[i]=0;
            for (Map.Entry<Double, Integer> entry : value_map.entrySet()) {
                is_value = entry.getKey();
                num_property_value = entry.getValue();
                for (int j = 0; j < area; j++) {
                    if ((is_value >= arr_range[j]) && (is_value < arr_range[j + 1]))
                        arr_count[j] += num_property_value;
                }
                if (is_value == value_max){
                    arr_count[area-1] += num_property_value;
                }
            }
            //计算support值，并写入数据库表中
            for (int i = 0; i < area; i++){
                if (i == area-1)
                    pro_value_area = "[" + arr_range[i] + "," + arr_range[i+1]+ "]";
                else
                    pro_value_area = "[" + arr_range[i] + "," + arr_range[i+1]+ ")";
                pro_value_range_sup = (arr_count[i] * 1.0) / entity_num;
                write_table(db,type_id,property_id,pro_value_area,pro_value_range_sup);
            }
        }
    }
    //局部密度划分
    private static void Local_density_division(Connection db,int type_id,int entity_num, int property_id, int property_value_num,Map<Double, Integer> value_map,List<Double> list)throws Exception{
        List<Double> list_low = new ArrayList<Double>();//存储是谷底的属性取值
        List<Double> list_low_range = new ArrayList<Double>();
        int list_low_size, list_low_range_size, range, area = 10;
        double value_is;
        //属性取值排序（从小到大）
        Collections.sort(list);
        //寻找临界点
        list_low.clear();
        list_low_range.clear();
        list_low.add(list.get(0));
        for (int i = 1; i <  list.size() - 1; i++){
            value_is = list.get(i);
            int value_is_num = value_map.get(value_is);
            if ((value_map.get(list.get(i-1)) > value_is_num)&&(value_is_num <= value_map.get(list.get(i+1))))
                    list_low.add(value_is);
        }
        list_low.add(list.get(list.size() - 1));
        //筛选临界点
        list_low_size = list_low.size();
        if (list_low_size == 2)
            Equal_width_division(db,type_id, entity_num, property_id, property_value_num,value_map,list);
        else if ((2 < list_low_size) && (list_low_size <= 10)){
            range(db,type_id,entity_num, property_id, list_low_size, list_low, value_map);
        }
        else{
            //处理临界点个数较多的情况(划分成10份)
            range = list_low_size/area;
            for (int i = 0; i < list_low_size; i = i+range) {
                list_low_range.add(list_low.get(i));
            }
            list_low_range.add(list_low.get(list_low_size - 1));
            list_low_range_size = list_low_range.size();
            range(db,type_id,entity_num, property_id, list_low_range_size, list_low_range, value_map);
        }
    }

    //区间划分方法的判断 默认值：Density_Threshold_min=0.3，Density_Threshold_max=1
    private static void num_division_main(Connection db, double Density_Threshold_min, double Density_Threshold_max) throws Exception{
        //创建property_numerical_support表,属性为int/double的支持度
        try (Statement stmt = db.createStatement()) {
            stmt.execute("CREATE TABLE property_numerical_support (type_id INTEGER NOT NULL," +
                    "property_id INTEGER NOT NULL, property_value_range CHAR NOT NULL," +
                    "pro_value_range_support DOUBLE NOT NULL)");
        }
        db.commit();
        //变量定义
        Map<Double, Integer> value_map = new HashMap<Double, Integer>(); //<属性值，有此属性值的实体个数>
        List<Double> list = new ArrayList<Double>(); //<属性值>

        try (PreparedStatement property_numerical_Stmt = db.prepareStatement("SELECT DISTINCT type_id,entity_num, property_id,property_en_num,property_value_num FROM property_mid_support WHERE" +
                " (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=2 AND (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=4 " +
                "AND (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=63 AND (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=13 AND" +
                "(SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=6 AND (SELECT string_type_id FROM mapping WHERE mapping.id=property_mid_support.property_value)!=16;");
             //查找某类型，某属性的所有属性取值，及其有属性值的实体个数
             PreparedStatement property_value_Stmt = db.prepareStatement("SELECT DISTINCT property_value,num_property_value FROM property_mid_support WHERE type_id=? AND property_id=?;");
             //查找某个具体属性值的具体值
             PreparedStatement value_real_stmt = db.prepareStatement("SELECT content FROM mapping WHERE id=?;")){
            ResultSet property_numerical = property_numerical_Stmt.executeQuery();
            while(property_numerical.next()) {
                value_map.clear();
                list.clear();
                Integer type_id = property_numerical.getInt(1);
                //该类型下实体的个数
                Integer entity_num = property_numerical.getInt(2);
                Integer property_id = property_numerical.getInt(3);
                //统计有此属性的实体的个数，记为property_entity_num
                Integer property_entity_num = property_numerical.getInt(4);
                //统计此属性值有多少个不同的取值，记为property_value_num
                Integer property_value_num = property_numerical.getInt(5);

                property_value_Stmt.setInt(1, type_id);
                property_value_Stmt.setInt(2, property_id);
                ResultSet property_value = property_value_Stmt.executeQuery();
                while (property_value.next()) {
                    Integer property_value_id = property_value.getInt(1);
                    Integer num_property_value = property_value.getInt(2);
                    //某个具体属性值的具体值
                    value_real_stmt.setInt(1, property_value_id);
                    ResultSet value_real = value_real_stmt.executeQuery();
                    double pro_value = value_real.getDouble(1);
                    //System.out.println(pro_value);
                    value_map.put(pro_value, num_property_value);
                    list.add(pro_value);
                }
                int length = list.size();
                if(length>=5){
                    Collections.sort(list);
                    //下4分位数
                    double pos_1 = (length+1)/4.0;
                    double q1 = list.get((int)Math.floor(pos_1)-1);
                    //上4分位数
                    double pos_3 = 3.0*(length+1)/4.0;
                    double q3 = list.get((int)Math.floor(pos_3)-1);
                    //上限
                    double iqr = q3-q1;
                    double max_line = q3+1.5*iqr;
                    //下限
                    double min_line = q1-1.5*iqr;
                    for(int i = 0;i<list.size();i++){
                        if(list.get(i)<min_line ||list.get(i)>max_line)
                            list.remove(i);
                    }
                }
                property_value_num = list.size();

                //属性值离散的程度：P=property_value_num/property_entity_num<=1
                double P =  (property_value_num*1.0)/property_entity_num;
                //1.Density_Threshold_min<P<=Density_Threshold_max:等宽划分；2.P<=Density_Threshold_min:局部密度划分
                if ((Density_Threshold_min < P)&&(P <= Density_Threshold_max))
                    Equal_width_division(db,type_id, entity_num, property_id, property_value_num,value_map,list);
                else if (P <= Density_Threshold_min)
                    Local_density_division(db,type_id, entity_num, property_id, property_value_num,value_map,list);
            }
        }
        db.commit();
    }
    public static void main(String[] args)throws Exception{
        Connection db = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
        db.setAutoCommit(false);
        num_division_main(db,0.5,1.0);
    }
}
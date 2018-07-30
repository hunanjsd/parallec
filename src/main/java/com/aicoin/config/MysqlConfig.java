package com.aicoin.config;

/**
 * @Author: Simo
 * @Description:
 * @Date: Create in 上午10:44 2018/7/26
 * @Modified By:
 */
public class MysqlConfig {
    /** mysql url ,database 包括在url中 */
    public static String mysqlUrl =  "jdbc:mysql://127.0.0.1:3306/kline_test?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
//    public static String mysqlUrl =  "jdbc:mysql://192.168.1.184:3306/kline_test?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&useSSL=false";
    /** mysql 驱动 */
    public static String mysqlDrive = "com.mysql.jdbc.Driver";
    /** mysql用户名 */
    public static String userName = "root";
    /** mysql用户名下对应密码 */
    public static String userPasswd = "Hunanjsd3";
}

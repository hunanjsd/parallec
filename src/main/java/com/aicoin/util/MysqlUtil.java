package com.aicoin.util;

import com.aicoin.config.MysqlConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Simo
 * @Description:
 * @Date: Create in 上午10:53 2018/7/26
 * @Modified By:
 */
public class MysqlUtil {
    private static final Logger logger = LoggerFactory.getLogger(MysqlUtil.class);

    public static final MysqlUtil instance = new MysqlUtil();

    private  Connection conn;

    public MysqlUtil(){
        init();
    }

    /**
     * 初始化mysql连接
     */
    public synchronized void init(){
        String url = MysqlConfig.mysqlUrl;
        String drive = MysqlConfig.mysqlDrive;
        String userName = MysqlConfig.userName;
        String passwd = MysqlConfig.userPasswd;

       try {
           Class.forName(drive);
           conn = DriverManager.getConnection(url,userName,passwd);
       }catch (SQLException e){
           logger.error("init mysql connection fail!");
           logger.error(e.getMessage(),e);
       }catch (ClassNotFoundException e){
           logger.error("The com.mysql.jdbc.Drive is not found! please check again ensure that this jar have been import!");
           logger.error(e.getMessage(),e);
       }
    }

    /**
     * 根据column列名从sql查询结果中提取value
     * @param sql mysql查询的sql语句
     * @param columnName 需要提取结果的列名
     * @return
     */
    public List<String > query(String  sql, String columnName){
        List<String > results = new ArrayList<>();
        if(StringUtils.isNotBlank(sql)){
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()){
                    String result = rs.getNString(columnName);
                    if(result!=null){
                        results.add(result);
                    }
                }
                rs.close();
                stmt.close();
                return results;
            }catch (SQLException e){
                logger.error("when create a mysql Statement occur a Exception!");
                logger.error(e.getMessage(),e);
            }
        }
      return null;
    }

    /**
     * 连接异常时重新初始化
     */
    public synchronized void reInit(){
        shutdown();
        init();
    }

    /**
     * 关闭对象时释放连接
     */
    public void shutdown(){
        try {
            conn.close();
        }catch (SQLException e){
            logger.error("when shutdown the mysql connection occur a exception!");
            logger.error(e.getMessage(),e);
        }
    }

    public static MysqlUtil getInstance() {
        return instance;
    }

    private void setConn(Connection conn) {
        this.conn = conn;
    }
}

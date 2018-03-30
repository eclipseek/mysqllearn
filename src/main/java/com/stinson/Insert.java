package com.stinson;/**
 * created by Intellij IDEA
 * User: yiqiang-zhang
 * Date: 2018-03-29
 * Time: 21:41
 */

import java.sql.*;

/**
 *
 * <pre>
 * 程序目的：向表中插入 2 w + 1 条记录，然后使用 mysql 自带的压力测试工具 mysqlslap 进行压力测试：
 * mysqlslap
 *  --defaults-file=/etc/my.cnf
 *  --concurrency=100
 *  --iterations=1
 *  --create-schema='test1'
 *  --query='select * from test1.tb1'
 *  engine=innodb
 *  --number-of-queries=2000
 *  -uroot -proot
 *  -verbose
 * 在程序没有优化之前，记录执行时间。然后对程序进行优化，优化思路参考：优化案例.MD
 *
 * 优化之后重启 mysql 服务：
 * # ./mysqladmin shutdown
 * # ./mysqld --defaults-file=/etc/my.optimize.cnf --datadir=/var/lib/mysql  --socket=/tmp/mysql.sock --user=mysql
 *
 *</pre>
 *
 * Author: zhangyq<p>
 * Date: 21:41 2018-03-29 <p>
 * Description: <p>
 *
 * 执行程序前创建库：<p>
 * create database if not exists test1; <p></p>
 *
 * 再创建表：<p>
 * CREATE TABLE IF NOT EXISTS tb1 ( <p></p>
 *      stuid INT NOT NULL PRIMARY KEY, <p>
 *      stuname VARCHAR (20) NOT NULL,  <p>
 *      stusex CHAR (1) NOT NULL,       <p>
 *      cardid VARCHAR (20) NOT NULL,   <p>
 *      birthday datetime,              <p>
 *      entertime datetime,             <p>
 *      address VARCHAR (100) DEFAULT NULL  <p>
 * )    <p>
 *
 * 参考：
 * https://www.2cto.com/database/201711/695306.html
 */
public class Insert {

    // JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://192.168.74.145:3306/test1";

    // 数据库的用户名与密码，需要根据自己的设置
    static final String USER = "root";
    static final String PASS = "root";

    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            // 注册 JDBC 驱动
            Class.forName("com.mysql.jdbc.Driver");

            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // 先删除所有数据
            String delete = "delete from tb1";
            stmt = conn.prepareStatement(delete);
            stmt.execute();

            System.out.println("旧数据已删除!");

            System.out.println("开始插入 20001 条新数据...");
            // 再插入 2 万条记录。
            String insert = "insert into tb1  values(?, 'zhangsan', '1','21276387261874682','1999-10-10','2017-10-24','beijingchangpingqu')";
            stmt = conn.prepareStatement(insert);
            boolean batch = true;
            for (int i = 0; i < 20000; i ++) {
                stmt.setInt(1, i);
                stmt.addBatch();
                batch = true;
                if (i % 100 == 0) {
                    stmt.executeBatch();
                    batch = false;
                }
            }

            // 最后一次提交
            if (batch) {
                stmt.executeBatch();
            }

            // 再插入一条
            insert = "insert into test1.tb1 values(20001,'admin','0','12322112123332','1999-1-1','2019-9-1','ppppppppppp')";
            conn.prepareStatement(insert).execute();

            // 验证插入了 20001 条
            String query = "select count(1) as cnt from tb1";
            ResultSet rs = conn.prepareStatement(query).executeQuery();
            rs.next();
            int cnt = rs.getInt("cnt");
            if (cnt != 20001) {
                throw new Exception("插入数据量不等于 200001");
            } else {
                System.out.println("20001 条记录插入成功！");
            }

            stmt.close();
            conn.close();
        }catch(SQLException se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }finally{
            // 关闭资源
            try{
                if(stmt!=null) stmt.close();
            }catch(SQLException se2){
            }// 什么都不做
            try{
                if(conn!=null) conn.close();
            }catch(SQLException se){
                se.printStackTrace();
            }
        }
        System.out.println("Goodbye!");
    }

}

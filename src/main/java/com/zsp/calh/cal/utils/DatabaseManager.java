package com.zsp.calh.cal.utils;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    private Connection connection;
    private String dbUrl;

    /**
     * 构造函数，初始化数据库连接
     * @param dbPath SQLite数据库文件路径
     */
    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    /**
     * 连接到SQLite数据库
     */
    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("已连接到SQLite数据库");
        }
    }

    /**
     * 关闭数据库连接
     */
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("已关闭SQLite数据库连接");
        }
    }

    /**
     * 根据Excel数据创建表
     * @param tableName 表名
     * @param columnCount 列数
     */
    public void createTable(String tableName, int columnCount) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE IF NOT EXISTS ")
                .append(tableName)
                .append(" (id INTEGER PRIMARY KEY AUTOINCREMENT");

        for (int i = 1; i <= columnCount; i++) {
            sqlBuilder.append(", column").append(i).append(" TEXT");
        }

        sqlBuilder.append(")");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlBuilder.toString());
            System.out.println("已创建表: " + tableName);
        }
    }

    /**
     * 保存Excel数据到SQLite数据库
     * @param tableName 表名
     * @param data Excel数据
     * @param skipHeader 是否跳过表头
     */
    public void saveExcelDataToDB(String tableName, List<List<String>> data, boolean skipHeader) throws SQLException {
        if (data == null || data.isEmpty()) {
            System.out.println("没有数据可保存");
            return;
        }

        int startIndex = skipHeader ? 1 : 0;
        if (startIndex >= data.size()) {
            System.out.println("没有数据可保存");
            return;
        }

        // 确定列数
        int columnCount = data.get(0).size();

        // 创建表
        createTable(tableName, columnCount);

        // 构建插入SQL
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName).append(" (column1");
        for (int i = 2; i <= columnCount; i++) {
            sqlBuilder.append(", column").append(i);
        }
        sqlBuilder.append(") VALUES (?");
        for (int i = 2; i <= columnCount; i++) {
            sqlBuilder.append(", ?");
        }
        sqlBuilder.append(")");

        String sql = sqlBuilder.toString();

        // 批量插入数据
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // 关闭自动提交以提高性能
            connection.setAutoCommit(false);

            for (int i = startIndex; i < data.size(); i++) {
                List<String> row = data.get(i);
                for (int j = 0; j < Math.min(row.size(), columnCount); j++) {
                    pstmt.setString(j + 1, row.get(j) != null ? row.get(j) : "");
                }
                pstmt.addBatch();
            }

            // 执行批处理
            int[] result = pstmt.executeBatch();
            connection.commit();

            System.out.println("成功保存 " + result.length + " 行数据到表 " + tableName);
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * 查询数据库表中的数据
     * @param tableName 表名
     * @param limit 限制返回的行数
     */
    public void queryTableData(String tableName, int limit) throws SQLException {
        String sql = "SELECT * FROM " + tableName;
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 打印表头
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(metaData.getColumnName(i) + "\t");
            }
            System.out.println();

            // 打印数据
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
        }
    }
}
// D:/ZspProject/Cal/src/main/java/com/zsp/calh/cal/utils/DatabaseManager.java

package com.zsp.calh.cal.utils;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    // 单例实例
    private static DatabaseManager instance;
    private Connection connection;
    private final String dbUrl;

    // 私有构造函数，防止外部实例化
    private DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        // 在构造时就尝试连接
        try {
            connect();
        } catch (SQLException e) {
            System.err.println("数据库初始化连接失败: " + e.getMessage());
            // 抛出运行时异常，因为如果数据库无法连接，程序可能无法正常运行
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化并获取单例实例。此方法在应用启动时应仅调用一次。
     * @param dbPath 数据库文件路径
     * @return DatabaseManager的单例实例
     */
    public static synchronized DatabaseManager initializeInstance(String dbPath) {
        if (instance == null) {
            instance = new DatabaseManager(dbPath);
        }
        return instance;
    }

    /**
     * 获取已初始化的单例实例。
     * @return DatabaseManager的单例实例
     * @throws IllegalStateException 如果实例尚未通过 initializeInstance 初始化
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            // 强调应该先调用带路径的初始化方法
            throw new IllegalStateException("DatabaseManager尚未初始化。请首先调用 initializeInstance(String dbPath)。");
        }
        return instance;
    }

    /**
     * 连接到SQLite数据库。如果连接已存在或已打开，则不执行任何操作。
     */
    private void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("已连接到SQLite数据库: " + dbUrl);
        }
    }

    /**
     * 关闭数据库连接。此方法应在应用程序关闭时调用。
     */
    public void disconnect() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("已关闭SQLite数据库连接。");
                }
            } catch (SQLException e) {
                System.err.println("关闭数据库连接时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 获取数据库连接对象。如果连接已关闭，会尝试重新连接。
     * @return 数据库连接对象
     */
    public Connection getConnection() throws SQLException {
        // 增加健壮性，如果连接因故关闭，尝试重连
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    // ... 其他方法 (createTable, saveExcelDataToDB, queryTableData) 保持不变 ...
    // 但需要确保它们都使用 this.connection 或 getConnection()



/**
     * 根据Excel数据创建表
     *
     * @param tableName   表名
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
     *
     * @param tableName  表名
     * @param data       Excel数据
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
     *
     * @param tableName 表名
     * @param limit     限制返回的行数
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

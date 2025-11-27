package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoadController {

    /**
     * 从Excel文件读取数据并转换为TemperatureData对象列表
     * 按照指定格式：从第3行开始读取，每行作为一个TemperatureData对象
     * @param filePath Excel文件路径
     * @return TemperatureData对象列表
*/
    public List<TemperatureData> readExcelToTemperatureData(String filePath) {
        List<TemperatureData> temperatureDataList = new ArrayList<>();
        
        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 从第3行开始读取数据（索引为2，因为从0开始计数）
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                TemperatureData data = new TemperatureData();
                
                // 线别（第3列，索引为2）
                data.setLineNumber(getCellValueAsString(row, 2));
                
                // 设备名称（第4列，索引为3）
                data.setDeviceName(getCellValueAsString(row, 3));
                
                // 日期（第5列，索引为4）
                String dateStr = getCellValueAsString(row, 4);
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        // 解析格式为"Fri Oct 10 00:00:00 CST"的日期字符串
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz", java.util.Locale.ENGLISH);
                        java.util.Date date = sdf.parse(dateStr);
                        java.time.LocalDate localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        data.setDate(java.time.MonthDay.of(localDate.getMonth(), localDate.getDayOfMonth()));
                    } catch (Exception e) {
                        // 如果解析失败，尝试其他格式或设置为null
                        System.err.println("日期解析失败: " + dateStr + " 在第" + (i + 1) + "行");
                        data.setDate(null);
                    }
                } else {
                    data.setDate(null);
                }
                
                // 车上测量温度（第6-13列，索引为5-12）
                data.setCarTempLeft3(parseDoubleFromCell(row, 5));
                data.setCarTempLeft4(parseDoubleFromCell(row, 6));
                data.setCarTempLeft5(parseDoubleFromCell(row, 7));
                data.setCarTempLeft6(parseDoubleFromCell(row, 8));
                data.setCarTempRight3(parseDoubleFromCell(row, 9));
                data.setCarTempRight4(parseDoubleFromCell(row, 10));
                data.setCarTempRight5(parseDoubleFromCell(row, 11));
                data.setCarTempRight6(parseDoubleFromCell(row, 12));
                
                // 地面探1温度（第14-21列，索引为13-20）
                data.setGroundTemp1Left3(parseDoubleFromCell(row, 13));
                data.setGroundTemp1Left4(parseDoubleFromCell(row, 14));
                data.setGroundTemp1Left5(parseDoubleFromCell(row, 15));
                data.setGroundTemp1Left6(parseDoubleFromCell(row, 16));
                data.setGroundTemp1Right3(parseDoubleFromCell(row, 17));
                data.setGroundTemp1Right4(parseDoubleFromCell(row, 18));
                data.setGroundTemp1Right5(parseDoubleFromCell(row, 19));
                data.setGroundTemp1Right6(parseDoubleFromCell(row, 20));
                
                // 地面探2（内探）温度（第22-29列，索引为21-28）
                data.setGroundTemp2Left3(parseDoubleFromCell(row, 21));
                data.setGroundTemp2Left4(parseDoubleFromCell(row, 22));
                data.setGroundTemp2Left5(parseDoubleFromCell(row, 23));
                data.setGroundTemp2Left6(parseDoubleFromCell(row, 24));
                data.setGroundTemp2Right3(parseDoubleFromCell(row, 25));
                data.setGroundTemp2Right4(parseDoubleFromCell(row, 26));
                data.setGroundTemp2Right5(parseDoubleFromCell(row, 27));
                data.setGroundTemp2Right6(parseDoubleFromCell(row, 28));
                
                // 地面探3温度（第30-37列，索引为29-36）
                data.setGroundTemp3Left3(parseDoubleFromCell(row, 29));
                data.setGroundTemp3Left4(parseDoubleFromCell(row, 30));
                data.setGroundTemp3Left5(parseDoubleFromCell(row, 31));
                data.setGroundTemp3Left6(parseDoubleFromCell(row, 32));
                data.setGroundTemp3Right3(parseDoubleFromCell(row, 33));
                data.setGroundTemp3Right4(parseDoubleFromCell(row, 34));
                data.setGroundTemp3Right5(parseDoubleFromCell(row, 35));
                data.setGroundTemp3Right6(parseDoubleFromCell(row, 36));
                
                // 地面探4（外探）温度（第38-45列，索引为37-44）
                data.setGroundTemp4Left3(parseDoubleFromCell(row, 37));
                data.setGroundTemp4Left4(parseDoubleFromCell(row, 38));
                data.setGroundTemp4Left5(parseDoubleFromCell(row, 39));
                data.setGroundTemp4Left6(parseDoubleFromCell(row, 40));
                data.setGroundTemp4Right3(parseDoubleFromCell(row, 41));
                data.setGroundTemp4Right4(parseDoubleFromCell(row, 42));
                data.setGroundTemp4Right5(parseDoubleFromCell(row, 43));
                data.setGroundTemp4Right6(parseDoubleFromCell(row, 44));
                
                // 线性值（第46-53列，索引为45-52）
                data.setLinearValueTemp1left(parseDoubleFromCell(row, 45));
                data.setLinearValueTemp1right(parseDoubleFromCell(row, 46));
                data.setLinearValueTemp2left(parseDoubleFromCell(row, 47));
                data.setLinearValueTemp2right(parseDoubleFromCell(row, 48));
                data.setLinearValueTemp3left(parseDoubleFromCell(row, 49));
                data.setLinearValueTemp3right(parseDoubleFromCell(row, 50));
                data.setLinearValueTemp4left(parseDoubleFromCell(row, 51));
                data.setLinearValueTemp4right(parseDoubleFromCell(row, 52));
                
                // 非线性值（第54-61列，索引为53-60）
                data.setNonlinearValueTemp1left(parseDoubleFromCell(row, 53));
                data.setNonlinearValueTemp1right(parseDoubleFromCell(row, 54));
                data.setNonlinearValueTemp2left(parseDoubleFromCell(row, 55));
                data.setNonlinearValueTemp2right(parseDoubleFromCell(row, 56));
                data.setNonlinearValueTemp3left(parseDoubleFromCell(row, 57));
                data.setNonlinearValueTemp3right(parseDoubleFromCell(row, 58));
                data.setNonlinearValueTemp4left(parseDoubleFromCell(row, 59));
                data.setNonlinearValueTemp4right(parseDoubleFromCell(row, 60));
                
                // 板温（第62-65列，索引为61-64）
                data.setPlateTempInnerLeft(parseDoubleFromCell(row, 61));
                data.setPlateTempInnerRight(parseDoubleFromCell(row, 62));
                data.setPlateTempOuterLeft(parseDoubleFromCell(row, 63));
                data.setPlateTempOuterRight(parseDoubleFromCell(row, 64));
                
                temperatureDataList.add(data);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取Excel文件出错: " + e.getMessage());
        }
        
        return temperatureDataList;
    }

    /**
     * 将TemperatureData对象列表保存到SQLite数据库。
     * 使用全局的DatabaseManager实例进行操作。
     * @param temperatureDataList TemperatureData对象列表
     * @param tableName 表名
     */
    /**
     * 将TemperatureData对象列表保存到SQLite数据库。
     * 返回实际成功插入的数据条数（已自动忽略重复）。
     */
    public int saveTemperatureDataToSQLite(List<TemperatureData> temperatureDataList, String tableName) {
        if (temperatureDataList == null || temperatureDataList.isEmpty()) {
            System.out.println("没有温度数据可保存到数据库");
            return 0;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();

        try {
            // 1. 创建表（如果不存在），现在包含了唯一性约束
            createTableIfNotExists(dbManager, tableName);

            // 2. 批量插入数据，并返回成功插入的行数
            return batchInsertData(dbManager, tableName, temperatureDataList);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("保存温度数据到SQLite数据库出错: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 创建存储温度数据的表（如果它还不存在）。
     * 已添加联合唯一约束：(line_number, device_name, date)
     */
    private void createTableIfNotExists(DatabaseManager dbManager, String tableName) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "line_number TEXT, device_name TEXT, date TEXT, "
                + "car_temp_left3 REAL, car_temp_left4 REAL, car_temp_left5 REAL, car_temp_left6 REAL, "
                + "car_temp_right3 REAL, car_temp_right4 REAL, car_temp_right5 REAL, car_temp_right6 REAL, "
                + "ground_temp1_left3 REAL, ground_temp1_left4 REAL, ground_temp1_left5 REAL, ground_temp1_left6 REAL, "
                + "ground_temp1_right3 REAL, ground_temp1_right4 REAL, ground_temp1_right5 REAL, ground_temp1_right6 REAL, "
                + "ground_temp2_left3 REAL, ground_temp2_left4 REAL, ground_temp2_left5 REAL, ground_temp2_left6 REAL, "
                + "ground_temp2_right3 REAL, ground_temp2_right4 REAL, ground_temp2_right5 REAL, ground_temp2_right6 REAL, "
                + "ground_temp3_left3 REAL, ground_temp3_left4 REAL, ground_temp3_left5 REAL, ground_temp3_left6 REAL, "
                + "ground_temp3_right3 REAL, ground_temp3_right4 REAL, ground_temp3_right5 REAL, ground_temp3_right6 REAL, "
                + "ground_temp4_left3 REAL, ground_temp4_left4 REAL, ground_temp4_left5 REAL, ground_temp4_left6 REAL, "
                + "ground_temp4_right3 REAL, ground_temp4_right4 REAL, ground_temp4_right5 REAL, ground_temp4_right6 REAL, "
                + "linear_value_temp1left REAL, linear_value_temp1right REAL, linear_value_temp2left REAL, linear_value_temp2right REAL, "
                + "linear_value_temp3left REAL, linear_value_temp3right REAL, linear_value_temp4left REAL, linear_value_temp4right REAL, "
                + "nonlinear_value_temp1left REAL, nonlinear_value_temp1right REAL, nonlinear_value_temp2left REAL, nonlinear_value_temp2right REAL, "
                + "nonlinear_value_temp3left REAL, nonlinear_value_temp3right REAL, nonlinear_value_temp4left REAL, nonlinear_value_temp4right REAL, "
                + "plate_temp_inner_left REAL, plate_temp_inner_right REAL, plate_temp_outer_left REAL, plate_temp_outer_right REAL, "
                + "UNIQUE(line_number, date, device_name)" // 【关键修改】添加联合唯一约束，防止重复
                + ")";

        try (Statement stmt = dbManager.getConnection().createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("表 '" + tableName + "' 已准备就绪。");
        }
    }

    /**
     * 使用事务和批处理高效地插入数据。
     * 使用 INSERT OR IGNORE 自动忽略重复数据。
     */
    private int batchInsertData(DatabaseManager dbManager, String tableName, List<TemperatureData> dataList) throws SQLException {
        // 【关键修改】使用 INSERT OR IGNORE INTO
        String insertSQL = "INSERT OR IGNORE INTO " + tableName + " (line_number, device_name, date, car_temp_left3, car_temp_left4, car_temp_left5, car_temp_left6, "
                + "car_temp_right3, car_temp_right4, car_temp_right5, car_temp_right6, "
                + "ground_temp1_left3, ground_temp1_left4, ground_temp1_left5, ground_temp1_left6, "
                + "ground_temp1_right3, ground_temp1_right4, ground_temp1_right5, ground_temp1_right6, "
                + "ground_temp2_left3, ground_temp2_left4, ground_temp2_left5, ground_temp2_left6, "
                + "ground_temp2_right3, ground_temp2_right4, ground_temp2_right5, ground_temp2_right6, "
                + "ground_temp3_left3, ground_temp3_left4, ground_temp3_left5, ground_temp3_left6, "
                + "ground_temp3_right3, ground_temp3_right4, ground_temp3_right5, ground_temp3_right6, "
                + "ground_temp4_left3, ground_temp4_left4, ground_temp4_left5, ground_temp4_left6, "
                + "ground_temp4_right3, ground_temp4_right4, ground_temp4_right5, ground_temp4_right6, "
                + "linear_value_temp1left, linear_value_temp1right, linear_value_temp2left, linear_value_temp2right, "
                + "linear_value_temp3left, linear_value_temp3right, linear_value_temp4left, linear_value_temp4right, "
                + "nonlinear_value_temp1left, nonlinear_value_temp1right, nonlinear_value_temp2left, nonlinear_value_temp2right, "
                + "nonlinear_value_temp3left, nonlinear_value_temp3right, nonlinear_value_temp4left, nonlinear_value_temp4right, "
                + "plate_temp_inner_left, plate_temp_inner_right, plate_temp_outer_left, plate_temp_outer_right) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = dbManager.getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        int successCount = 0;

        try {
            // 【关键修改】在操作前先开启事务，修复 auto-commit 错误
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                for (TemperatureData data : dataList) {
                    int paramIndex = 1;
                    pstmt.setString(paramIndex++, data.getLineNumber());
                    pstmt.setString(paramIndex++, data.getDeviceName());

                    // 日期格式化为字符串，确保唯一性判断基于“日”
                    String dateStr = "";
                    if (data.getDate() != null) {
                        dateStr = data.getDate().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd"));
                    }
                    pstmt.setString(paramIndex++, dateStr);

                    // --- 设置所有其他 double 参数 (60个) ---
                    // 车上
                    pstmt.setDouble(paramIndex++, data.getCarTempLeft3()); pstmt.setDouble(paramIndex++, data.getCarTempLeft4());
                    pstmt.setDouble(paramIndex++, data.getCarTempLeft5()); pstmt.setDouble(paramIndex++, data.getCarTempLeft6());
                    pstmt.setDouble(paramIndex++, data.getCarTempRight3()); pstmt.setDouble(paramIndex++, data.getCarTempRight4());
                    pstmt.setDouble(paramIndex++, data.getCarTempRight5()); pstmt.setDouble(paramIndex++, data.getCarTempRight6());

                    // 地面1
                    pstmt.setDouble(paramIndex++, data.getGroundTemp1Left3()); pstmt.setDouble(paramIndex++, data.getGroundTemp1Left4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp1Left5()); pstmt.setDouble(paramIndex++, data.getGroundTemp1Left6());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp1Right3()); pstmt.setDouble(paramIndex++, data.getGroundTemp1Right4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp1Right5()); pstmt.setDouble(paramIndex++, data.getGroundTemp1Right6());

                    // 地面2
                    pstmt.setDouble(paramIndex++, data.getGroundTemp2Left3()); pstmt.setDouble(paramIndex++, data.getGroundTemp2Left4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp2Left5()); pstmt.setDouble(paramIndex++, data.getGroundTemp2Left6());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp2Right3()); pstmt.setDouble(paramIndex++, data.getGroundTemp2Right4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp2Right5()); pstmt.setDouble(paramIndex++, data.getGroundTemp2Right6());

                    // 地面3
                    pstmt.setDouble(paramIndex++, data.getGroundTemp3Left3()); pstmt.setDouble(paramIndex++, data.getGroundTemp3Left4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp3Left5()); pstmt.setDouble(paramIndex++, data.getGroundTemp3Left6());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp3Right3()); pstmt.setDouble(paramIndex++, data.getGroundTemp3Right4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp3Right5()); pstmt.setDouble(paramIndex++, data.getGroundTemp3Right6());

                    // 地面4
                    pstmt.setDouble(paramIndex++, data.getGroundTemp4Left3()); pstmt.setDouble(paramIndex++, data.getGroundTemp4Left4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp4Left5()); pstmt.setDouble(paramIndex++, data.getGroundTemp4Left6());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp4Right3()); pstmt.setDouble(paramIndex++, data.getGroundTemp4Right4());
                    pstmt.setDouble(paramIndex++, data.getGroundTemp4Right5()); pstmt.setDouble(paramIndex++, data.getGroundTemp4Right6());

                    // 线性
                    pstmt.setDouble(paramIndex++, data.getLinearValueTemp1left()); pstmt.setDouble(paramIndex++, data.getLinearValueTemp1right());
                    pstmt.setDouble(paramIndex++, data.getLinearValueTemp2left()); pstmt.setDouble(paramIndex++, data.getLinearValueTemp2right());
                    pstmt.setDouble(paramIndex++, data.getLinearValueTemp3left()); pstmt.setDouble(paramIndex++, data.getLinearValueTemp3right());
                    pstmt.setDouble(paramIndex++, data.getLinearValueTemp4left()); pstmt.setDouble(paramIndex++, data.getLinearValueTemp4right());

                    // 非线性
                    pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp1left()); pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp1right());
                    pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp2left()); pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp2right());
                    pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp3left()); pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp3right());
                    pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp4left()); pstmt.setDouble(paramIndex++, data.getNonlinearValueTemp4right());

                    // 板温
                    pstmt.setDouble(paramIndex++, data.getPlateTempInnerLeft()); pstmt.setDouble(paramIndex++, data.getPlateTempInnerRight());
                    pstmt.setDouble(paramIndex++, data.getPlateTempOuterLeft()); pstmt.setDouble(paramIndex++, data.getPlateTempOuterRight());

                    pstmt.addBatch();
                }

                int[] result = pstmt.executeBatch();
                conn.commit(); // 提交事务

                // 【关键修改】统计成功插入的行数（忽略的行返回0，成功的行返回1）
                for (int i : result) {
                    if (i > 0) successCount++;
                }

                System.out.println("Excel处理完毕：读取 " + dataList.size() + " 条，实际入库 " + successCount + " 条，忽略重复 " + (dataList.size() - successCount) + " 条。");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return successCount;
    }
    /**
     * 获取单元格的值并转换为字符串
     * @param row 行对象
     * @param cellIndex 单元格索引
     * @return 单元格的值作为字符串
     */
    private String getCellValueAsString(Row row, int cellIndex) {
        if (row == null || cellIndex < 0 || cellIndex >= row.getLastCellNum()) {
            return "";
        }
        
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }
    
    /**
     * 从单元格中解析double值
     * @param row 行对象
     * @param cellIndex 单元格索引
     * @return 解析后的double值，如果解析失败则返回0.0
     */
    private double parseDoubleFromCell(Row row, int cellIndex) {
        String valueStr = getCellValueAsString(row, cellIndex);
        if (valueStr.isEmpty()) {
            return 0.0;
        }
        
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
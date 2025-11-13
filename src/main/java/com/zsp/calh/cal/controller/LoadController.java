package com.zsp.calh.cal.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

// 导入我们刚创建的DatabaseManager类
import com.zsp.calh.cal.utils.DatabaseManager;
// 导入TemperatureData类
import com.zsp.calh.cal.model.TemperatureData;

public class LoadController {
    
    /**
     * 从Excel文件中按行读取数据
     * @param filePath Excel文件路径
     * @return 包含所有行数据的列表，每行数据是一个字符串列表
     */
    public List<List<String>> readExcelFile(String filePath) {
        List<List<String>> data = new ArrayList<>();
        
        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {
            
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            
            // 迭代行
            Iterator<Row> rowIterator = sheet.iterator();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                List<String> rowData = new ArrayList<>();
                
                // 迭代单元格
                Iterator<Cell> cellIterator = row.cellIterator();
                
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    
                    // 根据单元格类型获取值
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue().toString());
                            } else {
                                // 处理数字，避免科学计数法
                                double value = cell.getNumericCellValue();
                                if (value == Math.floor(value)) {
                                    // 如果是整数，转换为长整型再转字符串
                                    rowData.add(String.valueOf((long) value));
                                } else {
                                    rowData.add(String.valueOf(value));
                                }
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(String.valueOf(cell.getBooleanCellValue()));
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add(cell.toString());
                    }
                }
                
                data.add(rowData);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取Excel文件出错: " + e.getMessage());
        }
        
        return data;
    }

    /**
     * 将Excel数据保存到SQLite数据库
     * @param excelFilePath Excel文件路径
     * @param dbFilePath SQLite数据库文件路径
     * @param tableName 表名
     * @param skipHeader 是否跳过表头
     */
    public void saveExcelToSQLite(String excelFilePath, String dbFilePath, String tableName, boolean skipHeader) {
        try {
            // 读取Excel数据
            List<List<String>> excelData = readExcelFile(excelFilePath);
            
            if (excelData.isEmpty()) {
                System.out.println("Excel文件中没有数据");
                return;
            }
            
            // 创建数据库管理器并连接
            DatabaseManager dbManager = new DatabaseManager(dbFilePath);
            dbManager.connect();
            
            try {
                // 保存数据到数据库
                dbManager.saveExcelDataToDB(tableName, excelData, skipHeader);
                
                // 可选：查询并显示前5行数据以验证
                System.out.println("\n数据库表内容预览（前5行）：");
                dbManager.queryTableData(tableName, 5);
            } finally {
                // 确保关闭数据库连接
                dbManager.disconnect();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("保存到SQLite数据库出错: " + e.getMessage());
        }
    }

    /**
     * 测试方法，打印Excel文件内容
     * @param filePath Excel文件路径
     */
    public void printExcelContent(String filePath) {
        List<List<String>> data = readExcelFile(filePath);
        
        System.out.println("Excel文件内容：");
        for (List<String> row : data) {
            System.out.println(String.join(", ", row));
        }
    }

    /**
     * 从Excel文件中读取数据并映射到TemperatureData对象列表
     * 从第3行开始读取，按照指定的列位置提取数据
     * @param filePath Excel文件路径
     * @return TemperatureData对象列表
     */
    public List<TemperatureData> readExcelToTemperatureData(String filePath) {
        List<TemperatureData> temperatureDataList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz", Locale.ENGLISH);


        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();

            // 从第3行开始读取数据
            for (int rowIndex = 3; rowIndex < rowCount; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                TemperatureData data = new TemperatureData();

                try {
                    // 第2列是线别
                    if (row.getCell(2) != null) {
                        data.setLineNumber(getCellValueAsString(row.getCell(2)));
                    }

                    // 第3列是设备名称
                    if (row.getCell(3) != null) {
                        data.setDeviceName(getCellValueAsString(row.getCell(3)));
                    }

                    // 第4列是日期
                    if (row.getCell(4) != null) {
                        String dateStr = getCellValueAsString(row.getCell(4));
                        // 假设日期格式为月-日，例如 "03-15"
                        try {
                            // 尝试解析完整日期格式 "Fri Oct 10 00:00:00 CST"
                            // 由于MonthDay不能直接解析这种格式，我们先创建一个虚拟的LocalDate
                            String[] parts = dateStr.trim().split(" ");
                            if (parts.length >= 4) {
                                String monthStr = parts[1]; // 如 "Oct"
                                String dayStr = parts[2];   // 如 "10"

                                // 将月份名称转换为月份数字
                                int month = 1;
                                for (int m = 1; m <= 12; m++) {
                                    if (Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).equals(monthStr)) {
                                        month = m;
                                        break;
                                    }
                                }

                                // 解析日期
                                int day = Integer.parseInt(dayStr);
                                data.setDate(MonthDay.of(month, day));
                            } else {
                                // 如果格式不匹配，尝试原来的月-日格式作为备选
                                try {
                                    data.setDate(MonthDay.parse("--" + dateStr.replace("-", "-"), DateTimeFormatter.ofPattern("MM-dd")));
                                } catch (Exception e2) {
                                    System.err.println("日期解析失败: " + dateStr + " 在第" + (rowIndex + 1) + "行");
                                }
                            }
                            } catch (Exception e) {
                            // 如果解析失败，尝试其他格式或设置为null
                            System.err.println("日期解析失败: " + dateStr + " 在第" + (rowIndex + 1) + "行");
                        }
                    }

                    // 第5-12列是车上测量温度
                    // 假设顺序是：左3, 左4, 左5, 左6, 右3, 右4, 右5, 右6
                    data.setCarTempLeft3(getCellValueAsDouble(row.getCell(5)));
                    data.setCarTempLeft4(getCellValueAsDouble(row.getCell(6)));
                    data.setCarTempLeft5(getCellValueAsDouble(row.getCell(7)));
                    data.setCarTempLeft6(getCellValueAsDouble(row.getCell(8)));
                    data.setCarTempRight3(getCellValueAsDouble(row.getCell(9)));
                    data.setCarTempRight4(getCellValueAsDouble(row.getCell(10)));
                    data.setCarTempRight5(getCellValueAsDouble(row.getCell(11)));
                    data.setCarTempRight6(getCellValueAsDouble(row.getCell(12)));

                    // 第13-20列是地面探1温度
                    data.setGroundTemp1Left3(getCellValueAsDouble(row.getCell(13)));
                    data.setGroundTemp1Left4(getCellValueAsDouble(row.getCell(14)));
                    data.setGroundTemp1Left5(getCellValueAsDouble(row.getCell(15)));
                    data.setGroundTemp1Left6(getCellValueAsDouble(row.getCell(16)));
                    data.setGroundTemp1Right3(getCellValueAsDouble(row.getCell(17)));
                    data.setGroundTemp1Right4(getCellValueAsDouble(row.getCell(18)));
                    data.setGroundTemp1Right5(getCellValueAsDouble(row.getCell(19)));
                    data.setGroundTemp1Right6(getCellValueAsDouble(row.getCell(20)));

                    // 第21-28列是地面探2（内探）温度
                    data.setGroundTemp2Left3(getCellValueAsDouble(row.getCell(21)));
                    data.setGroundTemp2Left4(getCellValueAsDouble(row.getCell(22)));
                    data.setGroundTemp2Left5(getCellValueAsDouble(row.getCell(23)));
                    data.setGroundTemp2Left6(getCellValueAsDouble(row.getCell(24)));
                    data.setGroundTemp2Right3(getCellValueAsDouble(row.getCell(25)));
                    data.setGroundTemp2Right4(getCellValueAsDouble(row.getCell(26)));
                    data.setGroundTemp2Right5(getCellValueAsDouble(row.getCell(27)));
                    data.setGroundTemp2Right6(getCellValueAsDouble(row.getCell(28)));

                    // 第29-36列是地面探3温度
                    data.setGroundTemp3Left3(getCellValueAsDouble(row.getCell(29)));
                    data.setGroundTemp3Left4(getCellValueAsDouble(row.getCell(30)));
                    data.setGroundTemp3Left5(getCellValueAsDouble(row.getCell(31)));
                    data.setGroundTemp3Left6(getCellValueAsDouble(row.getCell(32)));
                    data.setGroundTemp3Right3(getCellValueAsDouble(row.getCell(33)));
                    data.setGroundTemp3Right4(getCellValueAsDouble(row.getCell(34)));
                    data.setGroundTemp3Right5(getCellValueAsDouble(row.getCell(35)));
                    data.setGroundTemp3Right6(getCellValueAsDouble(row.getCell(36)));

                    // 第37-44列是地面探4（外探）温度（索引36-43）
                    data.setGroundTemp4Left3(getCellValueAsDouble(row.getCell(37)));
                    data.setGroundTemp4Left4(getCellValueAsDouble(row.getCell(38)));
                    data.setGroundTemp4Left5(getCellValueAsDouble(row.getCell(39)));
                    data.setGroundTemp4Left6(getCellValueAsDouble(row.getCell(40)));
                    data.setGroundTemp4Right3(getCellValueAsDouble(row.getCell(41)));
                    data.setGroundTemp4Right4(getCellValueAsDouble(row.getCell(42)));
                    data.setGroundTemp4Right5(getCellValueAsDouble(row.getCell(43)));
                    data.setGroundTemp4Right6(getCellValueAsDouble(row.getCell(44)));

                    // 第45-52列是线性值（索引44-51）
                    // 假设顺序是：左3, 左4, 左5, 左6, 右3, 右4, 右5, 右6
                    data.setLinearValueTemp1left(getCellValueAsDouble(row.getCell(45)));
                    data.setLinearValueTemp1right(getCellValueAsDouble(row.getCell(46)));
                    data.setLinearValueTemp2left(getCellValueAsDouble(row.getCell(47)));
                    data.setLinearValueTemp2right(getCellValueAsDouble(row.getCell(48)));
                    data.setLinearValueTemp3left(getCellValueAsDouble(row.getCell(49)));
                    data.setLinearValueTemp3right(getCellValueAsDouble(row.getCell(50)));
                    data.setLinearValueTemp4left(getCellValueAsDouble(row.getCell(51)));
                    data.setLinearValueTemp4right(getCellValueAsDouble(row.getCell(52)));

                    // 第53-60列是非线性值（索引52-59）
                    // 假设顺序是：左3, 左4, 左5, 左6, 右3, 右4, 右5, 右6
                    data.setNonlinearValueTemp1left(getCellValueAsDouble(row.getCell(53)));
                    data.setNonlinearValueTemp1right(getCellValueAsDouble(row.getCell(54)));
                    data.setNonlinearValueTemp2left(getCellValueAsDouble(row.getCell(55)));
                    data.setNonlinearValueTemp2right(getCellValueAsDouble(row.getCell(56)));
                    data.setNonlinearValueTemp3left(getCellValueAsDouble(row.getCell(57)));
                    data.setNonlinearValueTemp3right(getCellValueAsDouble(row.getCell(58)));
                    data.setNonlinearValueTemp4left(getCellValueAsDouble(row.getCell(59)));
                    data.setNonlinearValueTemp4right(getCellValueAsDouble(row.getCell(60)));

                    // 第61-64列是板温
                    // 假设顺序是：板温内左, 板温内右, 板温外左, 板温外右
                    data.setPlateTempInnerLeft(getCellValueAsDouble(row.getCell(61)));
                    data.setPlateTempInnerRight(getCellValueAsDouble(row.getCell(62)));
                    data.setPlateTempOuterLeft(getCellValueAsDouble(row.getCell(63)));
                    data.setPlateTempOuterRight(getCellValueAsDouble(row.getCell(64)));


                    temperatureDataList.add(data);
                } catch (Exception e) {
                    System.err.println("处理第" + (rowIndex + 1) + "行数据时出错: " + e.getMessage());
                }
            }
            
            System.out.println("成功读取并转换了 " + temperatureDataList.size() + " 条温度数据记录");
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取Excel文件出错: " + e.getMessage());
        }
        
        return temperatureDataList;
    }
    
    /**
     * 获取单元格值并转换为字符串
     * @param cell Excel单元格
     * @return 单元格值的字符串表示
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
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
     * 获取单元格值并转换为double
     * @param cell Excel单元格
     * @return 单元格的双精度浮点数值，如果无法转换则返回0
     */
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String valueStr = cell.getStringCellValue().trim();
                    if (!valueStr.isEmpty()) {
                        return Double.parseDouble(valueStr);
                    }
                    return 0.0;
                default:
                    return 0.0;
            }
        } catch (NumberFormatException e) {
            System.err.println("数值转换失败: " + getCellValueAsString(cell));
            return 0.0;
        }
    }
}
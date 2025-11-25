package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalculationController {
    @FXML
    private ComboBox<String> lineComboBox;
    
    @FXML
    private ComboBox<String> deviceComboBox;
    
    @FXML
    private TextArea resultTextArea;
    
    // 表名
    private static final String TABLE_NAME = "temperature_data";
    
    // 类似QueryController的修改，移除dbManager成员变量，使用getInstance()获取实例
    
    @FXML
    public void initialize() {
        // 使用全局数据库连接
        // 初始化下拉框选项 - 从数据库获取
        initializeComboBoxes();
    }
    
    // 修改所有使用dbManager的方法，使用getInstance()获取实例
    
    // 修改returnToQueryView方法，移除disconnect调用
    @FXML
    public void returnToQueryView() {
        try {
            // 不再关闭数据库连接
            
            // 加载查询页面FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/query-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // 获取当前舞台并设置新场景
            Stage stage = (Stage) resultTextArea.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("温度数据查询");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "返回查询页面失败: " + e.getMessage());
        }
    }
    
    // 从数据库初始化下拉框选项
    private void initializeComboBoxes() {
        try {
            // 获取所有线别
            List<String> lineNumbers = getDistinctValuesFromColumn("line_number");
            lineComboBox.setItems(FXCollections.observableArrayList(lineNumbers));
            
            // 获取所有设备名称
            List<String> deviceNames = getDistinctValuesFromColumn("device_name");
            deviceComboBox.setItems(FXCollections.observableArrayList(deviceNames));
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("初始化下拉框失败: " + e.getMessage());
        }
    }

    // 从数据库获取指定列的 distinct 值
    private List<String> getDistinctValuesFromColumn(String columnName) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT " + columnName + " FROM " + TABLE_NAME + " ORDER BY " + columnName;

        // 修正：使用单例来获取连接
        try (Statement stmt = DatabaseManager.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String value = rs.getString(columnName);
                if (value != null && !value.trim().isEmpty()) {
                    values.add(value);
                }
            }
        }

        return values;
    }

    
    // 执行计算
    @FXML
    public void performCalculation() {
        try {
            // 获取用户选择的过滤条件
            String selectedLine = lineComboBox.getValue();
            String selectedDevice = deviceComboBox.getValue();
            
            // 从数据库查询数据
            List<TemperatureData> filteredData = executeTemperatureDataQueryWithParameters(selectedLine, selectedDevice);
            
            if (filteredData.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "没有找到匹配的温度数据");
                return;
            }
            
            StringBuilder resultBuilder = new StringBuilder();
            double totalCarTemp = 0;
            int validCount = 0;
            
            for (TemperatureData data : filteredData) {
                double rowCarTemp = data.getCarTempLeft3() + data.getCarTempLeft4() + data.getCarTempLeft5() + 
                                   data.getCarTempLeft6() + data.getCarTempRight3() + data.getCarTempRight4() + 
                                   data.getCarTempRight5() + data.getCarTempRight6();
                
                totalCarTemp += rowCarTemp;
                validCount++;
                
                String dateStr = data.getDate() != null ? data.getDate().format(DateTimeFormatter.ofPattern("MM-dd")) : "无日期";
                resultBuilder.append("线别: " + data.getLineNumber() + ", 设备: " + data.getDeviceName() + ", 日期: " + dateStr + ", 车上温度总和: " + rowCarTemp + "\n");
            }
            
            if (validCount > 0) {
                double averageCarTemp = totalCarTemp / (validCount * 8); // 8个温度值
                resultBuilder.append("\n总计算结果:");
                resultBuilder.append("\n有效计算记录数: " + validCount);
                resultBuilder.append("\n车上温度平均值: " + averageCarTemp);
            }
            
            // 显示计算结果
            resultTextArea.setText(resultBuilder.toString());
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "查询错误", "执行查询失败: " + e.getMessage());
        }
    }
    
    // 执行带参数的查询
    private List<TemperatureData> executeTemperatureDataQueryWithParameters(String lineNumber, String deviceName) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM " + TABLE_NAME + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (lineNumber != null && !lineNumber.isEmpty()) {
            sqlBuilder.append(" AND line_number = ?");
            params.add(lineNumber);
        }
        
        if (deviceName != null && !deviceName.isEmpty()) {
            sqlBuilder.append(" AND device_name = ?");
            params.add(deviceName);
        }
        
        return executeQuery(sqlBuilder.toString(), params, null);
    }
    
    // 执行SQL查询并返回结果
    private List<TemperatureData> executeQuery(String sql, List<Object> params, List<Integer> paramTypes) throws SQLException {
        List<TemperatureData> dataList = new ArrayList<>();
        
        // 修正：使用单例来获取连接
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            
            // 设置参数
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    if (paramTypes != null && i < paramTypes.size()) {
                        pstmt.setObject(i + 1, params.get(i), paramTypes.get(i));
                    } else {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                }
            }
            
            // 执行查询
            try (ResultSet rs = pstmt.executeQuery()) {
                // 映射结果集到TemperatureData对象
                while (rs.next()) {
                    TemperatureData data = mapResultSetToTemperatureData(rs);
                    dataList.add(data);
                }
            }
        }
        
        return dataList;
    }
    
    // 将结果集映射到TemperatureData对象
    private TemperatureData mapResultSetToTemperatureData(ResultSet rs) throws SQLException {
        TemperatureData data = new TemperatureData();
        
        data.setLineNumber(rs.getString("line_number"));
        data.setDeviceName(rs.getString("device_name"));
        
        // 处理日期字段
        String dateStr = rs.getString("date");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                // 预处理异常格式，移除可能的前导--
                String cleanDateStr = dateStr;
                if (cleanDateStr.startsWith("--")) {
                    cleanDateStr = cleanDateStr.substring(2);
                }
                
                // 解析MonthDay字符串 (MM-dd格式)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
                data.setDate(MonthDay.parse(cleanDateStr, formatter));
            } catch (Exception e) {
                // 如果解析失败，设置为null
                System.err.println("日期解析失败: " + dateStr);
                data.setDate(null);
            }
        }
        
        // 设置温度数据字段
        data.setCarTempLeft3(rs.getDouble("car_temp_left3"));
        data.setCarTempLeft4(rs.getDouble("car_temp_left4"));
        data.setCarTempLeft5(rs.getDouble("car_temp_left5"));
        data.setCarTempLeft6(rs.getDouble("car_temp_left6"));
        data.setCarTempRight3(rs.getDouble("car_temp_right3"));
        data.setCarTempRight4(rs.getDouble("car_temp_right4"));
        data.setCarTempRight5(rs.getDouble("car_temp_right5"));
        data.setCarTempRight6(rs.getDouble("car_temp_right6"));
        
        return data;
    }
    // 显示提示对话框
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
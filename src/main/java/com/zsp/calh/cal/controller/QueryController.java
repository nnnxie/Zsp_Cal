package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class QueryController {
    @FXML
    private ComboBox<String> lineComboBox;
    
    @FXML
    private ComboBox<String> deviceComboBox;
    
    @FXML
    private TableView<TemperatureData> dataTableView;
    
    @FXML
    private TableColumn<TemperatureData, String> lineNumberColumn;
    @FXML
    private TableColumn<TemperatureData, String> deviceNameColumn;
    @FXML
    private TableColumn<TemperatureData, String> dateColumn;
    @FXML
    private TableColumn<TemperatureData, Double> carTempLeft3Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempLeft4Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempLeft5Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempLeft6Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempRight3Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempRight4Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempRight5Column;
    @FXML
    private TableColumn<TemperatureData, Double> carTempRight6Column;
    
    // 数据库管理器
    // 移除这个成员变量
    // private DatabaseManager dbManager;
    
    // 表名
    private static final String TABLE_NAME = "temperature_data";
    
    // 移除原来的dbManager成员变量声明，并修改初始化方法
    
    @FXML
    public void initialize() {
        // 使用全局数据库连接

        // 初始化表格列
        initializeTableColumns();

        // 初始化下拉框选项 - 从数据库获取
        initializeComboBoxes();

        // 程序启动时就加载并显示所有数据
        showAllData();

    }
    
    // 修改所有使用dbManager的方法，使用getInstance()获取实例
    // 例如：
    private List<String> getDistinctValuesFromColumn(String columnName) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT " + columnName + " FROM " + TABLE_NAME + " ORDER BY " + columnName;
        
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
    
    // 修改returnToMainView和goToCalculationView方法，移除disconnect调用
    @FXML
    public void returnToMainView() {
        try {
            // 不再关闭数据库连接
            
            // 加载主界面FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/hello-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // 获取当前舞台并设置新场景
            Stage stage = (Stage) dataTableView.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 300));
            stage.setTitle("温度数据处理工具");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "返回主界面失败: " + e.getMessage());
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
    
    // 初始化表格列
    private void initializeTableColumns() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        lineNumberColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLineNumber()));
        deviceNameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDeviceName()));
        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDate() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate().format(formatter));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        carTempLeft3Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempLeft3()).asObject());
        carTempLeft4Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempLeft4()).asObject());
        carTempLeft5Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempLeft5()).asObject());
        carTempLeft6Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempLeft6()).asObject());
        carTempRight3Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempRight3()).asObject());
        carTempRight4Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempRight4()).asObject());
        carTempRight5Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempRight5()).asObject());
        carTempRight6Column.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getCarTempRight6()).asObject());
    }
    
    // 显示所有数据
    @FXML
    public void showAllData() {
        try {
            List<TemperatureData> dataList = executeTemperatureDataQuery();
            dataTableView.setItems(FXCollections.observableArrayList(dataList));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "查询错误", "无法查询数据: " + e.getMessage());
        }
    }
    
    // 执行查询
    @FXML
    public void performQuery() {
        String selectedLine = lineComboBox.getValue();
        String selectedDevice = deviceComboBox.getValue();
        
        try {
            List<TemperatureData> filteredData = executeTemperatureDataQueryWithParameters(selectedLine, selectedDevice);
            
            // 显示查询结果
            dataTableView.setItems(FXCollections.observableArrayList(filteredData));
            
            showAlert(Alert.AlertType.INFORMATION, "查询结果", "找到 " + filteredData.size() + " 条匹配记录");
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "查询错误", "执行查询失败: " + e.getMessage());
        }
    }
    
    // 执行不带参数的查询
    private List<TemperatureData> executeTemperatureDataQuery() throws SQLException {
        String sql = "SELECT * FROM " + TABLE_NAME;
        return executeQuery(sql, null, null);
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
    // 修改executeQuery方法
    private List<TemperatureData> executeQuery(String sql, List<Object> params, List<Integer> paramTypes) throws SQLException {
        List<TemperatureData> dataList = new ArrayList<>();
        
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
        
        // 处理日期字段 - 修复日期解析失败问题
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


    
    // 跳转到计算页面
    @FXML
    public void goToCalculationView() {
        try {
            // 移除数据库关闭操作
            // if (dbManager != null) {
            //     dbManager.disconnect();
            // }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/calculation-view.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) dataTableView.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("温度数据计算");
            stage.show();

        } catch (IOException e) { // 移除了 SQLException
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "跳转到计算页面失败: " + e.getMessage());
        }
    }
    
    // 显示提示对话框
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
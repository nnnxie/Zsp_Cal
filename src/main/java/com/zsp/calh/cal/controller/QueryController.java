package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.model.TemperatureDataModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
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
    
    // 存储所有温度数据
    private List<TemperatureData> allTemperatureData;
    
    @FXML
    public void initialize() {
        // 获取温度数据模型的实例
        TemperatureDataModel dataModel = TemperatureDataModel.getInstance();
        
        // 获取数据
        allTemperatureData = dataModel.getTemperatureDataList();
        
        // 初始化下拉框
        lineComboBox.setItems(FXCollections.observableArrayList(dataModel.getAllLineNumbers()));
        deviceComboBox.setItems(FXCollections.observableArrayList(dataModel.getAllDeviceNames()));
        
        // 初始化表格列
        initializeTableColumns();
        
        // 显示所有数据
        showAllData();
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
        dataTableView.setItems(FXCollections.observableArrayList(allTemperatureData));
    }
    
    // 执行查询
    @FXML
    public void performQuery() {
        String selectedLine = lineComboBox.getValue();
        String selectedDevice = deviceComboBox.getValue();
        
        // 使用数据模型进行查询
        TemperatureDataModel dataModel = TemperatureDataModel.getInstance();
        List<TemperatureData> filteredData = dataModel.filterData(selectedLine, selectedDevice);
        
        // 显示查询结果
        dataTableView.setItems(FXCollections.observableArrayList(filteredData));
        
        showAlert(Alert.AlertType.INFORMATION, "查询结果", "找到 " + filteredData.size() + " 条匹配记录");
    }
    
    // 返回主界面
    @FXML
    public void returnToMainView() {
        try {
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
    
    // 跳转到计算页面
    @FXML
    public void goToCalculationView() {
        try {
            // 加载计算页面FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/calculation-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // 获取当前舞台并设置新场景
            Stage stage = (Stage) dataTableView.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("温度数据计算");
            stage.show();
            
        } catch (IOException e) {
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
package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.model.TemperatureDataModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CalculationController {
    @FXML
    private ComboBox<String> lineComboBox;
    
    @FXML
    private ComboBox<String> deviceComboBox;
    
    @FXML
    private TextArea resultTextArea;
    
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
    }
    
    // 执行计算
    @FXML
    public void performCalculation() {
        if (allTemperatureData == null || allTemperatureData.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "没有可用的温度数据");
            return;
        }
        
        // 获取用户选择的过滤条件
        String selectedLine = lineComboBox.getValue();
        String selectedDevice = deviceComboBox.getValue();
        
        // 过滤数据
        List<TemperatureData> filteredData = allTemperatureData;
        if (selectedLine != null && !selectedLine.isEmpty()) {
            filteredData = filteredData.stream()
                .filter(data -> data.getLineNumber() != null && data.getLineNumber().equals(selectedLine))
                .toList();
        }
        if (selectedDevice != null && !selectedDevice.isEmpty()) {
            filteredData = filteredData.stream()
                .filter(data -> data.getDeviceName() != null && data.getDeviceName().equals(selectedDevice))
                .toList();
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
            
            String dateStr = data.getDate() != null ? data.getDate().toString() : "无日期";
            resultBuilder.append("线别: " + data.getLineNumber() + ", 设备: " + data.getDeviceName() + ", 日期: " + dateStr + ", 车上温度总和: " + rowCarTemp + "\n");
        }
        
        if (validCount > 0) {
            double averageCarTemp = totalCarTemp / (validCount * 8); // 8个温度值
            resultBuilder.append("\n总计算结果:");
            resultBuilder.append("\n有效计算记录数: " + validCount);
            resultBuilder.append("\n车上温度平均值: " + averageCarTemp);
        } else {
            resultBuilder.append("没有找到匹配的数据记录");
        }
        
        // 显示计算结果
        resultTextArea.setText(resultBuilder.toString());
    }
    
    // 返回查询页面
    @FXML
    public void returnToQueryView() {
        try {
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
    
    // 显示提示对话框
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
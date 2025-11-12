package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.ExcelDataModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class CalculationController {
    @FXML
    private ComboBox<String> comboBox1;
    
    @FXML
    private ComboBox<String> comboBox2;
    
    @FXML
    private ComboBox<String> comboBox3;
    
    @FXML
    private ComboBox<String> comboBox4;
    
    @FXML
    private ComboBox<String> comboBox5;
    
    @FXML
    private ComboBox<String> comboBox6;
    
    @FXML
    private ComboBox<String> comboBox7;
    
    @FXML
    private TextArea resultTextArea;
    
    // 存储表头信息
    private List<String> headers;
    
    // 存储所有Excel数据
    private List<List<String>> allExcelData;
    
    @FXML
    public void initialize() {
        // 获取Excel数据模型的实例
        ExcelDataModel dataModel = ExcelDataModel.getInstance();
        
        // 获取表头和数据
        headers = dataModel.getHeaders();
        allExcelData = dataModel.getExcelData();
        
        // 初始化所有下拉框
        if (headers != null && !headers.isEmpty()) {
            comboBox1.setItems(FXCollections.observableArrayList(headers));
            comboBox2.setItems(FXCollections.observableArrayList(headers));
            comboBox3.setItems(FXCollections.observableArrayList(headers));
            comboBox4.setItems(FXCollections.observableArrayList(headers));
            comboBox5.setItems(FXCollections.observableArrayList(headers));
            comboBox6.setItems(FXCollections.observableArrayList(headers));
            comboBox7.setItems(FXCollections.observableArrayList(headers));
            
            // 默认选择第一个选项
            comboBox1.setValue(headers.get(0));
            comboBox2.setValue(headers.get(0));
            comboBox3.setValue(headers.get(0));
            comboBox4.setValue(headers.get(0));
            comboBox5.setValue(headers.get(0));
            comboBox6.setValue(headers.get(0));
            comboBox7.setValue(headers.get(0));
        } else {
            resultTextArea.setText("没有可用的Excel数据");
        }
    }
    
    // 执行计算
    @FXML
    public void performCalculation() {
        if (allExcelData == null || allExcelData.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "没有可用的Excel数据");
            return;
        }
        
        // 获取用户选择的列
        String selectedCol1 = comboBox1.getValue();
        String selectedCol2 = comboBox2.getValue();
        String selectedCol3 = comboBox3.getValue();
        String selectedCol4 = comboBox4.getValue();
        String selectedCol5 = comboBox5.getValue();
        String selectedCol6 = comboBox6.getValue();
        String selectedCol7 = comboBox7.getValue();
        
        // 检查是否所有选项都已选择
        if (selectedCol1 == null || selectedCol2 == null || selectedCol3 == null || 
            selectedCol4 == null || selectedCol5 == null || selectedCol6 == null || selectedCol7 == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择所有计算参数");
            return;
        }
        
        // 获取列索引
        int colIndex1 = getColumnIndex(selectedCol1);
        int colIndex2 = getColumnIndex(selectedCol2);
        int colIndex3 = getColumnIndex(selectedCol3);
        int colIndex4 = getColumnIndex(selectedCol4);
        int colIndex5 = getColumnIndex(selectedCol5);
        int colIndex6 = getColumnIndex(selectedCol6);
        int colIndex7 = getColumnIndex(selectedCol7);
        
        // 执行相加计算
        double sum = 0;
        int validCount = 0;
        
        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("计算结果\n\n");
        resultBuilder.append("选择的列: " + selectedCol1 + ", " + selectedCol2 + ", " + selectedCol3 + ", " + 
                            selectedCol4 + ", " + selectedCol5 + ", " + selectedCol6 + ", " + selectedCol7 + "\n\n");
        
        for (int i = 0; i < allExcelData.size(); i++) {
            List<String> row = allExcelData.get(i);
            double rowSum = 0;
            boolean rowHasValue = false;
            
            // 尝试从每行获取并累加数值
            try {
                if (colIndex1 < row.size() && !row.get(colIndex1).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex1).trim());
                    rowHasValue = true;
                }
                if (colIndex2 < row.size() && !row.get(colIndex2).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex2).trim());
                    rowHasValue = true;
                }
                if (colIndex3 < row.size() && !row.get(colIndex3).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex3).trim());
                    rowHasValue = true;
                }
                if (colIndex4 < row.size() && !row.get(colIndex4).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex4).trim());
                    rowHasValue = true;
                }
                if (colIndex5 < row.size() && !row.get(colIndex5).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex5).trim());
                    rowHasValue = true;
                }
                if (colIndex6 < row.size() && !row.get(colIndex6).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex6).trim());
                    rowHasValue = true;
                }
                if (colIndex7 < row.size() && !row.get(colIndex7).trim().isEmpty()) {
                    rowSum += Double.parseDouble(row.get(colIndex7).trim());
                    rowHasValue = true;
                }
                
                if (rowHasValue) {
                    sum += rowSum;
                    validCount++;
                    resultBuilder.append("行 " + (i + 1) + " 计算结果: " + rowSum + "\n");
                }
            } catch (NumberFormatException e) {
                // 如果无法解析为数字，则跳过此行
                resultBuilder.append("行 " + (i + 1) + ": 包含非数字值，无法计算\n");
            }
        }
        
        resultBuilder.append("\n总计算结果: " + sum);
        resultBuilder.append("\n有效计算行数: " + validCount);
        
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
            stage.setTitle("Excel数据查询");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "返回查询页面失败: " + e.getMessage());
        }
    }
    
    // 获取列索引
    private int getColumnIndex(String columnName) {
        if (headers != null) {
            return headers.indexOf(columnName);
        }
        return -1;
    }
    
    // 显示提示对话框
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.ExcelDataModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryController {
    @FXML
    private ComboBox<String> columnComboBox;
    
    @FXML
    private TextField keywordTextField;
    
    @FXML
    private TableView<Map<String, String>> dataTableView;
    
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
        
        // 初始化列下拉框
        if (headers != null && !headers.isEmpty()) {
            columnComboBox.setItems(FXCollections.observableArrayList(headers));
            if (!headers.isEmpty()) {
                columnComboBox.setValue(headers.get(0));
            }
            
            // 初始化表格列
            initializeTableColumns();
            
            // 显示所有数据
            showAllData();
        } else {
            System.err.println("没有可用的Excel数据");
        }
    }
    
    // 初始化表格列
    private void initializeTableColumns() {
        dataTableView.getColumns().clear();
        
        for (int i = 0; i < headers.size(); i++) {
            final int columnIndex = i;
            TableColumn<Map<String, String>, String> column = new TableColumn<>(headers.get(i));
            column.setCellValueFactory(cellData -> {
                Map<String, String> rowMap = cellData.getValue();
                String value = rowMap != null ? rowMap.getOrDefault("col" + columnIndex, "") : "";
                return new javafx.beans.property.SimpleStringProperty(value);
            });
            dataTableView.getColumns().add(column);
        }
    }
    
    // 显示所有数据
    @FXML
    public void showAllData() {
        populateTableView(allExcelData);
    }
    
    // 执行查询
    @FXML
    public void performQuery() {
        String selectedColumn = columnComboBox.getValue();
        String keyword = keywordTextField.getText();
        
        if (selectedColumn == null || keyword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择查询列并输入关键词");
            return;
        }
        
        // 使用数据模型进行查询
        ExcelDataModel dataModel = ExcelDataModel.getInstance();
        List<List<String>> filteredData = dataModel.filterData(selectedColumn, keyword);
        
        // 显示查询结果
        populateTableView(filteredData);
        
        showAlert(Alert.AlertType.INFORMATION, "查询结果", "找到 " + filteredData.size() + " 条匹配记录");
    }
    
    // 填充表格数据
    private void populateTableView(List<List<String>> data) {
        ObservableList<Map<String, String>> tableData = FXCollections.observableArrayList();
        
        for (List<String> row : data) {
            Map<String, String> rowData = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                if (i < headers.size()) {
                    rowData.put("col" + i, row.get(i));
                }
            }
            tableData.add(rowData);
        }
        
        dataTableView.setItems(tableData);
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
            stage.setTitle("Excel文件读取工具");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "返回主界面失败: " + e.getMessage());
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
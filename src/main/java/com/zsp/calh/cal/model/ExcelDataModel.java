package com.zsp.calh.cal.model;

import java.util.ArrayList;
import java.util.List;

// 导入TemperatureData类
import com.zsp.calh.cal.model.TemperatureData;

public class ExcelDataModel {
    // 单例模式，用于在不同Controller之间共享数据
    private static ExcelDataModel instance;
    
    // 存储Excel数据的二维列表
    private List<List<String>> excelData;
    
    // 存储Excel表头信息
    private List<String> headers;
    
    // 存储映射后的TemperatureData对象列表
    private List<TemperatureData> temperatureDataList;
    
    private ExcelDataModel() {
        this.excelData = new ArrayList<>();
        this.headers = new ArrayList<>();
        this.temperatureDataList = new ArrayList<>();
    }
    
    // 获取单例实例
    public static synchronized ExcelDataModel getInstance() {
        if (instance == null) {
            instance = new ExcelDataModel();
        }
        return instance;
    }
    
    // 设置Excel数据
    public void setExcelData(List<List<String>> data) {
        this.excelData.clear();
        this.headers.clear();
        
        if (data != null && !data.isEmpty()) {
            // 假设第一行是表头
            this.headers.addAll(data.get(0));
            
            // 其余行是数据
            for (int i = 1; i < data.size(); i++) {
                this.excelData.add(new ArrayList<>(data.get(i)));
            }
        }
    }
    
    // 获取Excel数据
    public List<List<String>> getExcelData() {
        return new ArrayList<>(excelData);
    }
    
    // 获取表头
    public List<String> getHeaders() {
        return new ArrayList<>(headers);
    }
    
    // 设置TemperatureData对象列表
    public void setTemperatureDataList(List<TemperatureData> dataList) {
        this.temperatureDataList.clear();
        if (dataList != null) {
            this.temperatureDataList.addAll(dataList);
        }
    }
    
    // 获取TemperatureData对象列表
    public List<TemperatureData> getTemperatureDataList() {
        return new ArrayList<>(temperatureDataList);
    }
    
    // 根据查询条件过滤数据
    public List<List<String>> filterData(String columnName, String keyword) {
        List<List<String>> filteredData = new ArrayList<>();
        
        // 查找列索引
        int columnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equals(columnName)) {
                columnIndex = i;
                break;
            }
        }
        
        // 如果找到了对应的列
        if (columnIndex != -1) {
            for (List<String> row : excelData) {
                if (row.size() > columnIndex && row.get(columnIndex).contains(keyword)) {
                    filteredData.add(row);
                }
            }
        }
        
        return filteredData;
    }
}
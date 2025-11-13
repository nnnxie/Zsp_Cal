package com.zsp.calh.cal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 温度数据模型，用于在不同Controller之间共享TemperatureData对象列表
 * 采用单例模式
 */
public class TemperatureDataModel {
    // 单例实例
    private static TemperatureDataModel instance;
    
    // 存储温度数据列表
    private List<TemperatureData> temperatureDataList;
    
    private TemperatureDataModel() {
        this.temperatureDataList = new ArrayList<>();
    }
    
    // 获取单例实例
    public static synchronized TemperatureDataModel getInstance() {
        if (instance == null) {
            instance = new TemperatureDataModel();
        }
        return instance;
    }
    
    // 设置温度数据列表
    public void setTemperatureDataList(List<TemperatureData> dataList) {
        this.temperatureDataList.clear();
        if (dataList != null) {
            this.temperatureDataList.addAll(dataList);
        }
    }
    
    // 获取温度数据列表
    public List<TemperatureData> getTemperatureDataList() {
        return new ArrayList<>(temperatureDataList);
    }
    
    // 根据线别和设备名称过滤数据
    public List<TemperatureData> filterData(String lineNumber, String deviceName) {
        return temperatureDataList.stream()
            .filter(data -> (lineNumber == null || lineNumber.isEmpty() || 
                    (data.getLineNumber() != null && data.getLineNumber().contains(lineNumber))))
            .filter(data -> (deviceName == null || deviceName.isEmpty() || 
                    (data.getDeviceName() != null && data.getDeviceName().contains(deviceName))))
            .collect(Collectors.toList());
    }
    
    // 获取所有线别
    public List<String> getAllLineNumbers() {
        return temperatureDataList.stream()
            .map(TemperatureData::getLineNumber)
            .distinct()
            .collect(Collectors.toList());
    }
    
    // 获取所有设备名称
    public List<String> getAllDeviceNames() {
        return temperatureDataList.stream()
            .map(TemperatureData::getDeviceName)
            .distinct()
            .collect(Collectors.toList());
    }
}
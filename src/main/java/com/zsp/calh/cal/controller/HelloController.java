package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.model.TemperatureDataModel;
import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.zsp.calh.cal.model.ExcelDataModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class HelloController {
    @FXML
    private Label welcomeText;
    
    @FXML
    private TextField filePathField;
    
    /**
     * 浏览文件按钮的事件处理方法
     */
    @FXML
    protected void browseFile() {
        FileChooser fileChooser = new FileChooser();
        
        // 设置文件选择器的标题
        fileChooser.setTitle("选择Excel文件");
        
        // 设置初始目录（可选）
        fileChooser.setInitialDirectory(
            new File(System.getProperty("user.home"))
        );
        
        // 添加文件过滤器
        FileChooser.ExtensionFilter extFilterXLSX = new FileChooser.ExtensionFilter("Excel文件 (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter extFilterXLS = new FileChooser.ExtensionFilter("Excel 97-2003文件 (*.xls)", "*.xls");
        fileChooser.getExtensionFilters().addAll(extFilterXLSX, extFilterXLS);
        
        // 显示文件选择对话框
        File selectedFile = fileChooser.showOpenDialog(getStage());
        
        // 如果用户选择了文件，设置文件路径到文本字段
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * 读取Excel文件按钮的事件处理方法
     */
    @FXML
    protected void readExcelFile() {
        String filePath = filePathField.getText();

        if (filePath.isEmpty()) {
            welcomeText.setText("请先选择一个Excel文件");
            return;
        }

        // 在这里初始化数据库管理器，确保在任何数据库操作之前完成
        // 这个初始化应该是应用级别的，放在这里只是一个示例
        // 更好的地方是在主应用的 start() 方法中
        String dbFilePath = "temperature_data.db";
        DatabaseManager.initializeInstance(dbFilePath);

        LoadController loadController = new LoadController();
        List<TemperatureData> data = loadController.readExcelToTemperatureData(filePath);

        if (!data.isEmpty()) {
            TemperatureDataModel dataModel = TemperatureDataModel.getInstance();
            dataModel.setTemperatureDataList(data);

            // 调用修改后的保存方法
            String tableName = "temperature_data";
            loadController.saveTemperatureDataToSQLite(data, tableName);

            welcomeText.setText("成功读取Excel文件并保存到数据库，共 " + data.size() + " 条数据记录");

            // ... 跳转逻辑不变 ...
        } else {
            welcomeText.setText("读取Excel文件失败或文件为空");
        }
    }
    
    /**
     * 跳转到计算页面的按钮事件处理方法
     */
    @FXML
    protected void goToCalculationView() {
        try {
            // 加载计算页面FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/calculation-view.fxml"));
            Parent root = fxmlLoader.load();
            
            // 获取当前舞台并设置新场景
            Stage stage = getStage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Excel数据计算");
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            welcomeText.setText("跳转到计算页面失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前舞台
     */
    private Stage getStage() {
        // 获取当前场景的舞台
        return (Stage) welcomeText.getScene().getWindow();
    }
}
package com.zsp.calh.cal;

import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. 初始化数据库连接 (单例)
        try {
            DatabaseManager.initializeInstance("temperature_data.db");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return; // 数据库失败则不启动界面
        }

        // 2. 加载查询页面 (Query View) 作为主入口
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("query-view.fxml"));

        // 设置一个较大的默认窗口，适应表格显示
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);

        stage.setTitle("温度数据管理系统");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // 应用关闭时断开数据库
        try {
            DatabaseManager.getInstance().disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
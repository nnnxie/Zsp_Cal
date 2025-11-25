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
        // 1. 在应用启动时，使用 initializeInstance 初始化全局唯一的数据库管理器
        // 这是管理数据库连接的推荐位置，确保它只被调用一次。
        try {
            String dbFilePath = "temperature_data.db"; // 定义数据库文件名
            DatabaseManager.initializeInstance(dbFilePath);
            System.out.println("数据库连接已在应用启动时建立。");
        } catch (RuntimeException e) {
            // 如果数据库初始化失败，这是一个严重错误，应用可能无法继续。
            // 打印错误并考虑是否要退出应用。
            System.err.println("数据库初始化失败，应用无法启动: " + e.getMessage());
            e.printStackTrace();
            // Platform.exit(); // 如果需要，可以取消注释以退出应用
            return; // 停止进一步的UI加载
        }

        // 2. 加载主界面 (hello-view.fxml)
        // 根据您的项目结构，初始界面似乎是 hello-view.fxml，而不是 query-view.fxml
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 300); // 调整为 hello-view 的尺寸
        stage.setTitle("温度数据处理工具");
        stage.setScene(scene);

        // 3. 不再需要 setOnCloseRequest，因为 stop() 方法是更好的选择

        stage.show();
    }

    /**
     * JavaFX应用生命周期方法，在应用关闭时自动调用。
     * 这是断开数据库连接、释放资源的最可靠位置。
     */
    @Override
    public void stop() throws Exception {
        try {
            DatabaseManager.getInstance().disconnect();
            System.out.println("应用关闭，数据库连接已成功断开。");
        } catch (Exception e) {
            System.err.println("关闭数据库连接时出错: " + e.getMessage());
            e.printStackTrace();
        }
        super.stop(); // 调用父类的stop方法
    }

    public static void main(String[] args) {
        launch();
    }
}
package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QueryController {
    // 筛选控件
    @FXML private ComboBox<String> dateComboBox;
    @FXML private ComboBox<String> lineComboBox;
    @FXML private ComboBox<String> deviceComboBox;
    @FXML private ComboBox<String> probeComboBox;
    @FXML private ComboBox<String> sideComboBox;

    // 表格与状态
    @FXML private TableView<TemperatureData> dataTableView;
    @FXML private Label statusLabel;

    // 基础固定列
    private TableColumn<TemperatureData, String> lineNumberColumn;
    private TableColumn<TemperatureData, String> deviceNameColumn;
    private TableColumn<TemperatureData, String> dateColumn;

    private static final String TABLE_NAME = "temperature_data";

    @FXML
    public void initialize() {
        setupBaseColumns();
        initializeComboBoxes();

        // 默认选中项
        probeComboBox.getSelectionModel().select("地面探头1");
        sideComboBox.getSelectionModel().select("左");

        statusLabel.setText("系统就绪，请选择筛选条件或导入数据");
    }

    /**
     * 导入 Excel 文件并保存到数据库
     */
    @FXML
    public void importExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls")
        );

        Stage stage = (Stage) dataTableView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            statusLabel.setText("正在处理文件: " + selectedFile.getName() + " ...");

            try {
                // 使用 LoadController 读取和保存
                LoadController loadController = new LoadController();
                List<TemperatureData> data = loadController.readExcelToTemperatureData(selectedFile.getAbsolutePath());

                if (data != null && !data.isEmpty()) {
                    // 【修改】获取实际插入的条数（int）
                    int insertedCount = loadController.saveTemperatureDataToSQLite(data, TABLE_NAME);
                    int ignoredCount = data.size() - insertedCount;

                    // 刷新下拉框（可能有新日期/设备）
                    initializeComboBoxes();

                    // 【修改】构建详细的提示信息
                    String msg = String.format("读取总数：%d 条\n成功入库：%d 条\n忽略重复：%d 条",
                            data.size(), insertedCount, ignoredCount);

                    statusLabel.setText("导入完成：" + msg.replace("\n", ", "));
                    showAlert(Alert.AlertType.INFORMATION, "导入结果", msg);

                    // 自动刷新当前查询
                    performQuery();
                } else {
                    statusLabel.setText("导入失败：文件为空或无有效数据");
                    showAlert(Alert.AlertType.WARNING, "数据为空", "未从文件中读取到有效数据。");
                }
            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setText("导入出错: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "导入错误", e.getMessage());
            }
        }
    }

    @FXML
    public void performQuery() {
        String date = dateComboBox.getValue();
        String line = lineComboBox.getValue();
        String device = deviceComboBox.getValue();
        String probeStr = probeComboBox.getValue();
        String sideStr = sideComboBox.getValue();

        if (probeStr == null || sideStr == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择探头和方位，以便生成对应的列。");
            return;
        }

        try {
            // 1. 获取数据
            List<TemperatureData> data = executeQuery(date, line, device);

            // 2. 动态生成列
            updateTableViewColumns(probeStr, sideStr);

            // 3. 填充表格
            dataTableView.setItems(FXCollections.observableArrayList(data));
            statusLabel.setText("查询完成，显示 " + data.size() + " 条记录");

        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("数据库查询失败");
            showAlert(Alert.AlertType.ERROR, "查询失败", e.getMessage());
        }
    }

    @FXML
    public void showAllData() {
        dateComboBox.setValue(null);
        lineComboBox.setValue(null);
        deviceComboBox.setValue(null);
        performQuery();
    }

    @FXML
    public void goToCalculationView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/calculation-view.fxml"));
            Stage stage = (Stage) dataTableView.getScene().getWindow();
            // 保持窗口大小一致
            stage.setScene(new Scene(fxmlLoader.load(), 1100, 750));
            stage.setTitle("数据计算中心");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "跳转失败", e.getMessage());
        }
    }

    // --- 内部逻辑方法 ---

    private void initializeComboBoxes() {
        try {
            // 尝试获取旧值，如果还没初始化则为null
            String oldDate = (dateComboBox.getValue() != null) ? dateComboBox.getValue() : null;
            String oldLine = (lineComboBox.getValue() != null) ? lineComboBox.getValue() : null;
            String oldDevice = (deviceComboBox.getValue() != null) ? deviceComboBox.getValue() : null;

            // 填充下拉框
            dateComboBox.setItems(FXCollections.observableArrayList(getDistinctValues("date")));
            lineComboBox.setItems(FXCollections.observableArrayList(getDistinctValues("line_number")));
            deviceComboBox.setItems(FXCollections.observableArrayList(getDistinctValues("device_name")));
            probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
            sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));

            // 恢复选中状态
            if (oldDate != null && dateComboBox.getItems().contains(oldDate)) dateComboBox.setValue(oldDate);
            if (oldLine != null && lineComboBox.getItems().contains(oldLine)) lineComboBox.setValue(oldLine);
            if (oldDevice != null && deviceComboBox.getItems().contains(oldDevice)) deviceComboBox.setValue(oldDevice);

        } catch (SQLException e) {
            // 核心修复：检查是否是"表不存在"的错误
            if (e.getMessage() != null && e.getMessage().contains("no such table")) {
                System.out.println("第一次运行：数据表尚未创建，跳过下拉框初始化。");
                // 可以在这里设置一些默认提示，或者什么都不做
                statusLabel.setText("欢迎！请先点击“导入Excel数据”以创建数据库。");
            } else {
                // 其他数据库错误则打印堆栈
                e.printStackTrace();
            }
        }
    }

    private void setupBaseColumns() {
        lineNumberColumn = new TableColumn<>("线别");
        lineNumberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLineNumber()));

        deviceNameColumn = new TableColumn<>("设备名称");
        deviceNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDeviceName()));

        dateColumn = new TableColumn<>("日期");
        dateColumn.setCellValueFactory(cell -> {
            if (cell.getValue().getDate() != null) {
                return new SimpleStringProperty(cell.getValue().getDate().format(DateTimeFormatter.ofPattern("MM-dd")));
            }
            return new SimpleStringProperty("");
        });
    }

    /**
     * 根据选择动态生成表格列
     */
    private void updateTableViewColumns(String probeStr, String sideStr) {
        dataTableView.getColumns().clear();
        dataTableView.getColumns().addAll(lineNumberColumn, deviceNameColumn, dateColumn);

        // 解析: "地面探头1" -> 1
        int probeIndex = Integer.parseInt(probeStr.replace("地面探头", ""));
        boolean isLeft = "左".equals(sideStr);
        String sideEn = isLeft ? "Left" : "Right";

        // 动态添加4个车上温度列 (3-6)
        for (int i = 3; i <= 6; i++) {
            TableColumn<TemperatureData, Double> col = new TableColumn<>("车上" + sideStr + i);
            String methodName = "getCarTemp" + sideEn + i; // e.g., getCarTempLeft3
            col.setCellValueFactory(cell -> getReflectedProperty(cell.getValue(), methodName));
            col.setPrefWidth(90);
            dataTableView.getColumns().add(col);
        }

        // 动态添加4个地面温度列 (3-6)
        for (int i = 3; i <= 6; i++) {
            TableColumn<TemperatureData, Double> col = new TableColumn<>("地面" + probeIndex + sideStr + i);
            String methodName = "getGroundTemp" + probeIndex + sideEn + i; // e.g., getGroundTemp1Left3
            col.setCellValueFactory(cell -> getReflectedProperty(cell.getValue(), methodName));
            col.setPrefWidth(110);
            dataTableView.getColumns().add(col);
        }
    }

    // 反射辅助方法
    private SimpleObjectProperty<Double> getReflectedProperty(TemperatureData data, String methodName) {
        try {
            Method m = TemperatureData.class.getMethod(methodName);
            return new SimpleObjectProperty<>((Double) m.invoke(data));
        } catch (Exception e) {
            return new SimpleObjectProperty<>(0.0);
        }
    }

    private List<TemperatureData> executeQuery(String date, String line, String device) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (date != null && !date.isEmpty()) { sql.append(" AND date = ?"); params.add(date); }
        if (line != null && !line.isEmpty()) { sql.append(" AND line_number = ?"); params.add(line); }
        if (device != null && !device.isEmpty()) { sql.append(" AND device_name = ?"); params.add(device); }

        List<TemperatureData> list = new ArrayList<>();
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTemperatureData(rs));
                }
            }
        }
        return list;
    }

    private List<String> getDistinctValues(String col) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT " + col + " FROM " + TABLE_NAME + " ORDER BY " + col;
        try (Statement stmt = DatabaseManager.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String val = rs.getString(1);
                if (val != null && !val.isEmpty()) list.add(val);
            }
        }
        return list;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // --- 完整的数据映射 (确保所有字段都被读取) ---
    private TemperatureData mapResultSetToTemperatureData(ResultSet rs) throws SQLException {
        TemperatureData data = new TemperatureData();
        data.setId(rs.getLong("id"));
        data.setLineNumber(rs.getString("line_number"));
        data.setDeviceName(rs.getString("device_name"));

        // 日期处理
        String dateStr = rs.getString("date");
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                if (dateStr.startsWith("--")) dateStr = dateStr.substring(2);
                data.setDate(MonthDay.parse(dateStr, DateTimeFormatter.ofPattern("MM-dd")));
            } catch (Exception e) { /* ignore */ }
        }

        // 车上温度 (Left3-6, Right3-6)
        data.setCarTempLeft3(rs.getDouble("car_temp_left3"));
        data.setCarTempLeft4(rs.getDouble("car_temp_left4"));
        data.setCarTempLeft5(rs.getDouble("car_temp_left5"));
        data.setCarTempLeft6(rs.getDouble("car_temp_left6"));
        data.setCarTempRight3(rs.getDouble("car_temp_right3"));
        data.setCarTempRight4(rs.getDouble("car_temp_right4"));
        data.setCarTempRight5(rs.getDouble("car_temp_right5"));
        data.setCarTempRight6(rs.getDouble("car_temp_right6"));

        // 地面探头 1-4
        for (int i = 1; i <= 4; i++) {
            // Left/Right 3-6
            setGroundTemp(data, rs, i, "left", 3);
            setGroundTemp(data, rs, i, "left", 4);
            setGroundTemp(data, rs, i, "left", 5);
            setGroundTemp(data, rs, i, "left", 6);
            setGroundTemp(data, rs, i, "right", 3);
            setGroundTemp(data, rs, i, "right", 4);
            setGroundTemp(data, rs, i, "right", 5);
            setGroundTemp(data, rs, i, "right", 6);

            // 线性与非线性 (Left/Right)
            setLinearNonLinear(data, rs, i, "left");
            setLinearNonLinear(data, rs, i, "right");
        }

        // 板温
        data.setPlateTempInnerLeft(rs.getDouble("plate_temp_inner_left"));
        data.setPlateTempInnerRight(rs.getDouble("plate_temp_inner_right"));
        data.setPlateTempOuterLeft(rs.getDouble("plate_temp_outer_left"));
        data.setPlateTempOuterRight(rs.getDouble("plate_temp_outer_right"));

        return data;
    }

    // 辅助映射方法：利用反射批量设置，避免写几百行代码
    private void setGroundTemp(TemperatureData data, ResultSet rs, int probe, String side, int idx) {
        try {
            // DB column: ground_temp1_left3
            String colName = "ground_temp" + probe + "_" + side + idx;
            double val = rs.getDouble(colName);

            // Setter: setGroundTemp1Left3
            String sideCamel = side.substring(0, 1).toUpperCase() + side.substring(1); // "Left"
            String methodName = "setGroundTemp" + probe + sideCamel + idx;

            Method m = TemperatureData.class.getMethod(methodName, double.class);
            m.invoke(data, val);
        } catch (Exception e) {
            // 忽略字段不存在的错误，防止个别列名不匹配导致崩溃
        }
    }

    private void setLinearNonLinear(TemperatureData data, ResultSet rs, int probe, String side) {
        try {
            // Linear DB: linear_value_temp1left
            String linCol = "linear_value_temp" + probe + side;
            // Nonlinear DB: nonlinear_value_temp1left
            String nonLinCol = "nonlinear_value_temp" + probe + side;

            // Setters: setLinearValueTemp1left (注意 Model 定义中通常是小写 left)
            // 根据你的 TemperatureData.java，如果定义是 setLinearValueTemp1left，则直接用
            String methodSuffix = "Temp" + probe + side; // Temp1left

            Method mLin = TemperatureData.class.getMethod("setLinearValueTemp" + methodSuffix, double.class);
            mLin.invoke(data, rs.getDouble(linCol));

            Method mNon = TemperatureData.class.getMethod("setNonlinearValueTemp" + methodSuffix, double.class);
            mNon.invoke(data, rs.getDouble(nonLinCol));

        } catch (Exception e) { }
    }
}
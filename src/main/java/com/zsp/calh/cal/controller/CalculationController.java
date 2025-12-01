package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
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
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalculationController {
    // --- 原有筛选控件 ---
    @FXML private ComboBox<String> dateComboBox;
    @FXML private ComboBox<String> lineComboBox;
    @FXML private ComboBox<String> deviceComboBox;
    @FXML private ComboBox<String> probeComboBox;
    @FXML private ComboBox<String> sideComboBox;
    @FXML private TextArea resultTextArea;

    // --- 新增控件 (板温、当前板温、非线性筛选、轴位优选) ---
    @FXML private TextField plateTempField;
    @FXML private TextField currentPlateTempField;
    @FXML private ComboBox<Integer> nonlinearFilterComboBox;
    @FXML private ComboBox<String> axialPreferenceComboBox;

    private static final String TABLE_NAME = "temperature_data";

    // 标志位：防止联动监听器死循环
    private boolean isUpdating = false;

    @FXML
    public void initialize() {
        // 1. 初始化筛选联动逻辑
        setupComboBoxListeners();

        // 首次加载筛选数据
        refreshComboBoxOptions(null);

        // 2. 设置原有默认值
        if (probeComboBox.getItems().isEmpty()) {
            probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
        }
        if (sideComboBox.getItems().isEmpty()) {
            sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));
        }
        probeComboBox.getSelectionModel().select("地面探头1");
        sideComboBox.getSelectionModel().select("左");

        // 3. --- 初始化新增控件 ---

        // 初始化【非线性筛选】：空选项 + 整数 -20 到 20
        List<Integer> nonlinearOptions = new ArrayList<>();
        nonlinearOptions.add(null); // 【修改点】添加空选项（在 Integer 列表中用 null 表示）
        for (int i = -20; i <= 20; i++) {
            nonlinearOptions.add(i);
        }
        nonlinearFilterComboBox.setItems(FXCollections.observableArrayList(nonlinearOptions));
        nonlinearFilterComboBox.getSelectionModel().selectFirst(); // 【修改点】默认选中第一个（空选项）

        // 初始化【轴位优选】：空、3、4、5、6
        List<String> axialOptions = new ArrayList<>();
        axialOptions.add(""); // 空选项
        axialOptions.add("3");
        axialOptions.add("4");
        axialOptions.add("5");
        axialOptions.add("6");
        axialPreferenceComboBox.setItems(FXCollections.observableArrayList(axialOptions));
        axialPreferenceComboBox.getSelectionModel().selectFirst(); // 默认选中空选项
    }

    @FXML
    public void importExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));

        Stage stage = (Stage) resultTextArea.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                LoadController loadController = new LoadController();
                List<TemperatureData> data = loadController.readExcelToTemperatureData(selectedFile.getAbsolutePath());
                if (!data.isEmpty()) {
                    // 保存数据并获取统计结果
                    int insertedCount = loadController.saveTemperatureDataToSQLite(data, TABLE_NAME);
                    int ignoredCount = data.size() - insertedCount;

                    // 刷新下拉框联动数据
                    refreshComboBoxOptions(null);

                    String msg = String.format("读取: %d 条, 入库: %d 条, 忽略重复: %d 条", data.size(), insertedCount, ignoredCount);
                    showAlert(Alert.AlertType.INFORMATION, "导入成功", msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "错误", "导入失败: " + e.getMessage());
            }
        }
    }

    @FXML
    public void performCalculation() {
        String date = dateComboBox.getValue();
        String line = lineComboBox.getValue();
        String device = deviceComboBox.getValue();
        String probeStr = probeComboBox.getValue();
        String sideStr = sideComboBox.getValue();

        // 获取新参数
        String plateTempInput = plateTempField.getText();
        String currentPlateTempInput = currentPlateTempField.getText();
        Integer nonlinearFilterVal = nonlinearFilterComboBox.getValue(); // 如果选中空选项，此处为 null
        String axialPreferenceVal = axialPreferenceComboBox.getValue();

        if (probeStr == null || sideStr == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择探头和方位（用于确定计算参数）。");
            return;
        }

        // 解析参数
        int probeIndex = Integer.parseInt(probeStr.replace("地面探头", ""));
        int nextProbeIndex = (probeIndex < 4) ? probeIndex + 1 : -1;

        boolean isLeft = "左".equals(sideStr);
        String sideEn = isLeft ? "Left" : "Right";

        try {
            // 查询数据
            List<TemperatureData> dataList = executeQuery(date, line, device);
            if (dataList.isEmpty()) {
                resultTextArea.setText("未找到符合条件的数据。");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("=== 计算报告 (当前探头: %d, 下一探头: %s, 方位: %s) ===\n",
                    probeIndex, (nextProbeIndex == -1 ? "无" : nextProbeIndex), sideStr));
            result.append("公式: H = [(1+G%)/(1+D%)]*(A-C) + ...\n");
            result.append("筛选条件: -2.0 <= (H - Z) <= 0.1\n\n");

            int matchCount = 0;

            for (TemperatureData row : dataList) {
                // 1. 获取不需要遍历的参数 (C, D, E, F, G)
                double C = getDynamicValue(row, "getLinearValueTemp" + probeIndex + sideEn.toLowerCase());
                double D = getDynamicValue(row, "getNonlinearValueTemp" + probeIndex + sideEn.toLowerCase());

                // E: 板温 (取内侧)
                double E = isLeft ? row.getPlateTempInnerLeft() : row.getPlateTempInnerRight();

                // F, G: 下一个探头的线性/非线性
                double F = 0.0;
                double G = 0.0;
                if (nextProbeIndex != -1) {
                    F = getDynamicValue(row, "getLinearValueTemp" + nextProbeIndex + sideEn.toLowerCase());
                    G = getDynamicValue(row, "getNonlinearValueTemp" + nextProbeIndex + sideEn.toLowerCase());
                }

                // 2. 遍历该侧的 4 个位置 (3, 4, 5, 6)
                for (int i = 3; i <= 6; i++) {
                    // A: 地面温度
                    double A = getDynamicValue(row, "getGroundTemp" + probeIndex + sideEn + i);
                    // Z: 车上温度
                    double Z = getDynamicValue(row, "getCarTemp" + sideEn + i);

                    // 3. 计算 H
                    double H = calculateH(A, C, D, E, F, G);
                    double diff = H - Z;

                    // 4. 筛选 (-2.0 <= diff <= 0.1)
                    if (diff >= -2.0 && diff <= 0.1) {
                        matchCount++;
                        String dateDisplay = row.getDate() != null ? row.getDate().format(DateTimeFormatter.ofPattern("MM-dd")) : "N/A";
                        result.append(String.format("[匹配] 日期:%s 线:%s 设备:%s (位置%d)\n", dateDisplay, row.getLineNumber(), row.getDeviceName(), i));
                        result.append(String.format("  A=%.2f, Z=%.2f, E=%.2f | C=%.2f, D=%.2f, F=%.2f, G=%.2f\n", A, Z, E, C, D, F, G));
                        result.append(String.format("  结果 H=%.4f, 差值=%.4f\n", H, diff));
                        result.append("------------------------------------------\n");
                    }
                }
            }

            result.append("\n统计: 共筛选出 " + matchCount + " 个符合误差范围的数据点。");
            resultTextArea.setText(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "计算出错", e.getMessage());
        }
    }

    // --- 联动逻辑方法 ---

    private void setupComboBoxListeners() {
        dateComboBox.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdating) refreshComboBoxOptions("date"); });
        lineComboBox.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdating) refreshComboBoxOptions("line"); });
        deviceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> { if (!isUpdating) refreshComboBoxOptions("device"); });
    }

    private void refreshComboBoxOptions(String trigger) {
        isUpdating = true;
        try {
            String curDate = dateComboBox.getValue();
            String curLine = lineComboBox.getValue();
            String curDevice = deviceComboBox.getValue();

            // 根据当前选择，去数据库查另外两个框应该显示什么
            if (!"date".equals(trigger)) updateComboItems(dateComboBox, getLinkedDistinctValues("date", null, curLine, curDevice), curDate);
            if (!"line".equals(trigger)) updateComboItems(lineComboBox, getLinkedDistinctValues("line_number", curDate, null, curDevice), curLine);
            if (!"device".equals(trigger)) updateComboItems(deviceComboBox, getLinkedDistinctValues("device_name", curDate, curLine, null), curDevice);

            // 确保探头和方位有值
            if (probeComboBox.getItems().isEmpty()) probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
            if (sideComboBox.getItems().isEmpty()) sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            isUpdating = false;
        }
    }

    private void updateComboItems(ComboBox<String> combo, List<String> items, String currentVal) {
        // 1. 创建新列表
        List<String> displayItems = new ArrayList<>();
        // 2. 强制添加空选项
        displayItems.add("");
        // 3. 添加数据库数据
        if (items != null) displayItems.addAll(items);

        combo.setItems(FXCollections.observableArrayList(displayItems));

        // 4. 恢复选中或置空
        if (currentVal != null && items != null && items.contains(currentVal)) {
            combo.setValue(currentVal);
        } else {
            combo.setValue(""); // 否则重置为"空"
        }
    }

    private List<String> getLinkedDistinctValues(String targetCol, String date, String line, String device) throws SQLException {
        List<String> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT " + targetCol + " FROM " + TABLE_NAME + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 只有非空且非""时才添加过滤条件
        if (date != null && !date.isEmpty()) { sql.append(" AND date = ?"); params.add(date); }
        if (line != null && !line.isEmpty()) { sql.append(" AND line_number = ?"); params.add(line); }
        if (device != null && !device.isEmpty()) { sql.append(" AND device_name = ?"); params.add(device); }

        sql.append(" ORDER BY " + targetCol);

        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String val = rs.getString(1);
                    if (val != null && !val.isEmpty()) list.add(val);
                }
            }
        }
        return list;
    }

    // --- 核心计算与反射 ---

    private double calculateH(double A, double C, double D, double E, double F, double G) {
        double D_pct = D / 100.0;
        double G_pct = G / 100.0;
        if (1 + D_pct == 0) return 0.0;
        double term1 = (1 + G_pct) / (1 + D_pct);
        double term2 = ((1 + G_pct) * D_pct / (1 + D_pct)) - G_pct;
        return term1 * (A - C) + term2 * E + F;
    }

    private double getDynamicValue(TemperatureData data, String methodName) {
        try {
            Method method;
            try {
                method = TemperatureData.class.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                if (methodName.endsWith("Left")) {
                    method = TemperatureData.class.getMethod(methodName.replace("Left", "left"));
                } else if (methodName.endsWith("Right")) {
                    method = TemperatureData.class.getMethod(methodName.replace("Right", "right"));
                } else {
                    return 0.0;
                }
            }
            Object res = method.invoke(data);
            return res == null ? 0.0 : (Double) res;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // --- 数据库映射 ---

    private List<TemperatureData> executeQuery(String date, String line, String device) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_NAME + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (date != null && !date.isEmpty()) { sql.append(" AND date = ?"); params.add(date); }
        if (line != null && !line.isEmpty()) { sql.append(" AND line_number = ?"); params.add(line); }
        if (device != null && !device.isEmpty()) { sql.append(" AND device_name = ?"); params.add(device); }

        List<TemperatureData> list = new ArrayList<>();
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) { pstmt.setObject(i + 1, params.get(i)); }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTemperatureData(rs));
                }
            }
        }
        return list;
    }

    private TemperatureData mapResultSetToTemperatureData(ResultSet rs) throws SQLException {
        TemperatureData data = new TemperatureData();
        data.setLineNumber(rs.getString("line_number"));
        data.setDeviceName(rs.getString("device_name"));
        String dateStr = rs.getString("date");
        if(dateStr!=null) try { if(dateStr.startsWith("--")) dateStr=dateStr.substring(2); data.setDate(MonthDay.parse(dateStr, DateTimeFormatter.ofPattern("MM-dd"))); } catch(Exception e){}

        data.setCarTempLeft3(rs.getDouble("car_temp_left3")); data.setCarTempLeft4(rs.getDouble("car_temp_left4"));
        data.setCarTempLeft5(rs.getDouble("car_temp_left5")); data.setCarTempLeft6(rs.getDouble("car_temp_left6"));
        data.setCarTempRight3(rs.getDouble("car_temp_right3")); data.setCarTempRight4(rs.getDouble("car_temp_right4"));
        data.setCarTempRight5(rs.getDouble("car_temp_right5")); data.setCarTempRight6(rs.getDouble("car_temp_right6"));

        data.setPlateTempInnerLeft(rs.getDouble("plate_temp_inner_left"));
        data.setPlateTempInnerRight(rs.getDouble("plate_temp_inner_right"));

        for(int i=1; i<=4; i++) {
            String[] sides = {"left", "right"};
            for(String side : sides) {
                for(int k=3; k<=6; k++) {
                    String col = "ground_temp"+i+"_"+side+k;
                    String sideCamel = side.equals("left")?"Left":"Right";
                    callSetter(data, "setGroundTemp"+i+sideCamel+k, rs.getDouble(col));
                }
                callSetter(data, "setLinearValueTemp"+i+side, rs.getDouble("linear_value_temp"+i+side));
                callSetter(data, "setNonlinearValueTemp"+i+side, rs.getDouble("nonlinear_value_temp"+i+side));
            }
        }
        return data;
    }

    private void callSetter(Object obj, String methodName, double val) {
        try {
            Method m = obj.getClass().getMethod(methodName, double.class);
            m.invoke(obj, val);
        } catch(Exception e) {}
    }

    @FXML
    public void returnToQueryView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zsp/calh/cal/query-view.fxml"));
            Stage stage = (Stage) resultTextArea.getScene().getWindow();
            stage.setScene(new Scene(fxmlLoader.load(), 1100, 750));
            stage.setTitle("温度数据查询");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        new Alert(type, msg).showAndWait();
    }
}
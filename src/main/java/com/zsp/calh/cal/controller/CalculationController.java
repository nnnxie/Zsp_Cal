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
import java.sql.Statement;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalculationController {
    // 控件
    @FXML private ComboBox<String> dateComboBox;
    @FXML private ComboBox<String> lineComboBox;
    @FXML private ComboBox<String> deviceComboBox;
    @FXML private ComboBox<String> probeComboBox;
    @FXML private ComboBox<String> sideComboBox;
    @FXML private TextArea resultTextArea;

    private static final String TABLE_NAME = "temperature_data";

    @FXML
    public void initialize() {
        initializeComboBoxes();
        // 默认值
        probeComboBox.getSelectionModel().select("地面探头1");
        sideComboBox.getSelectionModel().select("左");
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
                    loadController.saveTemperatureDataToSQLite(data, TABLE_NAME);
                    initializeComboBoxes(); // 刷新下拉框
                    showAlert(Alert.AlertType.INFORMATION, "成功", "成功导入 " + data.size() + " 条数据。");
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

        if (probeStr == null || sideStr == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择探头和方位（用于确定计算参数）。");
            return;
        }

        // 解析参数
        // probeIndex: 当前探头 (A, C, D)
        int probeIndex = Integer.parseInt(probeStr.replace("地面探头", ""));
        // nextProbeIndex: 下一个探头 (F, G)，如果是探头4，则没有下一个，设为-1
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
                // C, D: 当前探头的线性/非线性 (注意字段名是小写的 left/right)
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
                    // A: 地面温度 (GroundTemp + probeIndex + sideEn + i)
                    double A = getDynamicValue(row, "getGroundTemp" + probeIndex + sideEn + i);
                    // Z: 车上温度 (CarTemp + sideEn + i)
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

    // 核心公式
    private double calculateH(double A, double C, double D, double E, double F, double G) {
        // 将 D 和 G 视为百分比 (例如 Excel 中是 5，代表 5%)
        double D_pct = D / 100.0;
        double G_pct = G / 100.0;

        if (1 + D_pct == 0) return 0.0;

        double term1 = (1 + G_pct) / (1 + D_pct);
        double term2 = ((1 + G_pct) * D_pct / (1 + D_pct)) - G_pct;

        return term1 * (A - C) + term2 * E + F;
    }

    // 动态取值
    private double getDynamicValue(TemperatureData data, String methodName) {
        try {
            Method method;
            try {
                method = TemperatureData.class.getMethod(methodName);
            } catch (NoSuchMethodException e) {
                // 容错: 处理 Temp1Left vs Temp1left 的大小写差异
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

    // --- 数据库操作 (需完全包含映射逻辑) ---

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
                    // 借用 QueryController 中的逻辑，这里需要一个完整的映射
                    // 为简化，建议您可以将 QueryController 中的 mapResultSetToTemperatureData 提取到 LoadController 或 Utils 类中复用
                    // 但为了文件独立性，这里必须包含关键数据的读取
                    list.add(mapResultSetToTemperatureData(rs));
                }
            }
        }
        return list;
    }

    // 这里必须保留完整的映射，尤其是计算需要的 C, D, E, F, G 相关字段
    private TemperatureData mapResultSetToTemperatureData(ResultSet rs) throws SQLException {
        TemperatureData data = new TemperatureData();
        data.setLineNumber(rs.getString("line_number"));
        data.setDeviceName(rs.getString("device_name"));
        String dateStr = rs.getString("date");
        if(dateStr!=null) try { if(dateStr.startsWith("--")) dateStr=dateStr.substring(2); data.setDate(MonthDay.parse(dateStr, DateTimeFormatter.ofPattern("MM-dd"))); } catch(Exception e){}

        // 映射车上温度
        data.setCarTempLeft3(rs.getDouble("car_temp_left3")); data.setCarTempLeft4(rs.getDouble("car_temp_left4"));
        data.setCarTempLeft5(rs.getDouble("car_temp_left5")); data.setCarTempLeft6(rs.getDouble("car_temp_left6"));
        data.setCarTempRight3(rs.getDouble("car_temp_right3")); data.setCarTempRight4(rs.getDouble("car_temp_right4"));
        data.setCarTempRight5(rs.getDouble("car_temp_right5")); data.setCarTempRight6(rs.getDouble("car_temp_right6"));

        // 映射板温
        data.setPlateTempInnerLeft(rs.getDouble("plate_temp_inner_left"));
        data.setPlateTempInnerRight(rs.getDouble("plate_temp_inner_right"));

        // 映射所有地面、线性、非线性 (通过循环)
        for(int i=1; i<=4; i++) {
            // Ground 3-6
            String[] sides = {"left", "right"};
            for(String side : sides) {
                for(int k=3; k<=6; k++) {
                    String col = "ground_temp"+i+"_"+side+k;
                    String sideCamel = side.equals("left")?"Left":"Right";
                    callSetter(data, "setGroundTemp"+i+sideCamel+k, rs.getDouble(col));
                }
                // Linear/Nonlinear
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

    private void initializeComboBoxes() {
        try {
            dateComboBox.setItems(FXCollections.observableArrayList(getDistinct("date")));
            lineComboBox.setItems(FXCollections.observableArrayList(getDistinct("line_number")));
            deviceComboBox.setItems(FXCollections.observableArrayList(getDistinct("device_name")));
            probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
            sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private List<String> getDistinct(String col) throws SQLException {
        List<String> list = new ArrayList<>();
        try(Statement s = DatabaseManager.getInstance().getConnection().createStatement(); ResultSet r=s.executeQuery("SELECT DISTINCT "+col+" FROM "+TABLE_NAME)) {
            while(r.next()) { String v=r.getString(1); if(v!=null && !v.isEmpty()) list.add(v); }
        }
        return list;
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
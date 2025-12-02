package com.zsp.calh.cal.controller;

import com.zsp.calh.cal.model.TemperatureData;
import com.zsp.calh.cal.utils.DatabaseManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CalculationController {
    // --- 筛选控件 ---
    @FXML private ComboBox<String> dateComboBox;
    @FXML private ComboBox<String> lineComboBox;
    @FXML private ComboBox<String> deviceComboBox;
    @FXML private ComboBox<String> probeComboBox;
    @FXML private ComboBox<String> sideComboBox;

    // --- 结果表格控件 ---
    @FXML private TableView<ResultModel> resultTableView;
    @FXML private TableColumn<ResultModel, String> colRowInfo;
    @FXML private TableColumn<ResultModel, Integer> colRank;
    @FXML private TableColumn<ResultModel, Double> colF;
    @FXML private TableColumn<ResultModel, Double> colG;
    @FXML private TableColumn<ResultModel, String> colStdDev;
    @FXML private TableColumn<ResultModel, String> colDiff3;
    @FXML private TableColumn<ResultModel, String> colDiff4;
    @FXML private TableColumn<ResultModel, String> colDiff5;
    @FXML private TableColumn<ResultModel, String> colDiff6;
    @FXML private TableColumn<ResultModel, String> colRemark;

    // --- 参数输入控件 ---
    @FXML private TextField plateTempField;
    @FXML private TextField currentPlateTempField;
    @FXML private ComboBox<Integer> nonlinearFilterComboBox;
    @FXML private ComboBox<String> axialPreferenceComboBox;

    private static final String TABLE_NAME = "temperature_data";
    private boolean isUpdating = false;

    @FXML
    public void initialize() {
        setupComboBoxListeners();
        refreshComboBoxOptions(null);
        initializeTableColumns();

        // 默认值
        if (probeComboBox.getItems().isEmpty()) probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
        if (sideComboBox.getItems().isEmpty()) sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));
        probeComboBox.getSelectionModel().select("地面探头1");
        sideComboBox.getSelectionModel().select("左");

        // 初始化 G
        List<Integer> nonlinearOptions = new ArrayList<>();
        nonlinearOptions.add(null);
        for (int i = -20; i <= 20; i++) nonlinearOptions.add(i);
        nonlinearFilterComboBox.setItems(FXCollections.observableArrayList(nonlinearOptions));
        nonlinearFilterComboBox.getSelectionModel().selectFirst();

        // 初始化轴位
        List<String> axialOptions = new ArrayList<>();
        axialOptions.add("");
        axialOptions.add("3");
        axialOptions.add("4");
        axialOptions.add("5");
        axialOptions.add("6");
        axialPreferenceComboBox.setItems(FXCollections.observableArrayList(axialOptions));
        axialPreferenceComboBox.getSelectionModel().selectFirst();
    }

    private void initializeTableColumns() {
        colRowInfo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRowInfo()));
        colRank.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getRank()).asObject());
        colF.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getF()).asObject());
        colG.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getG()).asObject());

        colStdDev.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.4f", cell.getValue().getStdDev())));
        colDiff3.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.3f", cell.getValue().getDiff3())));
        colDiff4.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.3f", cell.getValue().getDiff4())));
        colDiff5.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.3f", cell.getValue().getDiff5())));
        colDiff6.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.3f", cell.getValue().getDiff6())));
        colRemark.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRemark()));
    }

    @FXML
    public void importExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"));

        Stage stage = (Stage) resultTableView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                LoadController loadController = new LoadController();
                List<TemperatureData> data = loadController.readExcelToTemperatureData(selectedFile.getAbsolutePath());
                if (!data.isEmpty()) {
                    int insertedCount = loadController.saveTemperatureDataToSQLite(data, TABLE_NAME);
                    int ignoredCount = data.size() - insertedCount;
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
        // 1. 基础校验
        String date = dateComboBox.getValue();
        String line = lineComboBox.getValue();
        String device = deviceComboBox.getValue();
        String probeStr = probeComboBox.getValue();
        String sideStr = sideComboBox.getValue();

        if (probeStr == null || sideStr == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择探头和方位。");
            return;
        }

        // 2. 校验“二选一必填”逻辑
        Integer selectedG = nonlinearFilterComboBox.getValue();
        String selectedAxisStr = axialPreferenceComboBox.getValue();
        boolean isGSelected = (selectedG != null);
        boolean isAxisSelected = (selectedAxisStr != null && !selectedAxisStr.isEmpty());

        if (!isGSelected && !isAxisSelected) {
            showAlert(Alert.AlertType.WARNING, "参数错误", "【非线性筛选】和【轴位优选】必须至少选择一项！");
            return;
        }

        // 3. 获取板温 E
        double E;
        try {
            if (plateTempField.getText() == null || plateTempField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "提示", "请输入板温(E)。");
                return;
            }
            E = Double.parseDouble(plateTempField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "错误", "板温必须是有效数字。");
            return;
        }

        // 4. 确定遍历范围
        List<Double> gList = new ArrayList<>();
        if (isGSelected) {
            gList.add(selectedG.doubleValue());
        } else {
            for (int i = -20; i <= 20; i++) gList.add((double) i);
        }

        List<Double> fList = new ArrayList<>();
        for (int i = -120; i <= 120; i++) fList.add(i / 10.0);

        // 解析探头参数
        int probeIndex = Integer.parseInt(probeStr.replace("地面探头", ""));
        boolean isLeft = "左".equals(sideStr);
        String sideEn = isLeft ? "Left" : "Right";

        // 准备结果列表
        ObservableList<ResultModel> resultList = FXCollections.observableArrayList();

        try {
            List<TemperatureData> dataList = executeQuery(date, line, device);
            if (dataList.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "未找到符合筛选条件的数据行。");
                return;
            }

            int rowCount = 0;
            for (TemperatureData row : dataList) {
                rowCount++;
                String rowInfo = String.format("行%d [%s]", rowCount, row.getDeviceName());

                double C = getDynamicValue(row, "getLinearValueTemp" + probeIndex + sideEn.toLowerCase());
                double D = getDynamicValue(row, "getNonlinearValueTemp" + probeIndex + sideEn.toLowerCase());

                List<ResultModel> candidates = new ArrayList<>();

                // 全排列遍历 F 和 G
                for (double G : gList) {
                    for (double F : fList) {
                        // 计算该组合下 4个轴 的 H值 和 差值
                        double[] diffs = new double[7]; // 索引3-6有效
                        List<Double> diffList = new ArrayList<>();

                        // 目标轴的差值 (用于排序)，如果没有选轴位，则默认为0
                        double targetDiffForSort = 0.0;

                        for (int ax = 3; ax <= 6; ax++) {
                            double A = getDynamicValue(row, "getGroundTemp" + probeIndex + sideEn + ax);
                            double Z = getDynamicValue(row, "getCarTemp" + sideEn + ax);
                            double H = calculateH(A, C, D, E, F, G);
                            double diff = H - Z;

                            diffs[ax] = diff;
                            diffList.add(diff);
                        }

                        // 【修改点】没有任何条件限制，直接计算并加入候选列表
                        // 准备排序所需的数据
                        if (isAxisSelected) {
                            int targetAxis = Integer.parseInt(selectedAxisStr);
                            targetDiffForSort = diffs[targetAxis];
                        }

                        // 计算标准差
                        double stdDev = calculateStdDev(diffList);

                        ResultModel model = new ResultModel();
                        model.setRowInfo(rowInfo);
                        model.setF(F);
                        model.setG(G);
                        model.setStdDev(stdDev);
                        model.setDiff3(diffs[3]);
                        model.setDiff4(diffs[4]);
                        model.setDiff5(diffs[5]);
                        model.setDiff6(diffs[6]);
                        model.setTargetDiff(targetDiffForSort);

                        // 备注信息
                        if (isAxisSelected) model.setRemark("优选轴" + selectedAxisStr);
                        else model.setRemark("全轴优");

                        candidates.add(model);
                    }
                }

                // --- 排序逻辑 ---
                // 第一优先级：标准差 StdDev (ASC)
                Comparator<ResultModel> comparator = Comparator.comparingDouble(ResultModel::getStdDev);

                if (isAxisSelected) {
                    // 轴位优选模式：第二优先级 -> 目标轴差值的绝对值
                    comparator = comparator.thenComparingDouble(r -> Math.abs(r.getTargetDiff()));
                } else {
                    // 非线性筛选模式：第二优先级 -> 分数 (均值绝对值 + 标准差)
                    comparator = comparator.thenComparingDouble(r -> {
                        double mean = (r.getDiff3() + r.getDiff4() + r.getDiff5() + r.getDiff6()) / 4.0;
                        return Math.abs(mean) + r.getStdDev();
                    });
                }

                List<ResultModel> top5 = candidates.stream()
                        .sorted(comparator)
                        .limit(5)
                        .collect(Collectors.toList());

                // 设置排名并添加到总列表
                for (int i = 0; i < top5.size(); i++) {
                    top5.get(i).setRank(i + 1);
                    resultList.add(top5.get(i));
                }
            }

            resultTableView.setItems(resultList);

            if (resultList.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "结果", "计算完成，无结果。");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "计算出错", e.getMessage());
        }
    }

    // --- 数据模型类 (用于 TableView) ---
    public static class ResultModel {
        private String rowInfo;
        private int rank;
        private double F;
        private double G;
        private double stdDev;
        private double diff3;
        private double diff4;
        private double diff5;
        private double diff6;
        private double targetDiff; // 内部排序用
        private String remark;

        // Getters and Setters
        public String getRowInfo() { return rowInfo; }
        public void setRowInfo(String rowInfo) { this.rowInfo = rowInfo; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public double getF() { return F; }
        public void setF(double f) { F = f; }
        public double getG() { return G; }
        public void setG(double g) { G = g; }
        public double getStdDev() { return stdDev; }
        public void setStdDev(double stdDev) { this.stdDev = stdDev; }
        public double getDiff3() { return diff3; }
        public void setDiff3(double diff3) { this.diff3 = diff3; }
        public double getDiff4() { return diff4; }
        public void setDiff4(double diff4) { this.diff4 = diff4; }
        public double getDiff5() { return diff5; }
        public void setDiff5(double diff5) { this.diff5 = diff5; }
        public double getDiff6() { return diff6; }
        public void setDiff6(double diff6) { this.diff6 = diff6; }
        public double getTargetDiff() { return targetDiff; }
        public void setTargetDiff(double targetDiff) { this.targetDiff = targetDiff; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    // --- 数学公式 ---
    private double calculateH(double A, double C, double D, double E, double F, double G) {
        double D_pct = D / 100.0;
        double G_pct = G / 100.0;
        if (1 + D_pct == 0) return 0.0;
        double term1 = (1 + G_pct) / (1 + D_pct);
        double term2 = ((1 + G_pct) * D_pct / (1 + D_pct)) - G_pct;
        return term1 * (A - C) + term2 * E + F;
    }

    private double calculateStdDev(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double sumSq = 0.0;
        for (double v : values) sumSq += Math.pow(v - mean, 2);
        return Math.sqrt(sumSq / values.size());
    }

    // --- 反射与工具 ---
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

    // --- 联动逻辑 ---
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

            if (!"date".equals(trigger)) updateComboItems(dateComboBox, getLinkedDistinctValues("date", null, curLine, curDevice), curDate);
            if (!"line".equals(trigger)) updateComboItems(lineComboBox, getLinkedDistinctValues("line_number", curDate, null, curDevice), curLine);
            if (!"device".equals(trigger)) updateComboItems(deviceComboBox, getLinkedDistinctValues("device_name", curDate, curLine, null), curDevice);

            if (probeComboBox.getItems().isEmpty()) probeComboBox.setItems(FXCollections.observableArrayList("地面探头1", "地面探头2", "地面探头3", "地面探头4"));
            if (sideComboBox.getItems().isEmpty()) sideComboBox.setItems(FXCollections.observableArrayList("左", "右"));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            isUpdating = false;
        }
    }

    private void updateComboItems(ComboBox<String> combo, List<String> items, String currentVal) {
        List<String> displayItems = new ArrayList<>();
        displayItems.add("");
        if (items != null) displayItems.addAll(items);
        combo.setItems(FXCollections.observableArrayList(displayItems));
        if (currentVal != null && items != null && items.contains(currentVal)) {
            combo.setValue(currentVal);
        } else {
            combo.setValue("");
        }
    }

    private List<String> getLinkedDistinctValues(String targetCol, String date, String line, String device) throws SQLException {
        List<String> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT " + targetCol + " FROM " + TABLE_NAME + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
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
                while (rs.next()) list.add(mapResultSetToTemperatureData(rs));
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
            Stage stage = (Stage) resultTableView.getScene().getWindow();
            stage.setScene(new Scene(fxmlLoader.load(), 1100, 750));
            stage.setTitle("温度数据查询");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        new Alert(type, msg).showAndWait();
    }
}
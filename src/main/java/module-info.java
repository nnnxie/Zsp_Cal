module com.zsp.calh.cal {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.ooxml;

    opens com.zsp.calh.cal to javafx.fxml;
    exports com.zsp.calh.cal;
    exports com.zsp.calh.cal.controller;
    exports com.zsp.calh.cal.model;
    opens com.zsp.calh.cal.controller to javafx.fxml;
}
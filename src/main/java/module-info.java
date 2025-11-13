module com.zsp.calh.cal {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.ooxml;
    requires static lombok;
    requires java.sql;

    opens com.zsp.calh.cal to javafx.fxml;
    exports com.zsp.calh.cal;
    exports com.zsp.calh.cal.controller;
    exports com.zsp.calh.cal.model;
    exports com.zsp.calh.cal.utils;
    opens com.zsp.calh.cal.controller to javafx.fxml;
    opens com.zsp.calh.cal.utils to javafx.fxml;
}
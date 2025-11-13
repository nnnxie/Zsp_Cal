package com.zsp.calh.cal.model;

// 首先修改导入语句
import lombok.Data;
import java.time.MonthDay;



@Data
public class TemperatureData {
    private Long id;              // 唯一标识符
    private String lineNumber;    // 线别
    private String deviceName;    // 设备名称
    private MonthDay date;        // 日期（只包含月和日）

    // 车上测量温度
    private double carTempLeft3;    // 车上测量温度左3
    private double carTempLeft4;    // 车上测量温度左4
    private double carTempLeft5;    // 车上测量温度左5
    private double carTempLeft6;    // 车上测量温度左6
    private double carTempRight3;   // 车上测量温度右3
    private double carTempRight4;   // 车上测量温度右4
    private double carTempRight5;   // 车上测量温度右5
    private double carTempRight6;   // 车上测量温度右6

    // 地面探测温度1
    private double groundTemp1Left3;    // 地面探测温度1左3
    private double groundTemp1Left4;    // 地面探测温度1左4
    private double groundTemp1Left5;    // 地面探测温度1左5
    private double groundTemp1Left6;    // 地面探测温度1左6
    private double groundTemp1Right3;   // 地面探测温度1右3
    private double groundTemp1Right4;   // 地面探测温度1右4
    private double groundTemp1Right5;   // 地面探测温度1右5
    private double groundTemp1Right6;   // 地面探测温度1右6

    // 地面探测温度2（内探）
    private double groundTemp2Left3;    // 地面探测温度2左3（内探）
    private double groundTemp2Left4;    // 地面探测温度2左4（内探）
    private double groundTemp2Left5;    // 地面探测温度2左5（内探）
    private double groundTemp2Left6;    // 地面探测温度2左6（内探）
    private double groundTemp2Right3;   // 地面探测温度2右3（内探）
    private double groundTemp2Right4;   // 地面探测温度2右4（内探）
    private double groundTemp2Right5;   // 地面探测温度2右5（内探）
    private double groundTemp2Right6;   // 地面探测温度2右6（内探）

    // 地面探测温度3
    private double groundTemp3Left3;    // 地面探测温度3左3
    private double groundTemp3Left4;    // 地面探测温度3左4
    private double groundTemp3Left5;    // 地面探测温度3左5
    private double groundTemp3Left6;    // 地面探测温度3左6
    private double groundTemp3Right3;   // 地面探测温度3右3
    private double groundTemp3Right4;   // 地面探测温度3右4// 地面探测温度3左6
    private double groundTemp3Right5;   // 地面探测温度3右3
    private double groundTemp3Right6;   // 地面探测温度3右4

    // 地面探测温度4（外探）
    private double groundTemp4Left3;    // 地面探测温度4左3（外探）
    private double groundTemp4Left4;    // 地面探测温度4左4（外探）
    private double groundTemp4Left5;    // 地面探测温度4左5（外探）
    private double groundTemp4Left6;    // 地面探测温度4左6（外探）
    private double groundTemp4Right3;   // 地面探测温度4右3（外探）
    private double groundTemp4Right4;   // 地面探测温度4右4（外探）
    private double groundTemp4Right5;   // 地面探测温度4右5（外探）
    private double groundTemp4Right6;   // 地面探测温度4右6（外探）

    // 线性
    private double linearValueTemp1left;// 线性探1左
    private double linearValueTemp1right;// 线性探1右
    private double linearValueTemp2left;// 线性探2左
    private double linearValueTemp2right;// 线性探2右
    private double linearValueTemp3left;// 线性探3左
    private double linearValueTemp3right;// 线性探3右
    private double linearValueTemp4left;// 线性探4左
    private double linearValueTemp4right;// 线性探4右

    // 非线性
    private double nonlinearValueTemp1left;// 非线性探1左
    private double nonlinearValueTemp1right;// 非线性探1右
    private double nonlinearValueTemp2left;// 非线性探2左
    private double nonlinearValueTemp2right;// 非线性探2右
    private double nonlinearValueTemp3left;// 非线性探3左
    private double nonlinearValueTemp3right;// 非线性探3右
    private double nonlinearValueTemp4left;// 非线性探4左
    private double nonlinearValueTemp4right;// 非线性探4右

    // 板温字段
    private double plateTempInnerLeft;  // 板温内左
    private double plateTempInnerRight; // 板温内右
    private double plateTempOuterLeft;  // 板温外左
    private double plateTempOuterRight; // 板温外右
}
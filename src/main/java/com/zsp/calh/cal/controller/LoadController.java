package com.zsp.calh.cal.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoadController {
    
    /**
     * 从Excel文件中按行读取数据
     * @param filePath Excel文件路径
     * @return 包含所有行数据的列表，每行数据是一个字符串列表
     */
    public List<List<String>> readExcelFile(String filePath) {
        List<List<String>> data = new ArrayList<>();
        
        try (FileInputStream file = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(file)) {
            
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            
            // 迭代行
            Iterator<Row> rowIterator = sheet.iterator();
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                List<String> rowData = new ArrayList<>();
                
                // 迭代单元格
                Iterator<Cell> cellIterator = row.cellIterator();
                
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    
                    // 根据单元格类型获取值
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue().toString());
                            } else {
                                rowData.add(String.valueOf(cell.getNumericCellValue()));
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(String.valueOf(cell.getBooleanCellValue()));
                            break;
                        case BLANK:
                            rowData.add("");
                            break;
                        default:
                            rowData.add(cell.toString());
                    }
                }
                
                data.add(rowData);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("读取Excel文件出错: " + e.getMessage());
        }
        
        return data;
    }
    
    /**
     * 测试方法，打印Excel文件内容
     * @param filePath Excel文件路径
     */
    public void printExcelContent(String filePath) {
        List<List<String>> data = readExcelFile(filePath);
        
        System.out.println("Excel文件内容：");
        for (List<String> row : data) {
            System.out.println(String.join(", ", row));
        }
    }
}
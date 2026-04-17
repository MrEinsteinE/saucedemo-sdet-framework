package com.sdet.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelDataReader — reads test data from .xlsx files for data-driven testing.
 *
 * Usage:
 *   Object[][] data = ExcelDataReader.getTableArray("src/test/resources/testdata/login_data.xlsx", "ValidLogin");
 *
 * Sheet format: first row = column headers, subsequent rows = data rows.
 */
public class ExcelDataReader {

    /**
     * Returns a 2D Object array suitable for TestNG @DataProvider.
     * Each row is a Map<String, String> of {header -> cellValue}.
     */
    public static Object[][] getTableArray(String filePath, String sheetName) throws IOException {
        List<Map<String, String>> rows = readSheet(filePath, sheetName);
        Object[][] result = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            result[i][0] = rows.get(i);
        }
        return result;
    }

    /**
     * Reads all rows from a sheet, returning List of column→value maps.
     */
    public static List<Map<String, String>> readSheet(String filePath, String sheetName) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in " + filePath);
            }

            Row headerRow = sheet.getRow(0);
            int colCount = headerRow.getLastCellNum();

            // Build header list
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < colCount; c++) {
                headers.add(getCellValue(headerRow.getCell(c)));
            }

            // Build data rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < colCount; c++) {
                    rowMap.put(headers.get(c), getCellValue(row.getCell(c)));
                }
                data.add(rowMap);
            }
        }
        return data;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell)
                        ? cell.getLocalDateTimeCellValue().toString()
                        : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCachedFormulaResultType() == CellType.STRING
                        ? cell.getStringCellValue()
                        : String.valueOf(cell.getNumericCellValue());
            default:
                return "";
        }
    }
}

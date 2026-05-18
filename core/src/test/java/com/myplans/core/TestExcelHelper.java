package com.myplans.core;

import java.io.ByteArrayOutputStream;

public final class TestExcelHelper {

    private TestExcelHelper() {
    }

    public static byte[] xlsxWithOneTag(String codigo, String tipo) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("codigo");
            header.createCell(1).setCellValue("tipo");
            header.createCell(2).setCellValue("descripcion");
            header.createCell(3).setCellValue("area");

            org.apache.poi.ss.usermodel.Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(codigo);
            data.createCell(1).setCellValue(tipo);
            data.createCell(2).setCellValue("Motor X");
            data.createCell(3).setCellValue("Sala A");

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
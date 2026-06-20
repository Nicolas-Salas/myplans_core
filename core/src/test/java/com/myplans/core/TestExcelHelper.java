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
            header.createCell(0).setCellValue("ELEMENTO");
            header.createCell(1).setCellValue("TIPO");

            org.apache.poi.ss.usermodel.Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(codigo);
            data.createCell(1).setCellValue(tipo);

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] xlsxWithWrongHeaders() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("wrong_col_a");
            header.createCell(1).setCellValue("wrong_col_b");
            header.createCell(2).setCellValue("wrong_col_c");

            org.apache.poi.ss.usermodel.Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("val1");
            data.createCell(1).setCellValue("val2");
            data.createCell(2).setCellValue("val3");

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] xlsxWithMissingTipoColumn() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("codigo");
            header.createCell(1).setCellValue("descripcion");
            header.createCell(2).setCellValue("area");

            org.apache.poi.ss.usermodel.Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("TAG-001");
            data.createCell(1).setCellValue("Motor bomba");
            data.createCell(2).setCellValue("Sala B");

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] xlsxWithHeaderOnly() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("codigo");
            header.createCell(1).setCellValue("tipo");
            header.createCell(2).setCellValue("descripcion");
            header.createCell(3).setCellValue("area");

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
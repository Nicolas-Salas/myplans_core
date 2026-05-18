package com.myplans.core.util;

import com.myplans.core.entity.Plano;
import com.myplans.core.entity.Tag;
import com.myplans.core.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ExcelExporterComponent {

    public byte[] exportTagsMatrix(Plano plano, List<Tag> tags) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Matriz TAGs");

            // --- Encabezado del plano (filas 0-2) ---
            CellStyle titleStyle = boldStyle(workbook);
            Row titleRow = sheet.createRow(0);
            createCell(titleRow, 0, "Plano:", titleStyle);
            createCell(titleRow, 1, plano.getNombre());

            Row codeRow = sheet.createRow(1);
            createCell(codeRow, 0, "Código:", titleStyle);
            createCell(codeRow, 1, plano.getCodigoPlano());
            createCell(codeRow, 2, "Revisión:", titleStyle);
            createCell(codeRow, 3, plano.getRev());

            // --- Encabezado de la matriz (fila 4) ---
            String[] headers = {
                    "ID TAG", "Código", "Descripción", "Área", "Tipo",
                    "Estado", "Comentario", "Usuario Ingreso", "Fecha Ingreso",
                    "Usuario Actualización", "Última Modificación"
            };
            Row header = sheet.createRow(4);
            for (int i = 0; i < headers.length; i++) {
                createCell(header, i, headers[i], titleStyle);
            }

            // --- Filas de TAGs ---
            int rowIdx = 5;
            for (Tag tag : tags) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, tag.getIdTag() == null ? "" : tag.getIdTag().toString());
                createCell(row, 1, tag.getCodigo());
                createCell(row, 2, tag.getDescripcion());
                createCell(row, 3, tag.getArea());
                createCell(row, 4, tag.getTipo() == null ? "" : tag.getTipo().name());
                createCell(row, 5, tag.getEstadoActual() == null ? "" : tag.getEstadoActual().name());
                createCell(row, 6, tag.getComentario());
                createCell(row, 7, tag.getIdUsuarioIngreso() == null ? "" : tag.getIdUsuarioIngreso().toString());
                createCell(row, 8, tag.getFechaIngreso() == null ? "" : tag.getFechaIngreso().toString());
                createCell(row, 9, tag.getIdUsuarioActualizacion() == null ? "" : tag.getIdUsuarioActualizacion().toString());
                createCell(row, 10, tag.getUltimaModificacion() == null ? "" : tag.getUltimaModificacion().toString());
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Error al generar el Excel: " + e.getMessage());
        }
    }

    private CellStyle boldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void createCell(Row row, int col, String value) {
        createCell(row, col, value, null);
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}
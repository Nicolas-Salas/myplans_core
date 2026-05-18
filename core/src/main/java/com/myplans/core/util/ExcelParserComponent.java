package com.myplans.core.util;

import com.myplans.core.dto.TagExcelRowDTO;
import com.myplans.core.exception.BusinessException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Cell;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class ExcelParserComponent {

    public static final String EXCEL_TYPE_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String OCTET_STREAM = "application/octet-stream";

    public boolean hasExcelFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        if (EXCEL_TYPE_XLSX.equals(contentType)) {
            return true;
        }
        return name != null && name.toLowerCase().endsWith(".xlsx");
    }

    public List<TagExcelRowDTO> parseTagsExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException("El archivo Excel no contiene hojas");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            List<TagExcelRowDTO> tags = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            if (!rows.hasNext()) {
                throw new BusinessException("El archivo Excel está vacío");
            }

            // Leer encabezados y encontrar columnas por nombre
            Row headerRow = rows.next();
            int colElemento = -1;
            int colTipo = -1;
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim().toUpperCase();
                if ("ELEMENTO".equals(header)) colElemento = cell.getColumnIndex();
                else if ("TIPO".equals(header)) colTipo = cell.getColumnIndex();
            }
            if (colElemento == -1) {
                throw new BusinessException(
                        "No se encontró la columna 'ELEMENTO' en el encabezado del Excel");
            }
            if (colTipo == -1) {
                throw new BusinessException(
                        "No se encontró la columna 'TIPO' en el encabezado del Excel");
            }

            int rowNumber = 1;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                rowNumber++;

                String codigo = formatter.formatCellValue(currentRow.getCell(colElemento)).trim();
                String tipo = formatter.formatCellValue(currentRow.getCell(colTipo)).trim();

                if (codigo.isEmpty() && tipo.isEmpty()) {
                    continue;
                }

                if (codigo.isEmpty()) {
                    throw new BusinessException(
                            "Fila " + rowNumber + ": la columna 'ELEMENTO' es obligatoria");
                }
                if (tipo.isEmpty()) {
                    throw new BusinessException(
                            "Fila " + rowNumber + ": la columna 'TIPO' es obligatoria");
                }

                tags.add(new TagExcelRowDTO(codigo, tipo));
            }

            return tags;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(
                    "Error al procesar el archivo Excel: " + e.getMessage());
        }
    }
}
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

            // Saltar fila de encabezado
            if (rows.hasNext()) {
                rows.next();
            }

            int rowNumber = 1;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                rowNumber++;

                String codigo = formatter.formatCellValue(currentRow.getCell(0)).trim();
                String tipo = formatter.formatCellValue(currentRow.getCell(1)).trim();
                String descripcion = formatter.formatCellValue(currentRow.getCell(2)).trim();
                String area = formatter.formatCellValue(currentRow.getCell(3)).trim();

                if (codigo.isEmpty() && tipo.isEmpty() && descripcion.isEmpty() && area.isEmpty()) {
                    continue;
                }

                if (codigo.isEmpty()) {
                    throw new BusinessException(
                            "Fila " + rowNumber + ": el campo 'codigo' es obligatorio");
                }
                if (tipo.isEmpty()) {
                    throw new BusinessException(
                            "Fila " + rowNumber + ": el campo 'tipo' es obligatorio");
                }

                tags.add(new TagExcelRowDTO(codigo, tipo, descripcion, area));
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
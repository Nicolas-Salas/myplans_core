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

    public static final String EXCEL_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public boolean hasExcelFormat(MultipartFile file) {
        return EXCEL_TYPE.equals(file.getContentType());
    }

    public List<TagExcelRowDTO> parseTagsExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            List<TagExcelRowDTO> tags = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                String codigo = formatter.formatCellValue(currentRow.getCell(0)).trim();
                String tipo = formatter.formatCellValue(currentRow.getCell(1)).trim();
                String descripcion = formatter.formatCellValue(currentRow.getCell(2)).trim();
                String area = formatter.formatCellValue(currentRow.getCell(3)).trim();

                if (codigo.isEmpty()) {
                    break;
                }

                TagExcelRowDTO tagRow = new TagExcelRowDTO(codigo, tipo, descripcion, area);
                tags.add(tagRow);
            }

            return tags;
        } catch (Exception e) {
            throw new BusinessException("Error al procesar el archivo Excel. Verifique el formato.");
        }
    }
}
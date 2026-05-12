package com.myplans.core.service;

import com.myplans.core.dto.TagExcelRowDTO;
import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.entity.Plano;
import com.myplans.core.entity.Tag;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;
import com.myplans.core.exception.BusinessException;
import com.myplans.core.mapper.TagMapper;
import com.myplans.core.repository.PlanoRepository;
import com.myplans.core.repository.TagRepository;
import com.myplans.core.util.ExcelParserComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final PlanoRepository planoRepository;
    private final ExcelParserComponent excelParserComponent;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public List<TagResponseDTO> uploadTagsFromExcel(Integer idPlano, MultipartFile file, Integer idUsuarioIngreso) {
        if (!excelParserComponent.hasExcelFormat(file)) {
            throw new BusinessException("El archivo debe ser un formato Excel (.xlsx) válido.");
        }

        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new BusinessException("Plano no encontrado."));

        if (plano.getEstado() != PlanoEstado.PENDIENTE) {
            throw new BusinessException("Solo se pueden cargar TAGs en planos con estado PENDIENTE.");
        }

        List<TagExcelRowDTO> parsedRows = excelParserComponent.parseTagsExcel(file);
        
        if (parsedRows.isEmpty()) {
            throw new BusinessException("El archivo Excel está vacío o no tiene el formato correcto.");
        }

        Set<String> uniqueCodes = new HashSet<>();
        List<Tag> tagsToSave = new ArrayList<>();

        for (TagExcelRowDTO row : parsedRows) {
            if (!uniqueCodes.add(row.codigo())) {
                throw new BusinessException("El archivo Excel contiene códigos de TAG duplicados: " + row.codigo());
            }

            if (tagRepository.existsByPlanoAndCodigo(plano, row.codigo())) {
                throw new BusinessException("El TAG con código " + row.codigo() + " ya existe en este plano.");
            }

            TagTipo tipoTag;
            try {
                tipoTag = TagTipo.valueOf(row.tipo().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Tipo de TAG inválido (" + row.tipo() + ") en el código: " + row.codigo());
            }

            Tag tag = Tag.builder()
                    .plano(plano)
                    .codigo(row.codigo())
                    .tipo(tipoTag)
                    .descripcion(row.descripcion())
                    .area(row.area())
                    .estadoActual(TagEstado.PENDIENTE)
                    .idUsuarioIngreso(idUsuarioIngreso)
                    .build();

            tagsToSave.add(tag);
        }

        List<Tag> savedTags = tagRepository.saveAll(tagsToSave);
        return tagMapper.toResponseList(savedTags);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDTO> getTagsByPlano(Integer idPlano) {
        if (!planoRepository.existsById(idPlano)) {
            throw new BusinessException("Plano no encontrado.");
        }
        List<Tag> tags = tagRepository.findByPlanoIdPlano(idPlano);
        return tagMapper.toResponseList(tags);
    }
}
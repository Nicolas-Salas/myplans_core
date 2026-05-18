package com.myplans.core.service;

import com.myplans.core.audit.AuditEvent;
import com.myplans.core.audit.AuditServiceClient;
import com.myplans.core.dto.TagEstadoUpdateDTO;
import com.myplans.core.dto.TagExcelRowDTO;
import com.myplans.core.dto.TagResponseDTO;
import com.myplans.core.entity.Plano;
import com.myplans.core.entity.Tag;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.entity.enums.TagEstado;
import com.myplans.core.entity.enums.TagTipo;
import com.myplans.core.exception.BusinessException;
import com.myplans.core.exception.ConflictException;
import com.myplans.core.exception.ForbiddenOperationException;
import com.myplans.core.exception.ResourceNotFoundException;
import com.myplans.core.mapper.TagMapper;
import com.myplans.core.repository.PlanoRepository;
import com.myplans.core.repository.TagRepository;
import com.myplans.core.security.AuthenticatedUser;
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
    private final ExcelParserComponent excelParser;
    private final TagMapper tagMapper;
    private final AuditServiceClient auditServiceClient;

    @Override
    @Transactional
    public List<TagResponseDTO> uploadTagsFromExcel(Integer idPlano, MultipartFile file,
                                                    AuthenticatedUser user) {
        if (!excelParser.hasExcelFormat(file)) {
            throw new BusinessException("El archivo debe ser un Excel (.xlsx) válido");
        }
        if (user == null || user.getIdUsuario() == null) {
            throw new BusinessException(
                    "No se pudo identificar al usuario que sube el archivo. " +
                    "Verifica que tu sesión esté activa");
        }

        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plano no encontrado con ID: " + idPlano));

        if (plano.getStatus() != PlanoEstado.ABIERTO) {
            throw new ConflictException(
                    "Solo se pueden cargar TAGs en planos ABIERTOS. " +
                    "Estado actual: " + plano.getStatus());
        }

        List<TagExcelRowDTO> rows = excelParser.parseTagsExcel(file);
        if (rows.isEmpty()) {
            throw new BusinessException("El archivo Excel no contiene filas con TAGs");
        }

        Set<String> uniqueCodes = new HashSet<>();
        List<Tag> toSave = new ArrayList<>();

        for (TagExcelRowDTO row : rows) {
            if (!uniqueCodes.add(row.codigo())) {
                throw new ConflictException(
                        "El archivo contiene códigos de TAG duplicados: " + row.codigo());
            }
            if (tagRepository.existsByPlanoAndCodigo(plano, row.codigo())) {
                throw new ConflictException(
                        "El TAG '" + row.codigo() + "' ya existe en este plano");
            }

            TagTipo tipo;
            try {
                tipo = TagTipo.valueOf(row.tipo().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        "Tipo de TAG inválido ('" + row.tipo() + "') en el código: " +
                        row.codigo() + ". Valores aceptados: EQUIPO, CABLE, NULO");
            }

            Tag tag = Tag.builder()
                    .plano(plano)
                    .codigo(row.codigo())
                    .tipo(tipo)
                    .area(plano.getSubsistema())
                    .estadoActual(TagEstado.PENDIENTE)
                    .idUsuarioIngreso(user.getIdUsuario())
                    .build();

            toSave.add(tag);
        }

        List<Tag> saved = tagRepository.saveAll(toSave);
        return tagMapper.toResponseList(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDTO> getTagsByPlano(Integer idPlano) {
        if (!planoRepository.existsById(idPlano)) {
            throw new ResourceNotFoundException("Plano no encontrado con ID: " + idPlano);
        }
        return tagMapper.toResponseList(tagRepository.findByPlanoIdPlanoOrderByCodigoAsc(idPlano));
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponseDTO getTagById(Integer idTag) {
        Tag tag = tagRepository.findById(idTag)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TAG no encontrado con ID: " + idTag));
        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public TagResponseDTO updateEstado(Integer idTag, TagEstadoUpdateDTO request,
                                       AuthenticatedUser user) {
        if (user == null || user.getIdUsuario() == null) {
            throw new BusinessException(
                    "No se pudo identificar al usuario. Verifica tu sesión");
        }

        Tag tag = tagRepository.findById(idTag)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TAG no encontrado con ID: " + idTag));

        Plano plano = tag.getPlano();
        if (plano.getStatus() == PlanoEstado.CERRADO) {
            throw new ConflictException(
                    "No se pueden modificar TAGs de un plano CERRADO");
        }

        TagEstado nuevo = request.estadoNuevo();
        TagEstado anterior = tag.getEstadoActual();

        if (nuevo == anterior
                && (request.comentario() == null
                    || request.comentario().equals(tag.getComentario()))) {
            return tagMapper.toResponse(tag);
        }

        if (nuevo == TagEstado.OBSERVADO) {
            String comentario = request.comentario();
            if (comentario == null || comentario.isBlank()) {
                throw new BusinessException(
                        "El comentario es obligatorio cuando el TAG queda OBSERVADO");
            }
        }

        if (anterior == TagEstado.APROBADO && nuevo != TagEstado.APROBADO) {
            if (!user.hasRole("AUDITOR") && !user.hasRole("ADMIN")) {
                throw new ForbiddenOperationException(
                        "Solo un Supervisor puede revertir un TAG aprobado");
            }
        }

        tag.setEstadoActual(nuevo);
        if (request.comentario() != null) {
            tag.setComentario(request.comentario());
        }
        tag.setIdUsuarioActualizacion(user.getIdUsuario());

        Tag saved = tagRepository.save(tag);

        AuditEvent event = new AuditEvent(
                saved.getIdTag(),
                user.getIdUsuario(),
                anterior == null ? null : anterior.name(),
                nuevo.name(),
                request.comentario());
        auditServiceClient.publish(event);

        return tagMapper.toResponse(saved);
    }
}
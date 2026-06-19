package com.myplans.core.service;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.dto.PlanoUpdateDTO;
import com.myplans.core.entity.Plano;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.exception.BusinessException;
import com.myplans.core.exception.ConflictException;
import com.myplans.core.exception.NoFieldsToUpdateException;
import com.myplans.core.exception.ResourceNotFoundException;
import com.myplans.core.mapper.PlanoMapper;
import com.myplans.core.repository.PlanoRepository;

import com.myplans.core.security.AuthenticatedUser;
import com.myplans.core.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoServiceImpl implements PlanoService {

    private static final String STORAGE_FOLDER_PLANOS = "planos";

    private final PlanoRepository planoRepository;
    private final PlanoMapper planoMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public PlanoResponseDTO createPlano(PlanoRequestDTO request, AuthenticatedUser user) {
        Plano plano = planoMapper.toEntity(request);
        plano.setStatus(PlanoEstado.ABIERTO);
        plano.setNroPaginas(0);
        plano = planoRepository.save(plano);
        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanoResponseDTO getPlanoById(Integer idPlano) {
        return planoMapper.toResponse(findPlanoOrFail(idPlano));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<PlanoResponseDTO> getAllPlanos(PlanoEstado status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Plano> planoPage = (status == null)
                ? planoRepository.findAll(pageable)
                : planoRepository.findByStatus(status, pageable);

        List<PlanoResponseDTO> content = planoMapper.toResponseList(planoPage.getContent());
        return PageResponseDTO.<PlanoResponseDTO>builder()
                .content(content)
                .pageNumber(planoPage.getNumber())
                .pageSize(planoPage.getSize())
                .totalElements(planoPage.getTotalElements())
                .totalPages(planoPage.getTotalPages())
                .last(planoPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public PlanoResponseDTO updatePlano(Integer idPlano, PlanoUpdateDTO request) {
        if (request == null || request.isEmpty()) {
            throw new NoFieldsToUpdateException(
                    "Debes enviar al menos un campo para actualizar: " +
                    "nombre, formulario, alcance, subsistema, codigoPlano, " +
                    "rev, observaciones, responsable o fechaFirma");
        }

        Plano plano = findPlanoOrFail(idPlano);
        if (plano.getStatus() == PlanoEstado.CERRADO) {
            throw new ConflictException("No se puede modificar un plano cerrado");
        }

        planoMapper.updateEntity(request, plano);
        plano = planoRepository.save(plano);
        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional
    public PlanoResponseDTO uploadPlanoPdf(Integer idPlano, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo PDF está vacío");
        }
        String name = file.getOriginalFilename();
        if (!"application/pdf".equals(file.getContentType())
                && (name == null || !name.toLowerCase().endsWith(".pdf"))) {
            throw new BusinessException("El archivo debe ser un PDF válido");
        }

        Plano plano = findPlanoOrFail(idPlano);
        if (plano.getStatus() == PlanoEstado.CERRADO) {
            throw new ConflictException("No se pueden adjuntar documentos a un plano cerrado");
        }

        if (plano.getUrlS3() != null && !plano.getUrlS3().isBlank()) {
            storageService.delete(plano.getUrlS3());
        }

        String key = storageService.upload(file, STORAGE_FOLDER_PLANOS);
        plano.setUrlS3(key);

        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            plano.setNroPaginas(doc.getNumberOfPages());
        } catch (IOException e) {
            plano.setNroPaginas(0);
        }

        plano = planoRepository.save(plano);
        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional
    public PlanoResponseDTO validarPlano(Integer idPlano) {
        Plano plano = findPlanoOrFail(idPlano);
        if (plano.getStatus() != PlanoEstado.ABIERTO) {
            throw new ConflictException(
                    "El plano solo puede ser validado si está en estado ABIERTO. " +
                    "Estado actual: " + plano.getStatus());
        }
        plano.setStatus(PlanoEstado.VALIDADO);
        return planoMapper.toResponse(planoRepository.save(plano));
    }

    @Override
    @Transactional
    public PlanoResponseDTO cerrarPlano(Integer idPlano) {
        Plano plano = findPlanoOrFail(idPlano);
        if (plano.getStatus() != PlanoEstado.VALIDADO) {
            throw new ConflictException(
                    "El plano debe estar VALIDADO para ser cerrado. " +
                    "Estado actual: " + plano.getStatus());
        }
        plano.setStatus(PlanoEstado.CERRADO);
        return planoMapper.toResponse(planoRepository.save(plano));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPlanoPdf(Integer idPlano) {
        Plano plano = findPlanoOrFail(idPlano);
        if (plano.getUrlS3() == null || plano.getUrlS3().isBlank()) {
            throw new ResourceNotFoundException(
                    "El plano con ID " + idPlano + " no tiene un PDF adjunto");
        }
        return storageService.download(plano.getUrlS3());
    }

    // -------- helpers --------
    private Plano findPlanoOrFail(Integer idPlano) {
        return planoRepository.findById(idPlano)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plano no encontrado con ID: " + idPlano));
    }
}
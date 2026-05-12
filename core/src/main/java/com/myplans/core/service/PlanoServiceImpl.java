package com.myplans.core.service;

import com.myplans.core.dto.PageResponseDTO;
import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.entity.Plano;
import com.myplans.core.entity.enums.PlanoEstado;
import com.myplans.core.exception.BusinessException;
import com.myplans.core.mapper.PlanoMapper;
import com.myplans.core.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoServiceImpl implements PlanoService {

    private final PlanoRepository planoRepository;
    private final PlanoMapper planoMapper;
    private final String uploadDir = "uploads/planos";

    @Override
    @Transactional
    public PlanoResponseDTO createPlano(PlanoRequestDTO request) {
        if (planoRepository.existsByCodigo(request.codigo())) {
            throw new BusinessException("El código de plano ya existe en el sistema.");
        }
        
        Plano plano = planoMapper.toEntity(request);
        plano = planoRepository.save(plano);
        
        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanoResponseDTO getPlanoById(Integer idPlano) {
        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new BusinessException("Plano no encontrado con el ID proporcionado."));
        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<PlanoResponseDTO> getAllPlanos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Plano> planoPage = planoRepository.findAll(pageable);
        
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
    public PlanoResponseDTO uploadPlanoPdf(Integer idPlano, MultipartFile file) {
        if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
            throw new BusinessException("El archivo debe ser un PDF válido y no estar vacío.");
        }

        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new BusinessException("Plano no encontrado."));

        if (plano.getEstado() == PlanoEstado.CERRADO) {
            throw new BusinessException("No se pueden adjuntar documentos a un plano cerrado.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + ".pdf";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            plano.setUrlPdf(filePath.toString());
            plano = planoRepository.save(plano);

            return planoMapper.toResponse(plano);

        } catch (IOException e) {
            throw new BusinessException("Error al guardar el archivo PDF localmente.");
        }
    }

    @Override
    @Transactional
    public PlanoResponseDTO validarPlano(Integer idPlano) {
        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new BusinessException("Plano no encontrado."));

        if (plano.getEstado() != PlanoEstado.PENDIENTE) {
            throw new BusinessException("El plano solo puede ser validado si se encuentra en estado PENDIENTE.");
        }

        plano.setEstado(PlanoEstado.VALIDADO);
        plano = planoRepository.save(plano);

        return planoMapper.toResponse(plano);
    }

    @Override
    @Transactional
    public PlanoResponseDTO cerrarPlano(Integer idPlano) {
        Plano plano = planoRepository.findById(idPlano)
                .orElseThrow(() -> new BusinessException("Plano no encontrado."));

        if (plano.getEstado() != PlanoEstado.VALIDADO) {
            throw new BusinessException("El plano debe estar VALIDADO para poder ser cerrado.");
        }

        plano.setEstado(PlanoEstado.CERRADO);
        plano = planoRepository.save(plano);

        return planoMapper.toResponse(plano);
    }
}
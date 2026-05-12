package com.myplans.core.mapper;

import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.entity.Plano;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanoMapper {
    
    @Mapping(target = "idPlano", ignore = true)
    @Mapping(target = "fechaIngreso", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "urlPdf", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Plano toEntity(PlanoRequestDTO request);

    PlanoResponseDTO toResponse(Plano entity);

    List<PlanoResponseDTO> toResponseList(List<Plano> entities);
}
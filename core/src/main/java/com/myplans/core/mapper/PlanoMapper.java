package com.myplans.core.mapper;

import com.myplans.core.dto.PlanoRequestDTO;
import com.myplans.core.dto.PlanoResponseDTO;
import com.myplans.core.entity.Plano;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanoMapper {
    @Mapping(target = "idPlano", ignore = true)
    @Mapping(target = "urlS3", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "nroPaginas", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Plano toEntity(PlanoRequestDTO request);

    @Mapping(target = "cantidadTags", expression = "java(entity.getTags() == null ? 0L : (long) entity.getTags().size())")
    PlanoResponseDTO toResponse(Plano entity);

    @Named("toResponseList")
    List<PlanoResponseDTO> toResponseList(List<Plano> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idPlano", ignore = true)
    @Mapping(target = "urlS3", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "nroPaginas", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "tags", ignore = true)
    void updateEntity(com.myplans.core.dto.PlanoUpdateDTO request, @MappingTarget Plano entity);
}
package org.redhat.sbomer.mappers.api;

import org.redhat.sbomer.model.BaseSBOM;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedSourcePolicy = ReportingPolicy.WARN,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        implementationPackage = "org.redhat.sbomer.mappers",
        componentModel = "cdi")
public interface BaseSBOMMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generationTime", ignore = true)
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "sbom", source = "bom")
    BaseSBOM toEntity(org.redhat.sbomer.dto.BaseSBOM dtoEntity);

    @Mapping(target = "id", expression = "java( dbEntity.getId().toString() )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "generationTime", source = "generationTime")
    @Mapping(target = "bom", source = "sbom")
    org.redhat.sbomer.dto.BaseSBOM toDTO(BaseSBOM dbEntity);

    List<BaseSBOM> toEntityList(List<org.redhat.sbomer.dto.BaseSBOM> addressList);

    List<org.redhat.sbomer.dto.BaseSBOM> toDtoList(List<BaseSBOM> addressEntityList);

}
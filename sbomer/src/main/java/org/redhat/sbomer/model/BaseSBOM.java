package org.redhat.sbomer.model;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jboss.pnc.api.deliverablesanalyzer.dto.BuildSystemType;
import org.redhat.sbomer.validation.CycloneDxBom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@DynamicUpdate
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@ToString
@Table(
        name = "base_sbom",
        indexes = { @Index(name = "idx_basesbom_buildid", columnList = "build_id") },
        uniqueConstraints = @UniqueConstraint(name = "uq_basesbom_buildid", columnNames = { "build_id" }))
@NamedQueries({ @NamedQuery(name = BaseSBOM.FIND_BY_BUILDID, query = "FROM BaseSBOM WHERE buildId = ?1") })
public class BaseSBOM extends PanacheEntityBase {

    public static final String FIND_BY_BUILDID = "BaseSBOM.findByBuildId";

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(name = "build_id", nullable = false, updatable = false)
    @NotBlank(message = "Build identifier missing")
    private String buildId;

    private Instant generationTime;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "sbom", columnDefinition = JsonTypes.JSON_BIN)
    @CycloneDxBom
    private JsonNode sbom;

    @JsonIgnore
    public Bom getCycloneDxBom() {
        try {
            return new JsonParser().parse(sbom.toString().getBytes());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Don't do this
        // We always should a valid return
        return null;
    }
}

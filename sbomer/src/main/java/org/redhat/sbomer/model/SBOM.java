package org.redhat.sbomer.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
@Table(name = "sboms")
public class SBOM implements Serializable {

  @Id
  @NotBlank(message = "PNC build identifier missing")
  private String buildId;

  @Type(type = JsonTypes.JSON_BIN)
  @Column(name = "bom", columnDefinition = JsonTypes.JSON_BIN)
  @NotNull(message = "Missing BOM")
  @CycloneDxBom
  private JsonNode bom;

  public Bom getCycloneDxBom() {
    try {
      return new JsonParser().parse(bom.toString().getBytes());
    } catch (ParseException e) {
      e.printStackTrace();
    }

    // Don't do this
    // We always should a valid return
    return null;
  }
}

package org.redhat.sbomer.validations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.parsers.JsonParser;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.fasterxml.jackson.databind.JsonNode;

public class CycloneDxBomValidator implements ConstraintValidator<CycloneDxBom, JsonNode> {

  @Override
  public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
    if (value == null) {
      return false;
    }

    List<ParseException> exceptions = Collections.emptyList();

    try {
      exceptions = new JsonParser().validate(value, Version.VERSION_14);

      if (exceptions.isEmpty()) {
        return true;
      }

    } catch (IOException e) {
      context.unwrap(HibernateConstraintValidatorContext.class).addMessageParameter("errors", "unable to parse object");

      return false;
    }

    context.unwrap(HibernateConstraintValidatorContext.class).addMessageParameter("errors", exceptions.stream()
        .map(cv -> "bom" + cv.getMessage().substring(1)).toList().stream().collect(Collectors.joining(", ")));

    return false;
  }

}

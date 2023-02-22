package org.redhat.sbomer.validations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.cyclonedx.exception.ParseException;

import lombok.Getter;

@Getter
public class ValidationError {
  List<String> messages;

  public ValidationError(String message) {
    this.messages = Collections.singletonList(message);
  }

  /**
   * Converts Hibernate Validator violations in a readable list of messages.
   * 
   * @param violations
   */
  public ValidationError(Set<? extends ConstraintViolation<?>> violations) {
    messages = violations.stream()
        .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage()).toList();
  }

  /**
   * Converts CycloneDX validation exceptions in a readable list of messages.
   * 
   * @param violations
   */
  public ValidationError(List<ParseException> violations) {
    messages = violations.stream()
        .map(cv -> "bom" + cv.getMessage().substring(1)).toList();
  }

}

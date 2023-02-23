package org.redhat.sbomer.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CycloneDxBomValidator.class)
@Documented
public @interface CycloneDxBom {
    String message() default "not a valid CycloneDX object: {errors}";

    List<String> errors = Collections.emptyList();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

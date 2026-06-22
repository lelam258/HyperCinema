package com.cinema.hyperCinema.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BranchTimeRangeValidator.class)
public @interface BranchTimeRangeValid {

    String message() default "{branch.time_range.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

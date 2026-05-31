package com.cinema.hyperCinema.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BranchTimeRangeValidator.class)
public @interface BranchTimeRangeValid {

    String message() default "{branch.time_range.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

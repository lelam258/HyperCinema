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
@Constraint(validatedBy = MovieReleaseDateRangeValidator.class)
public @interface MovieReleaseDateRangeValid {

    String message() default "{movie.search.release_date_range_ignored}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

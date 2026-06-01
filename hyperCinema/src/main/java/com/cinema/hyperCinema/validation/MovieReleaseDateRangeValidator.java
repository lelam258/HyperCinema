package com.cinema.hyperCinema.validation;

import java.time.LocalDate;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import jakarta.validation.constraintvalidation.ValidationTarget;

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
public class MovieReleaseDateRangeValidator
        implements ConstraintValidator<MovieReleaseDateRangeValid, Object> {

    private static final String RELEASE_DATE_FROM_PROPERTY = "releaseDateFrom";
    private static final String RELEASE_DATE_TO_PROPERTY = "releaseDateTo";

    @Override
    public boolean isValid(Object target, ConstraintValidatorContext context) {
        if (target == null) {
            return true;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(target);
        if (!wrapper.isReadableProperty(RELEASE_DATE_FROM_PROPERTY)
                || !wrapper.isReadableProperty(RELEASE_DATE_TO_PROPERTY)) {

            return true;
        }

        LocalDate from = readLocalDate(wrapper, RELEASE_DATE_FROM_PROPERTY);
        LocalDate to = readLocalDate(wrapper, RELEASE_DATE_TO_PROPERTY);

        if (from == null && to == null) {
            return true;
        }

        if (from == null || to == null) {
            return false;
        }

        return !from.isAfter(to);
    }

    private static LocalDate readLocalDate(BeanWrapper wrapper, String property) {
        Object value = wrapper.getPropertyValue(property);
        return (value instanceof LocalDate localDate) ? localDate : null;
    }
}

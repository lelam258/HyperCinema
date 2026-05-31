package com.cinema.hyperCinema.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import jakarta.validation.constraintvalidation.ValidationTarget;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalTime;

@SupportedValidationTarget(ValidationTarget.ANNOTATED_ELEMENT)
public class BranchTimeRangeValidator
        implements ConstraintValidator<BranchTimeRangeValid, Object> {

    private static final String OPENING_TIME_PROPERTY = "openingTime";
    private static final String CLOSING_TIME_PROPERTY = "closingTime";

    @Override
    public boolean isValid(Object target, ConstraintValidatorContext context) {
        if (target == null) {
            return true;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(target);
        if (!wrapper.isReadableProperty(OPENING_TIME_PROPERTY)
                || !wrapper.isReadableProperty(CLOSING_TIME_PROPERTY)) {

            return true;
        }

        LocalTime opening = readLocalTime(wrapper, OPENING_TIME_PROPERTY);
        LocalTime closing = readLocalTime(wrapper, CLOSING_TIME_PROPERTY);

        if (opening == null || closing == null) {

            return true;
        }

        return opening.isBefore(closing);
    }

    private static LocalTime readLocalTime(BeanWrapper wrapper, String property) {
        Object value = wrapper.getPropertyValue(property);
        return (value instanceof LocalTime localTime) ? localTime : null;
    }
}

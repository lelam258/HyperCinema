package com.cinema.hyperCinema.service.branch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.cinema.hyperCinema.dto.admin.branch.response.FieldChange;
import com.cinema.hyperCinema.model.Branch;

@Component
public class BranchDiffer {

    public List<FieldChange> diff(Branch existing, Branch proposed) {
        Objects.requireNonNull(existing, "existing branch must not be null");
        Objects.requireNonNull(proposed, "proposed branch must not be null");

        List<FieldChange> changes = new ArrayList<>();
        compareField(existing, proposed, "name", Branch::getName, changes);
        compareField(existing, proposed, "address", Branch::getAddress, changes);
        compareField(existing, proposed, "city", Branch::getCity, changes);
        compareField(existing, proposed, "phone", Branch::getPhone, changes);
        compareField(existing, proposed, "status", Branch::getStatus, changes);
        compareField(existing, proposed, "openingTime", Branch::getOpeningTime, changes);
        compareField(existing, proposed, "closingTime", Branch::getClosingTime, changes);
        return Collections.unmodifiableList(changes);
    }

    private static <V> void compareField(Branch existing,
                                         Branch proposed,
                                         String fieldName,
                                         Function<Branch, V> accessor,
                                         List<FieldChange> changes) {
        V oldValue = accessor.apply(existing);
        V newValue = accessor.apply(proposed);
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(fieldName, oldValue, newValue));
        }
    }
}

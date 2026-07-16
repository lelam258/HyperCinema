package com.cinema.hyperCinema.dto.admin.branch.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateResult {

    private BranchDetailView branch;

    private boolean hasChanges;

    private List<FieldChange> changes;
}

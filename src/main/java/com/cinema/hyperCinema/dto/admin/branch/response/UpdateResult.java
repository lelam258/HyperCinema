package com.cinema.hyperCinema.dto.admin.branch.response;

import lombok.*;

import java.util.List;

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

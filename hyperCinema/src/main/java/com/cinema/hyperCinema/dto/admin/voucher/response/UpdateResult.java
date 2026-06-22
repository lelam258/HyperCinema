package com.cinema.hyperCinema.dto.admin.voucher.response;

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

    private boolean hasChanges;

    private Integer voucherId;
}

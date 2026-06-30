package com.cinema.hyperCinema.dto.admin.voucher.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateResult {

    private boolean hasChanges;

    private Integer voucherId;
}

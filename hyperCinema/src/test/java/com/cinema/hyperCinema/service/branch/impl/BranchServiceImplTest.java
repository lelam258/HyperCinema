package com.cinema.hyperCinema.service.branch.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cinema.hyperCinema.dto.admin.branch.request.BranchUpdateRequest;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.UpdateResult;
import com.cinema.hyperCinema.model.Branch;
import com.cinema.hyperCinema.repository.BranchRepository;
import com.cinema.hyperCinema.repository.HallRepository;
import com.cinema.hyperCinema.repository.ShowtimeRepository;
import com.cinema.hyperCinema.repository.UserRepository;
import com.cinema.hyperCinema.service.audit.BranchAuditLogger;
import com.cinema.hyperCinema.service.branch.BranchDiffer;
import com.cinema.hyperCinema.util.BranchMapper;
import com.cinema.hyperCinema.validation.BranchValidator;

class BranchServiceImplTest {

    @Test
    void updateIgnoresWhitespaceOnlyChangesAfterNormalization() {
        BranchRepository branchRepository = mock(BranchRepository.class);
        BranchMapper branchMapper = mock(BranchMapper.class);
        BranchAuditLogger auditLogger = mock(BranchAuditLogger.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        HallRepository hallRepository = mock(HallRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        Branch existing = new Branch();
        existing.setBranchId(3);
        existing.setName("CGV Vincom");
        existing.setAddress("191 Ba Trieu");
        existing.setCity("Ha Noi");
        existing.setPhone("0905123456");
        existing.setStatus("Active");
        existing.setOpeningTime(LocalTime.of(9, 0));
        existing.setClosingTime(LocalTime.of(22, 0));
        existing.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));

        BranchUpdateRequest request = new BranchUpdateRequest();
        request.setName("  CGV Vincom  ");
        request.setAddress("  191 Ba Trieu ");
        request.setCity(" Ha Noi ");
        request.setPhone(" 0905123456 ");
        request.setOpeningTime(LocalTime.of(9, 0));
        request.setClosingTime(LocalTime.of(22, 0));

        when(branchRepository.findById(3)).thenReturn(Optional.of(existing));
        when(branchMapper.toDetailView(existing)).thenReturn(BranchDetailView.builder().branchId(3).build());

        BranchServiceImpl service = new BranchServiceImpl(
                branchRepository,
                branchMapper,
                new BranchValidator(branchRepository, userRepository),
                auditLogger,
                new BranchDiffer(),
                showtimeRepository,
                hallRepository,
                userRepository);

        UpdateResult result = service.update(3, request, null);

        assertThat(result.isHasChanges()).isFalse();
        verify(branchRepository, never()).save(any(Branch.class));
    }
}

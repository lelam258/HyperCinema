package com.cinema.hyperCinema.service.membership;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.MembershipPlanUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipCreateRequest;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipSearchCriteria;
import com.cinema.hyperCinema.dto.admin.membership.request.UserMembershipUpdateRequest;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanDetailView;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanListItem;
import com.cinema.hyperCinema.dto.admin.membership.response.MembershipPlanOption;
import com.cinema.hyperCinema.dto.admin.membership.response.UpdateResult;
import com.cinema.hyperCinema.dto.admin.membership.response.UserMembershipDetailView;
import com.cinema.hyperCinema.dto.admin.membership.response.UserMembershipListItem;
import com.cinema.hyperCinema.dto.admin.membership.response.UserOption;
import com.cinema.hyperCinema.model.User;

public interface MembershipService {

    Page<MembershipPlanListItem> searchPlans(MembershipPlanSearchCriteria criteria, Pageable pageable, User actor);

    MembershipPlanDetailView findPlan(Integer planId, User actor);

    MembershipPlanDetailView createPlan(MembershipPlanCreateRequest request, User actor);

    int suggestedPlanLevel(Integer beforePlanId, Integer afterPlanId, User actor);

    UpdateResult updatePlan(Integer planId, MembershipPlanUpdateRequest request, User actor);

    void deactivatePlan(Integer planId, User actor);

    Page<UserMembershipListItem> searchUserMemberships(UserMembershipSearchCriteria criteria,
                                                       Pageable pageable,
                                                       User actor);

    UserMembershipDetailView findUserMembership(Integer membershipId, User actor);

    UserMembershipDetailView createUserMembership(UserMembershipCreateRequest request, User actor);

    UpdateResult updateUserMembership(Integer membershipId, UserMembershipUpdateRequest request, User actor);

    void cancelUserMembership(Integer membershipId, User actor);

    List<MembershipPlanOption> activePlanOptions();

    List<MembershipPlanOption> allPlanOptions();

    List<UserOption> customerOptions(String keyword);
}

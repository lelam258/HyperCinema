package com.cinema.hyperCinema.service.feedback;

import com.cinema.hyperCinema.model.Feedback;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.dto.feedback.FeedbackCreateRequest;
import com.cinema.hyperCinema.dto.feedback.FeedbackUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface FeedbackService {
    Page<Feedback> getFeedbackList(Pageable pageable);
    Feedback getFeedbackDetail(Integer feedbackId);
    Feedback createFeedback(FeedbackCreateRequest request, User user);
    Feedback updateFeedback(Integer feedbackId, FeedbackUpdateRequest request, User user);
    List<Feedback> getFeedbackHistory(User user);
    Map<String, Object> getFeedbackStats();
}

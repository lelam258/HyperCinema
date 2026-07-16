package com.cinema.hyperCinema.service.feedback.impl;

import com.cinema.hyperCinema.model.Feedback;
import com.cinema.hyperCinema.model.User;
import com.cinema.hyperCinema.dto.feedback.FeedbackCreateRequest;
import com.cinema.hyperCinema.dto.feedback.FeedbackUpdateRequest;
import com.cinema.hyperCinema.exception.feedback.FeedbackNotFoundException;
import com.cinema.hyperCinema.exception.feedback.FeedbackValidationException;
import com.cinema.hyperCinema.repository.FeedbackRepository;
import com.cinema.hyperCinema.service.feedback.FeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbackList(Pageable pageable) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback getFeedbackDetail(Integer feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException("feedback.not_found"));
    }

    @Override
    public Feedback createFeedback(FeedbackCreateRequest request, User user) {
        if (request == null) {
            throw new FeedbackValidationException("feedback.comment.required");
        }
        validateRating(request.getRating());
        validateComment(request.getComment());

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment().trim());

        return feedbackRepository.save(feedback);
    }

    @Override
    public Feedback updateFeedback(Integer feedbackId, FeedbackUpdateRequest request, User user) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new FeedbackNotFoundException("feedback.not_found"));

        // Xác thực quyền sở hữu: Chỉ cho phép chính khách hàng đã tạo chỉnh sửa
        if (!feedback.getUser().getUserId().equals(user.getUserId())) {
            throw new FeedbackValidationException("feedback.access_denied");
        }

        if (request == null) {
            throw new FeedbackValidationException("feedback.comment.required");
        }
        validateRating(request.getRating());
        validateComment(request.getComment());

        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment().trim());

        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackHistory(User user) {
        return feedbackRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFeedbackStats() {
        List<Feedback> allFeedbacks = feedbackRepository.findAll();
        long total = allFeedbacks.size();
        double sum = 0.0;
        int[] counts = new int[5]; // index 0: 1 star, ..., index 4: 5 stars

        for (Feedback f : allFeedbacks) {
            int rating = f.getRating();
            sum += rating;
            if (rating >= 1 && rating <= 5) {
                counts[rating - 1]++;
            }
        }

        double avg = total > 0 ? (double) Math.round((sum / total) * 10) / 10 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", total);
        stats.put("averageRating", avg);
        stats.put("ratingCounts", counts);

        // calculate percentage
        double[] percents = new double[5];
        if (total > 0) {
            for (int i = 0; i < 5; i++) {
                percents[i] = (double) Math.round(((double) counts[i] / total * 100) * 10) / 10;
            }
        }
        stats.put("ratingPercentages", percents);

        return stats;
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new FeedbackValidationException("feedback.rating.required");
        }
    }

    private void validateComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new FeedbackValidationException("feedback.comment.required");
        }
    }
}

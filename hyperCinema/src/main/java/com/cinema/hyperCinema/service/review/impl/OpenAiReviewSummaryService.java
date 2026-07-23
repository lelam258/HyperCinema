package com.cinema.hyperCinema.service.review.impl;

import com.cinema.hyperCinema.dto.review.ReviewSummaryResponse;
import com.cinema.hyperCinema.dto.review.ReviewSummaryResponse.ReviewAspectSummary;
import com.cinema.hyperCinema.model.Movie;
import com.cinema.hyperCinema.model.Review;
import com.cinema.hyperCinema.repository.MovieRepository;
import com.cinema.hyperCinema.repository.ReviewRepository;
import com.cinema.hyperCinema.service.review.ReviewSummaryService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenAiReviewSummaryService implements ReviewSummaryService {

    private static final List<String> ASPECTS =
            List.of("Kịch bản", "Diễn xuất", "Hình ảnh", "Âm thanh", "Giải trí");

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5.6-luna}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public OpenAiReviewSummaryService(
            ReviewRepository reviewRepository,
            MovieRepository movieRepository,
            ObjectMapper objectMapper) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public ReviewSummaryResponse summarize(Integer movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phim ID: " + movieId));
        List<Review> reviews = reviewRepository.findByMovieIdWithUser(movieId);
        if (reviews.isEmpty()) {
            return new ReviewSummaryResponse(movie.getTitle(), 0, 0, 0, 0,
                    "Chưa có đủ đánh giá để AI tổng hợp.", List.of(), false);
        }

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return requestOpenAi(movie, reviews);
            } catch (Exception exception) {
                // The review screen must remain useful if the provider is temporarily unavailable.
            }
        }
        return buildLocalFallback(movie, reviews);
    }

    private ReviewSummaryResponse requestOpenAi(Movie movie, List<Review> reviews) throws Exception {
        StringBuilder reviewText = new StringBuilder();
        for (Review review : reviews.stream().limit(100).toList()) {
            reviewText.append("- ").append(review.getRating()).append("/5: ")
                    .append(review.getComment()).append('\n');
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("reasoning", Map.of("effort", "low"));
        requestBody.put("input", """
                Bạn là chuyên gia phân tích phản hồi khán giả Việt Nam.
                Hãy tổng hợp trung thực các đánh giá cho phim "%s".
                Không thêm thông tin ngoài đánh giá. Trả về JSON theo schema đã cho.
                Tỷ lệ positivePercent + neutralPercent + negativePercent phải bằng 100.
                Mỗi aspect chỉ xuất hiện nếu có đánh giá thực sự nhắc tới; tối đa 5 aspect.

                Đánh giá:
                %s
                """.formatted(movie.getTitle(), reviewText));
        requestBody.put("text", Map.of(
                "verbosity", "low",
                "format", Map.of(
                        "type", "json_schema",
                        "name", "movie_review_summary",
                        "strict", true,
                        "schema", responseSchema())));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/responses"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI response status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String outputText = extractOutputText(root);
        JsonNode result = objectMapper.readTree(outputText);
        List<ReviewAspectSummary> aspects = new ArrayList<>();
        result.path("aspects").forEach(node -> aspects.add(new ReviewAspectSummary(
                node.path("name").asText(),
                node.path("reviewCount").asInt(),
                node.path("positivePercent").asInt(),
                node.path("neutralPercent").asInt(),
                node.path("negativePercent").asInt(),
                node.path("summary").asText())));
        return new ReviewSummaryResponse(
                movie.getTitle(),
                reviews.size(),
                result.path("positivePercent").asInt(),
                result.path("neutralPercent").asInt(),
                result.path("negativePercent").asInt(),
                result.path("summary").asText(),
                aspects,
                true);
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI response did not contain output_text");
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> percentageProperties = Map.of(
                "positivePercent", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                "neutralPercent", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                "negativePercent", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        Map<String, Object> aspectProperties = new LinkedHashMap<>(percentageProperties);
        aspectProperties.put("name", Map.of("type", "string"));
        aspectProperties.put("reviewCount", Map.of("type", "integer", "minimum", 1));
        aspectProperties.put("summary", Map.of("type", "string"));

        Map<String, Object> properties = new LinkedHashMap<>(percentageProperties);
        properties.put("summary", Map.of("type", "string"));
        properties.put("aspects", Map.of(
                "type", "array",
                "items", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("name", "reviewCount", "positivePercent",
                                "neutralPercent", "negativePercent", "summary"),
                        "properties", aspectProperties)));
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("positivePercent", "neutralPercent", "negativePercent",
                        "summary", "aspects"),
                "properties", properties);
    }

    private ReviewSummaryResponse buildLocalFallback(Movie movie, List<Review> reviews) {
        Sentiment overall = sentiment(reviews);
        List<ReviewAspectSummary> aspects = new ArrayList<>();
        Map<String, List<String>> keywords = Map.of(
                "Kịch bản", List.of("kịch bản", "cốt truyện", "nội dung", "plot"),
                "Diễn xuất", List.of("diễn xuất", "diễn viên", "nhân vật"),
                "Hình ảnh", List.of("hình ảnh", "kỹ xảo", "đồ họa", "cảnh quay"),
                "Âm thanh", List.of("âm thanh", "âm nhạc", "nhạc phim", "ost"),
                "Giải trí", List.of("giải trí", "hài", "vui", "cuốn"));
        for (String aspect : ASPECTS) {
            List<Review> matching = reviews.stream()
                    .filter(review -> containsAny(review.getComment(), keywords.get(aspect)))
                    .toList();
            if (!matching.isEmpty()) {
                Sentiment value = sentiment(matching);
                aspects.add(new ReviewAspectSummary(aspect, matching.size(), value.positive(),
                        value.neutral(), value.negative(), aspectSummary(aspect, value)));
            }
        }
        String summary = "Khán giả nhìn chung " + (overall.positive() >= 60
                ? "đánh giá tích cực"
                : overall.negative() >= 40 ? "có nhiều ý kiến chưa hài lòng" : "có ý kiến khá đa chiều")
                + " về bộ phim. Điểm mạnh và hạn chế được tổng hợp từ "
                + reviews.size() + " đánh giá thực tế.";
        return new ReviewSummaryResponse(movie.getTitle(), reviews.size(), overall.positive(),
                overall.neutral(), overall.negative(), summary, aspects, false);
    }

    private boolean containsAny(String text, List<String> keywords) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(normalized::contains);
    }

    private Sentiment sentiment(List<Review> reviews) {
        int positive = (int) reviews.stream().filter(review -> review.getRating() >= 4).count();
        int negative = (int) reviews.stream().filter(review -> review.getRating() <= 2).count();
        int positivePercent = (int) Math.round(positive * 100.0 / reviews.size());
        int negativePercent = (int) Math.round(negative * 100.0 / reviews.size());
        return new Sentiment(positivePercent, 100 - positivePercent - negativePercent, negativePercent);
    }

    private String aspectSummary(String aspect, Sentiment sentiment) {
        if (sentiment.positive() >= 70) {
            return aspect + " được phần lớn khán giả khen ngợi, dù vẫn có một số góp ý nhỏ.";
        }
        if (sentiment.negative() >= 40) {
            return aspect + " là điểm gây nhiều tranh luận và nhận về khá nhiều góp ý.";
        }
        return "Ý kiến về " + aspect.toLowerCase(Locale.ROOT)
                + " khá đa chiều, có cả nhận xét tích cực lẫn điểm cần cải thiện.";
    }

    private record Sentiment(int positive, int neutral, int negative) {
    }
}

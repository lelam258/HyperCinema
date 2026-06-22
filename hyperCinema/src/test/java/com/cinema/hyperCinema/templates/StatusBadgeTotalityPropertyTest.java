package com.cinema.hyperCinema.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based test (jqwik) cho fragment {@code statusBadge(status)} hợp nhất
 * trong {@code templates/fragments/components.html} (task 3.2).
 *
 * <p>Fragment status badge là bề mặt PBT thực sự duy nhất của tính năng
 * UI Redesign — một ánh xạ thuần {@code status: String -> HTML} có không gian
 * đầu vào lớn (mọi chuỗi, gồm {@code null}/rỗng/whitespace/ký tự đặc biệt/giá
 * trị lạ). Test render fragment thật qua một {@link SpringTemplateEngine}
 * standalone (cùng cơ chế với {@code LayoutFragmentTest}).
 *
 * <p>Bảng mapping (design.md §4.3):
 * <pre>
 *   NowShowing, Active   -> .badge .badge-success
 *   ComingSoon           -> .badge .badge-info
 *   Maintenance          -> .badge .badge-warning
 *   Ended, Inactive      -> .badge .badge-neutral
 *   (null/rỗng/lạ)       -> .badge .badge-neutral + nhãn "Không xác định"
 * </pre>
 */
class StatusBadgeTotalityPropertyTest {

    /** Tên logic của fragment template trên classpath (templates/fragments/components.html). */
    private static final String COMPONENTS = "fragments/components";

    /** Nhãn phi-màu cho giá trị không xác định (design.md §4.3 / §5.3). */
    private static final String UNKNOWN_LABEL = "Không xác định";

    /** Các giá trị trạng thái đã biết (Movie + Branch). */
    private static final Set<String> KNOWN_STATUSES = Set.of(
            "ComingSoon", "NowShowing", "Ended", "Active", "Maintenance", "Inactive");

    /**
     * Engine được khởi tạo trong static initializer (không dùng @BeforeAll của
     * JUnit Jupiter) để bảo đảm sẵn sàng dù vòng đời do engine jqwik điều khiển.
     */
    private static final SpringTemplateEngine ENGINE = buildEngine();

    private static SpringTemplateEngine buildEngine() {
        // Resolver 1: nạp fragment thật từ classpath (src/main/resources/templates/).
        ClassLoaderTemplateResolver classpathResolver = new ClassLoaderTemplateResolver();
        classpathResolver.setPrefix("templates/");
        classpathResolver.setSuffix(".html");
        classpathResolver.setTemplateMode(TemplateMode.HTML);
        classpathResolver.setCharacterEncoding("UTF-8");
        classpathResolver.setResolvablePatterns(Set.of("fragments/*"));
        classpathResolver.setCacheable(true);
        classpathResolver.setOrder(1);

        // Resolver 2: coi chính nội dung chuỗi là template (wrapper gọi th:replace).
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode(TemplateMode.HTML);
        stringResolver.setOrder(2);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(classpathResolver);
        engine.addTemplateResolver(stringResolver);
        return engine;
    }

    // ---------------------------------------------------------------------
    // Hạ tầng render
    // ---------------------------------------------------------------------

    /**
     * Render {@code statusBadge(status)} bằng cách truyền {@code status} qua
     * biến context (tránh vấn đề escape khi nhúng trực tiếp giá trị ngẫu nhiên
     * vào biểu thức fragment).
     */
    private static String renderBadge(String status) {
        String wrapper = "<div xmlns:th=\"http://www.thymeleaf.org\" "
                + "th:replace=\"~{" + COMPONENTS + " :: statusBadge(${status})}\"></div>";

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);

        WebContext context = new WebContext(exchange, Locale.forLanguageTag("vi"));
        context.setVariable("status", status);
        return ENGINE.process(wrapper, context);
    }

    // ---------------------------------------------------------------------
    // Oracle: variant + label kỳ vọng theo mapping §4.3
    // ---------------------------------------------------------------------

    private static String expectedVariant(String status) {
        if (status == null) {
            return "badge-neutral";
        }
        switch (status) {
            case "NowShowing":
            case "Active":
                return "badge-success";
            case "ComingSoon":
                return "badge-info";
            case "Maintenance":
                return "badge-warning";
            case "Ended":
            case "Inactive":
                return "badge-neutral";
            default:
                return "badge-neutral";
        }
    }

    private static String expectedLabel(String status) {
        return (status != null && KNOWN_STATUSES.contains(status)) ? status : UNKNOWN_LABEL;
    }

    // ---------------------------------------------------------------------
    // Generator: trộn (a) giá trị đã biết, (b) null/rỗng/whitespace,
    //            (c) chuỗi ngẫu nhiên & ký tự đặc biệt
    // ---------------------------------------------------------------------

    @Provide
    Arbitrary<String> statuses() {
        // (a) Giá trị đã biết
        Arbitrary<String> known = Arbitraries.of(
                "ComingSoon", "NowShowing", "Ended", "Active", "Maintenance", "Inactive");

        // (b) null / rỗng / whitespace
        Arbitrary<String> emptyish = Arbitraries.of("", " ", "   ", "\t", "\n", " \t \n ");
        Arbitrary<String> nullArb = Arbitraries.<String>just(null);

        // (c) Chuỗi ngẫu nhiên (printable ASCII, gồm cả ký tự đặc biệt)
        Arbitrary<String> random = Arbitraries.strings()
                .withCharRange('\u0020', '\u007e')
                .ofMinLength(0).ofMaxLength(24);

        // (c') Chuỗi giàu ký tự đặc biệt HTML để stress nhánh fallback
        Arbitrary<String> specials = Arbitraries.strings()
                .withChars('<', '>', '&', '"', '\'', '/', ' ', 'a', 'Z', '1', '@', '#')
                .ofMinLength(1).ofMaxLength(16);

        return Arbitraries.oneOf(known, emptyish, nullArb, random, specials);
    }

    // ---------------------------------------------------------------------
    // Property 1
    // ---------------------------------------------------------------------

    // Feature: ui-redesign, Property 1: Status badge totality + non-color label
    /**
     * <b>Property 1: Status badge có tính toàn phần và luôn có nhãn phi-màu.</b>
     *
     * <p>Với mọi chuỗi {@code status} (giá trị đã biết, null/rỗng/whitespace,
     * hoặc chuỗi lạ), fragment render đúng MỘT phần tử badge mà: (a) mang biến
     * thể class đúng theo mapping (giá trị lạ -> neutral), và (b) chứa một nhãn
     * văn bản KHÔNG rỗng — nghĩa là badge không bao giờ trống và không bao giờ
     * chỉ dựa vào màu sắc.
     *
     * <p><b>Validates: Requirements 4.5, 4.8, 8.4</b>
     */
    @Property(tries = 200)
    void statusBadgeIsTotalAndAlwaysLabeled(@ForAll("statuses") String status) {
        String html = renderBadge(status);

        // (1) TOÀN PHẦN: render đúng MỘT phần tử badge cho mọi input.
        int badgeCount = countBadges(html);
        assertEquals(1, badgeCount,
                () -> "statusBadge phải render đúng 1 badge cho status=[" + status
                        + "]; đếm được " + badgeCount + "; html=\n" + html);

        BadgeSpan badge = extractBadge(html);

        // (2) MAPPING: badge mang class nền '.badge' + biến thể đúng theo §4.3.
        assertTrue(badge.cssClass.contains("badge"),
                () -> "badge phải mang class nền 'badge' cho status=[" + status
                        + "]; class=" + badge.cssClass);

        String expectedVariant = expectedVariant(status);
        assertTrue(badge.cssClass.contains(expectedVariant),
                () -> "badge cho status=[" + status + "] phải mang biến thể '" + expectedVariant
                        + "'; nhận class=" + badge.cssClass);

        // (2b) Biến thể phải là một trong tập hữu hạn đã định nghĩa (totality của variant).
        assertTrue(isOneOfKnownVariants(badge.cssClass),
                () -> "badge cho status=[" + status + "] phải dùng đúng 1 biến thể đã biết "
                        + "(success/info/warning/neutral); nhận class=" + badge.cssClass);

        // (3) NHÃN PHI-MÀU: nhãn text không rỗng (chỉ báo trạng thái không chỉ dựa vào màu).
        assertTrue(badge.text != null && !badge.text.trim().isEmpty(),
                () -> "badge cho status=[" + status + "] phải có nhãn text KHÔNG rỗng; nhận text=["
                        + badge.text + "]");

        // (3b) Nhãn đúng theo hợp đồng: giá trị đã biết -> chính chuỗi đó;
        //      giá trị lạ -> "Không xác định".
        String expectedLabel = expectedLabel(status);
        assertEquals(expectedLabel, badge.text.trim(),
                () -> "badge cho status=[" + status + "] phải có nhãn '" + expectedLabel
                        + "'; nhận '" + badge.text.trim() + "'");
    }

    // ---------------------------------------------------------------------
    // Tiện ích parse badge
    // ---------------------------------------------------------------------

    private static final Pattern BADGE_SPAN = Pattern.compile(
            "<span[^>]*class=\"(badge[^\"]*)\"[^>]*>(.*?)</span>", Pattern.DOTALL);

    private static int countBadges(String html) {
        Matcher matcher = BADGE_SPAN.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static BadgeSpan extractBadge(String html) {
        Matcher matcher = BADGE_SPAN.matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Không tìm thấy phần tử badge nào trong output:\n" + html);
        }
        return new BadgeSpan(matcher.group(1), matcher.group(2));
    }

    private static boolean isOneOfKnownVariants(String cssClass) {
        int hits = 0;
        for (String variant : new String[] {"badge-success", "badge-info", "badge-warning", "badge-neutral"}) {
            if (cssClass.contains(variant)) {
                hits++;
            }
        }
        return hits == 1;
    }

    /** Bản ghi bất biến mô tả một phần tử badge đã render. */
    private static final class BadgeSpan {
        final String cssClass;
        final String text;

        BadgeSpan(String cssClass, String text) {
            this.cssClass = cssClass;
            this.text = text;
        }
    }
}

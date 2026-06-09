package com.cinema.hyperCinema.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
 * trong {@code templates/fragments/components.html} (task 3.2) — góc nhìn
 * <b>an toàn escape HTML</b>.
 *
 * <p>Hai fragment {@code _status-badge.html} cũ render nhãn trạng thái qua
 * {@code th:text} (mã hóa thực thể HTML, KHÔNG dùng {@code th:utext}). Fragment
 * hợp nhất mới phải <b>bảo toàn hành vi escape</b> đó khi di trú (Req 5.5): với
 * bất kỳ giá trị {@code status} nào — kể cả chuỗi nhúng các ký tự đặc biệt HTML
 * ({@code < > & " '}) — đầu ra render KHÔNG được chứa thẻ HTML thô bắt nguồn từ
 * input. Mọi ký tự đặc biệt nếu xuất hiện phải ở dạng đã mã hóa thực thể.
 *
 * <p>Test render fragment thật qua một {@link SpringTemplateEngine} standalone
 * (cùng cơ chế với {@code StatusBadgeTotalityPropertyTest} / {@code LayoutFragmentTest}).
 */
class StatusBadgeEscapePropertyTest {

    /** Tên logic của fragment template trên classpath (templates/fragments/components.html). */
    private static final String COMPONENTS = "fragments/components";

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
    // Generator: chuỗi nhúng ký tự đặc biệt HTML (< > & " ') + payload injection
    // ---------------------------------------------------------------------

    private static boolean containsHtmlSpecial(String s) {
        return s.indexOf('<') >= 0 || s.indexOf('>') >= 0 || s.indexOf('&') >= 0
                || s.indexOf('"') >= 0 || s.indexOf('\'') >= 0;
    }

    @Provide
    Arbitrary<String> htmlUnsafeStatuses() {
        // (a) Payload giống các thử nghiệm chèn HTML/injection thực tế.
        Arbitrary<String> payloads = Arbitraries.of(
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>",
                "<b>bold</b>",
                "<a href=\"#\">link</a>",
                "\"><script>evil()</script>",
                "' onmouseover='steal()",
                "</span><span class=\"badge badge-success\">spoof</span>",
                "<",
                ">",
                "&",
                "\"",
                "'",
                "<>",
                "<&>",
                "5 < 3 && 2 > 1",
                "O'Brien & \"Sons\"",
                "<div class=\"x\">",
                "&lt;already-encoded&gt;",
                "NowShowing<script>",
                "Active\" onclick=\"x");

        // (b) Chuỗi ngẫu nhiên từ bộ ký tự giàu metacharacter HTML; bảo đảm có ít
        //     nhất một ký tự đặc biệt để mọi mẫu đều stress nhánh escape.
        Arbitrary<String> randoms = Arbitraries.strings()
                .withChars('<', '>', '&', '"', '\'', '/', '=', ' ', 'a', 'B', '1', ';', ':', '-')
                .ofMinLength(1).ofMaxLength(32)
                .filter(StatusBadgeEscapePropertyTest::containsHtmlSpecial);

        return Arbitraries.oneOf(payloads, randoms);
    }

    // ---------------------------------------------------------------------
    // Property 2
    // ---------------------------------------------------------------------

    // Feature: ui-redesign, Property 2: Status badge HTML escape safety
    /**
     * <b>Property 2: Status badge escape an toàn (bảo toàn hành vi escape khi di trú).</b>
     *
     * <p>Với mọi chuỗi {@code status} nhúng ký tự đặc biệt HTML ({@code < > & " '}),
     * fragment render đúng MỘT badge mà đầu ra KHÔNG chứa thẻ HTML thô bắt nguồn
     * từ input: (a) mọi thẻ trong output chỉ là {@code <span>} của badge (không có
     * thẻ nào bị chèn từ input), và (b) nhãn hiển thị của badge không chứa ký tự
     * {@code <} hoặc {@code >} thô — nghĩa là ký tự đặc biệt đã được mã hóa thực
     * thể. Đây chính là hành vi của {@code th:text} mà di trú phải bảo toàn; nếu
     * fragment đổi sang {@code th:utext}, property này sẽ thất bại.
     *
     * <p><b>Validates: Requirements 5.5</b>
     */
    @Property(tries = 200)
    void statusBadgeNeverEmitsRawHtmlFromInput(@ForAll("htmlUnsafeStatuses") String status) {
        String html = renderBadge(status);

        // (1) WELL-FORMED: render đúng MỘT badge (không bị chèn thêm badge giả).
        int badgeCount = countBadges(html);
        assertEquals(1, badgeCount,
                () -> "statusBadge phải render đúng 1 badge cho status=[" + status
                        + "]; đếm được " + badgeCount + "; html=\n" + html);

        // (2) KHÔNG CHÈN THẺ: mọi thẻ HTML trong output phải là 'span' của badge.
        //     Ký tự '<' thô bắt nguồn từ input (vd. <script>) sẽ tạo ra một thẻ
        //     khác 'span' -> vi phạm. Nội dung đã escape (&lt;script&gt;) không có
        //     '<' thô nên không bị bắt nhầm.
        List<String> tagNames = extractTagNames(html);
        for (String tag : tagNames) {
            assertEquals("span", tag.toLowerCase(Locale.ROOT),
                    () -> "output cho status=[" + status + "] chỉ được chứa thẻ <span> của badge; "
                            + "phát hiện thẻ thô '" + tag + "' (có thể là thẻ HTML bị chèn từ input); html=\n" + html);
        }

        // (3) NHÃN ĐÃ ESCAPE: nội dung hiển thị của badge không chứa '<' hoặc '>'
        //     thô. Nếu th:text bị đổi thành th:utext, ký tự thô từ input sẽ lọt
        //     vào đây và assertion thất bại.
        String inner = extractBadgeInner(html);
        assertFalse(inner.contains("<"),
                () -> "nhãn badge cho status=[" + status + "] không được chứa '<' thô (phải mã hóa thực thể); "
                        + "inner=[" + inner + "]");
        assertFalse(inner.contains(">"),
                () -> "nhãn badge cho status=[" + status + "] không được chứa '>' thô (phải mã hóa thực thể); "
                        + "inner=[" + inner + "]");

        // (4) Badge vẫn mang class nền '.badge' (bảo toàn cấu trúc component).
        BadgeSpan badge = extractBadge(html);
        assertTrue(badge.cssClass.contains("badge"),
                () -> "badge cho status=[" + status + "] phải mang class nền 'badge'; class=" + badge.cssClass);
    }

    // ---------------------------------------------------------------------
    // Tiện ích parse
    // ---------------------------------------------------------------------

    private static final Pattern BADGE_SPAN = Pattern.compile(
            "<span[^>]*class=\"(badge[^\"]*)\"[^>]*>(.*?)</span>", Pattern.DOTALL);

    /** Bắt tên thẻ ngay sau '<' hoặc '</' thô (bỏ qua nội dung đã mã hóa thực thể). */
    private static final Pattern ANY_TAG = Pattern.compile("</?\\s*([a-zA-Z][a-zA-Z0-9-]*)");

    private static int countBadges(String html) {
        Matcher matcher = BADGE_SPAN.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static List<String> extractTagNames(String html) {
        Matcher matcher = ANY_TAG.matcher(html);
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static String extractBadgeInner(String html) {
        Matcher matcher = BADGE_SPAN.matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Không tìm thấy phần tử badge nào trong output:\n" + html);
        }
        return matcher.group(2);
    }

    private static BadgeSpan extractBadge(String html) {
        Matcher matcher = BADGE_SPAN.matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Không tìm thấy phần tử badge nào trong output:\n" + html);
        }
        return new BadgeSpan(matcher.group(1), matcher.group(2));
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

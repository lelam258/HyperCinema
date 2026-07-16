package com.cinema.hyperCinema.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

/**
 * Structural scan test cho Layout_Fragment dùng chung
 * ({@code templates/fragments/layout.html}).
 *
 * <p>Render từng fragment ({@code head(title)}, {@code adminHeader(active)},
 * {@code footer}) bằng một {@link TemplateEngine} standalone cấu hình giống
 * runtime (Thymeleaf 3.1 + web context để resolve {@code @{...}}), rồi assert
 * cấu trúc HTML cốt lõi mà các Page_Template phụ thuộc:
 *
 * <ul>
 *   <li>head: {@code <meta charset>} + meta viewport responsive + link tới
 *       {@code @{/css/theme.css}} (Req 3.1, 3.4, 7.6).</li>
 *   <li>adminHeader: cùng một tập liên kết nav, cùng thứ tự, cùng nhãn, cùng
 *       class trên mọi giá trị {@code active}; chỉ mục đang active được tô đậm
 *       (Req 3.5, 3.6).</li>
 *   <li>footer: render markup nhất quán (Req 3.3).</li>
 * </ul>
 *
 * Test chỉ kiểm tra cấu trúc (presentation/markup), không chạm tới hành vi —
 * khớp tinh thần "structural scan" của task 2.2.
 */
class LayoutFragmentTest {

    /** Tên logic của fragment template trên classpath (templates/fragments/layout.html). */
    private static final String LAYOUT = "fragments/layout";

    private static SpringTemplateEngine engine;

    @BeforeAll
    static void setUpEngine() {
        // Resolver 1: nạp fragment thật từ classpath (src/main/resources/templates/).
        // Giới hạn ở "fragments/*" để mọi template-string wrapper rơi xuống resolver 2.
        ClassLoaderTemplateResolver classpathResolver = new ClassLoaderTemplateResolver();
        classpathResolver.setPrefix("templates/");
        classpathResolver.setSuffix(".html");
        classpathResolver.setTemplateMode(TemplateMode.HTML);
        classpathResolver.setCharacterEncoding("UTF-8");
        classpathResolver.setResolvablePatterns(java.util.Set.of("fragments/*"));
        classpathResolver.setCacheable(true);
        classpathResolver.setOrder(1);

        // Resolver 2: coi chính nội dung chuỗi là template (wrapper gọi th:replace).
        StringTemplateResolver stringResolver = new StringTemplateResolver();
        stringResolver.setTemplateMode(TemplateMode.HTML);
        stringResolver.setOrder(2);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(classpathResolver);
        templateEngine.addTemplateResolver(stringResolver);
        engine = templateEngine;
    }

    // ---------------------------------------------------------------------
    // Hạ tầng render
    // ---------------------------------------------------------------------

    /**
     * Render một lời gọi fragment qua wrapper {@code th:replace}, dưới một web
     * context giả lập (context path rỗng) để {@code @{...}} resolve đúng.
     *
     * @param fragmentExpression ví dụ {@code "fragments/layout :: footer"} hoặc
     *                           {@code "fragments/layout :: head('Tiêu đề')"}
     */
    private static String render(String fragmentExpression) {
        String wrapper = "<div xmlns:th=\"http://www.thymeleaf.org\" "
                + "th:replace=\"~{" + fragmentExpression + "}\"></div>";

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        IWebExchange exchange = application.buildExchange(request, response);

        WebContext context = new WebContext(exchange, Locale.forLanguageTag("vi"));
        return engine.process(wrapper, context);
    }

    private static String renderAdminHeader(String activeExpression) {
        return render(LAYOUT + " :: adminHeader(" + activeExpression + ")");
    }

    // ---------------------------------------------------------------------
    // head(title) — Req 3.1, 3.4, 7.6
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("head(title): có meta charset, meta viewport responsive và link theme.css qua @{...}")
    void headRendersCharsetViewportAndThemeStylesheetLink() {
        String html = render(LAYOUT + " :: head('Tiêu đề Test - HyperCinema')");

        // Req 3.1: meta charset UTF-8
        assertTrue(html.contains("charset=\"UTF-8\""),
                "head fragment phải khai báo <meta charset=\"UTF-8\">; nhận:\n" + html);

        // Req 7.6 / 3.1: meta viewport responsive (giá trị chính xác)
        assertTrue(html.contains("name=\"viewport\""),
                "head fragment phải khai báo meta viewport; nhận:\n" + html);
        assertTrue(html.contains("content=\"width=device-width, initial-scale=1.0\""),
                "meta viewport phải đúng nội dung width=device-width, initial-scale=1.0; nhận:\n" + html);

        // Req 2.2 / 3.1: link theme.css resolve qua cú pháp URL Thymeleaf @{/css/theme.css}
        assertTrue(html.contains("rel=\"stylesheet\""),
                "head fragment phải có <link rel=\"stylesheet\">; nhận:\n" + html);
        assertTrue(html.contains("href=\"/css/theme.css\""),
                "head fragment phải liên kết Theme_Stylesheet qua @{/css/theme.css} "
                        + "(resolve thành /css/theme.css); nhận:\n" + html);
    }

    @Test
    @DisplayName("head(title): tham số title được render vào <title>")
    void headRendersTitleParameter() {
        String html = render(LAYOUT + " :: head('Quản lý Phim - HyperCinema')");

        assertTrue(html.contains("<title>Quản lý Phim - HyperCinema</title>"),
                "head fragment phải render tham số title vào thẻ <title>; nhận:\n" + html);
    }

    // ---------------------------------------------------------------------
    // adminHeader(active) — Req 3.5, 3.6
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("adminHeader: nav có đúng tập link/thứ tự/nhãn/class theo design")
    void adminHeaderRendersExpectedNavLinks() {
        List<NavLink> links = extractNavLinks(renderAdminHeader("'movies'"));

        assertEquals(2, links.size(),
                "Nav admin phải có đúng 2 liên kết (Quản lý Phim, Quản lý Chi nhánh)");

        // Thứ tự + đích đến + nhãn theo design.md §4.1
        assertEquals("/admin/movies", links.get(0).href);
        assertEquals("Quản lý Phim", links.get(0).label);
        assertEquals("/admin/branches", links.get(1).href);
        assertEquals("Quản lý Chi nhánh", links.get(1).label);

        // Cùng base class trên mọi link (Req 3.6)
        for (NavLink link : links) {
            assertTrue(link.cssClasses.contains("admin-nav-link"),
                    "Mỗi nav link phải mang class chung 'admin-nav-link'; nhận: " + link.cssClasses);
        }
    }

    @Test
    @DisplayName("adminHeader: cùng tập link/thứ tự/nhãn/base-class bất kể tham số active (Req 3.6)")
    void adminHeaderNavIsIdenticalAcrossActiveStates() {
        List<List<NavLink>> renders = List.of(
                extractNavLinks(renderAdminHeader("'movies'")),
                extractNavLinks(renderAdminHeader("'branches'")),
                extractNavLinks(renderAdminHeader("'home'")),
                extractNavLinks(renderAdminHeader("null")));

        // Tập "ổn định" (href, label, base-class không kể is-active) phải giống hệt nhau.
        List<String> reference = stableSignature(renders.get(0));
        for (List<NavLink> render : renders) {
            assertEquals(reference, stableSignature(render),
                    "Nav admin phải render cùng tập link/thứ tự/nhãn/class trên mọi trang admin");
        }
    }

    @Test
    @DisplayName("adminHeader: chỉ mục đang active được tô đậm (is-active + aria-current)")
    void adminHeaderHighlightsOnlyActiveItem() {
        // active='movies' → chỉ link movies có is-active + aria-current
        List<NavLink> moviesActive = extractNavLinks(renderAdminHeader("'movies'"));
        assertTrue(hasActiveMarker(moviesActive.get(0)),
                "Khi active='movies', link Quản lý Phim phải được tô đậm");
        assertFalse(hasActiveMarker(moviesActive.get(1)),
                "Khi active='movies', link Quản lý Chi nhánh KHÔNG được tô đậm");
        assertEquals(1, countActive(moviesActive), "Phải có đúng 1 mục active");

        // active='branches' → chỉ link branches có is-active + aria-current
        List<NavLink> branchesActive = extractNavLinks(renderAdminHeader("'branches'"));
        assertFalse(hasActiveMarker(branchesActive.get(0)),
                "Khi active='branches', link Quản lý Phim KHÔNG được tô đậm");
        assertTrue(hasActiveMarker(branchesActive.get(1)),
                "Khi active='branches', link Quản lý Chi nhánh phải được tô đậm");
        assertEquals(1, countActive(branchesActive), "Phải có đúng 1 mục active");

        // active không khớp ('home') → không mục nào active
        assertEquals(0, countActive(extractNavLinks(renderAdminHeader("'home'"))),
                "Khi active không khớp mục nào, không link nào được tô đậm");
    }

    // ---------------------------------------------------------------------
    // footer — Req 3.3
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("footer: render markup footer dùng chung nhất quán")
    void footerRendersConsistentMarkup() {
        String html = render(LAYOUT + " :: footer");

        assertTrue(html.contains("<footer"),
                "footer fragment phải render phần tử <footer>; nhận:\n" + html);
        assertTrue(html.contains("class=\"app-footer\""),
                "footer fragment phải dùng class chung 'app-footer'; nhận:\n" + html);
        assertTrue(html.contains("HyperCinema"),
                "footer fragment phải hiển thị thương hiệu HyperCinema; nhận:\n" + html);
    }

    // ---------------------------------------------------------------------
    // Tiện ích parse nav link
    // ---------------------------------------------------------------------

    private static final Pattern ANCHOR = Pattern.compile("<a\\b([^>]*)>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern HREF = Pattern.compile("href=\"([^\"]*)\"");
    private static final Pattern CLASS = Pattern.compile("class=\"([^\"]*)\"");

    /** Trích các anchor mang class 'admin-nav-link' theo đúng thứ tự xuất hiện. */
    private static List<NavLink> extractNavLinks(String html) {
        List<NavLink> links = new ArrayList<>();
        Matcher anchors = ANCHOR.matcher(html);
        while (anchors.find()) {
            String attrs = anchors.group(1);
            String inner = anchors.group(2).trim();
            if (!attrs.contains("admin-nav-link")) {
                continue;
            }
            String href = group(HREF, attrs);
            String cssClasses = group(CLASS, attrs);
            boolean ariaCurrent = attrs.contains("aria-current=\"page\"");
            links.add(new NavLink(href, inner, cssClasses, ariaCurrent));
        }
        return links;
    }

    private static String group(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : "";
    }

    /** Chữ ký ổn định bỏ qua trạng thái active: href|label|baseClass. */
    private static List<String> stableSignature(List<NavLink> links) {
        List<String> signature = new ArrayList<>();
        for (NavLink link : links) {
            String baseClass = link.cssClasses.replace("is-active", "").trim().replaceAll("\\s+", " ");
            signature.add(link.href + "|" + link.label + "|" + baseClass);
        }
        return signature;
    }

    private static boolean hasActiveMarker(NavLink link) {
        return link.cssClasses.contains("is-active") && link.ariaCurrent;
    }

    private static long countActive(List<NavLink> links) {
        return links.stream().filter(LayoutFragmentTest::hasActiveMarker).count();
    }

    /** Bản ghi bất biến mô tả một liên kết nav admin. */
    private static final class NavLink {
        final String href;
        final String label;
        final String cssClasses;
        final boolean ariaCurrent;

        NavLink(String href, String label, String cssClasses, boolean ariaCurrent) {
            this.href = href;
            this.label = label;
            this.cssClasses = cssClasses;
            this.ariaCurrent = ariaCurrent;
        }
    }
}

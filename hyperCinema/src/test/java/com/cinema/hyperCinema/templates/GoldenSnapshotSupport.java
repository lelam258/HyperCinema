package com.cinema.hyperCinema.templates;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.cinema.hyperCinema.dto.admin.branch.request.AssignManagerRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.AssignStaffRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchCreateRequest;
import com.cinema.hyperCinema.dto.admin.branch.request.BranchSearchCriteria;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchDetailView;
import com.cinema.hyperCinema.dto.admin.branch.response.BranchListItem;
import com.cinema.hyperCinema.dto.admin.branch.response.HallSummary;
import com.cinema.hyperCinema.dto.admin.branch.response.UserSummary;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieCreateRequest;
import com.cinema.hyperCinema.dto.admin.movie.request.MovieSearchCriteria;
import com.cinema.hyperCinema.dto.admin.movie.response.BranchAssignmentSummary;
import com.cinema.hyperCinema.dto.admin.movie.response.GenreSummary;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieDetailView;
import com.cinema.hyperCinema.dto.admin.movie.response.MovieListItem;
import com.cinema.hyperCinema.dto.auth.ForgotPasswordRequestDTO;
import com.cinema.hyperCinema.dto.auth.RegisterRequestDTO;
import com.cinema.hyperCinema.dto.auth.ResetPasswordRequestDTO;

/**
 * Golden snapshot harness (UI Redesign — task 4.1).
 *
 * <p><b>Mục đích.</b> Đây là <i>cổng an toàn</i> chốt baseline hành vi của 16
 * Page_Template <b>trước khi di trú</b> (Testing Strategy §8.2). Lớp này render
 * mỗi trang bằng <b>đúng đường render production của Spring + Thymeleaf</b>
 * ({@link ThymeleafViewResolver} → {@link View#render}) với <b>model fixture cố
 * định</b>, rồi trích một <i>behavior manifest</i> (manifest hành vi) ổn định
 * để so khớp sau khi di trú (task 8.2).
 *
 * <p><b>Vì sao dùng đường render production thay vì engine standalone.</b> Các
 * trang form ({@code movie-form}, {@code branch-form}, {@code auth/*},
 * {@code assign-*}) dùng {@code th:object}/{@code th:field}/{@code th:errors};
 * các xử lý này thuộc Spring dialect và cần một {@code RequestContext} +
 * {@code BindingResult}. {@link ThymeleafViewResolver} tái sử dụng đúng mã
 * Spring đã được kiểm thử để dựng các thứ đó — nhờ vậy {@code th:field},
 * {@code #{...}} (i18n) và {@code @{...}} (URL) đều render <b>giống production</b>
 * mà không cần khởi động toàn bộ {@code @SpringBootTest} (tránh phụ thuộc
 * MySQL/Testcontainers). Đây cũng là tinh thần "MockMvc render utility" của
 * task: dùng hạ tầng MVC mock của Spring để render view với model cố định.
 *
 * <p><b>Manifest gồm những trục bảo toàn hành vi (Req 5.5, 5.6, 6.1–6.3):</b>
 * <ul>
 *   <li>{@code links} — tập href của mọi {@code <a>} (đích điều hướng/sort/phân trang).</li>
 *   <li>{@code forms} — mỗi form: {@code action} + {@code method} + danh sách
 *       field (tên + giá trị/giá trị option đang chọn + nút submit).</li>
 *   <li>{@code i18nKeys} — tập khóa/biểu thức {@code #{...}} trích từ <b>mã
 *       nguồn template</b> (literal {@code #{movie.not_found}} lẫn động
 *       {@code #{${errorKey}}}/{@code #{__${infoKey}__}}).</li>
 *   <li>{@code tables} — mỗi bảng: số cột + danh sách tiêu đề ({@code movie-list}
 *       8 cột, {@code branch-list} 7 cột).</li>
 *   <li>{@code pagination} — cấu trúc phân trang 0-indexed (info + từng control
 *       kèm tham số {@code page}).</li>
 *   <li>{@code title} + {@code text} — toàn bộ chuỗi text hiển thị (đã chuẩn hóa).</li>
 * </ul>
 *
 * <p>Manifest được tuần tự hóa thành JSON xác định (khóa theo thứ tự chèn, tập
 * được sắp xếp) và ghi xuống {@code src/test/resources/golden/&lt;name&gt;.json}.
 */
public final class GoldenSnapshotSupport {

    private GoldenSnapshotSupport() {
    }

    /** Locale tiếng Việt — dự án Vietnamese-first. */
    public static final Locale VI = Locale.forLanguageTag("vi");

    /** Thư mục golden (tương đối module root, nơi `mvn test` chạy). */
    public static final Path GOLDEN_DIR = Paths.get("src", "test", "resources", "golden");

    // =====================================================================
    // Render infrastructure (đường render production của Spring + Thymeleaf)
    // =====================================================================

    private static ThymeleafViewResolver viewResolver;
    private static GenericWebApplicationContext appContext;
    private static MockServletContext servletContext;

    private static synchronized ThymeleafViewResolver viewResolver() {
        if (viewResolver != null) {
            return viewResolver;
        }

        servletContext = new MockServletContext();
        appContext = new GenericWebApplicationContext(servletContext);

        // MessageSource giống Spring Boot: basename "messages", UTF-8, không
        // fallback system locale (khớp application.properties). PHẢI đăng ký
        // TRƯỚC refresh() để AbstractApplicationContext.initMessageSource() dùng
        // chính bean này làm MessageSource của context (nếu đăng ký sau refresh,
        // context đã tự tạo "Empty MessageSource" cùng tên → xung đột).
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        appContext.getBeanFactory().registerSingleton("messageSource", messageSource);

        appContext.refresh();
        // Để RequestContext (do ThymeleafView dựng) tìm thấy WebApplicationContext.
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appContext);

        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(appContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messageSource);

        ThymeleafViewResolver vr = new ThymeleafViewResolver();
        vr.setTemplateEngine(engine);
        vr.setApplicationContext(appContext);
        vr.setCharacterEncoding("UTF-8");
        vr.setCache(false);

        viewResolver = vr;
        return viewResolver;
    }

    /**
     * Render một view name với model + request param cố định, trả HTML hoàn
     * chỉnh (UTF-8). Sử dụng đúng đường render production của Spring.
     */
    public static String render(String viewName, Map<String, Object> model, Map<String, String> params) {
        try {
            ThymeleafViewResolver vr = viewResolver();
            View view = vr.resolveViewName(viewName, VI);
            if (view == null) {
                throw new IllegalStateException("Không resolve được view: " + viewName);
            }

            MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
            request.setMethod("GET");
            request.addPreferredLocale(VI);
            request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, appContext);
            request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new FixedLocaleResolver(VI));
            if (params != null) {
                params.forEach(request::setParameter);
            }

            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setCharacterEncoding("UTF-8");

            view.render(model, request, response);
            return response.getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException("Render thất bại cho view '" + viewName + "': " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // Behavior manifest — trích các trục bảo toàn hành vi từ HTML đã render
    // =====================================================================

    /**
     * Manifest hành vi của một trang — chỉ giữ các trục <i>hành vi</i> (Req 5/6),
     * cố tình bỏ qua class CSS / style nội tuyến / cấu trúc markup trình bày.
     * Dùng {@link LinkedHashMap}/{@link TreeSet}/sorted-list để tuần tự hóa ổn định.
     */
    public static final class PageManifest {
        public String view;
        public String title;
        public List<String> links;            // sorted set of <a href>
        public List<FormManifest> forms;      // theo thứ tự xuất hiện
        public List<String> i18nKeys;         // sorted set, trích từ source template
        public List<TableManifest> tables;    // theo thứ tự xuất hiện
        public PaginationManifest pagination;  // null nếu không có
        public List<String> text;             // chuỗi text hiển thị (theo thứ tự, đã chuẩn hóa)
    }

    public static final class FormManifest {
        public String action;
        public String method;
        public List<String> fields;           // "name=value" / "name[option]=value(selected)" / "submit:label"
    }

    public static final class TableManifest {
        public int columnCount;
        public List<String> headers;
    }

    public static final class PaginationManifest {
        public String info;                   // text rút gọn của .pagination-info
        public List<String> controls;         // nhãn + (page=..) cho từng control
    }

    // ---- Regex tiện ích (HTML đã render) --------------------------------

    private static final Pattern ANCHOR = Pattern.compile("<a\\b([^>]*)>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern FORM = Pattern.compile("<form\\b([^>]*)>(.*?)</form>", Pattern.DOTALL);
    private static final Pattern INPUT = Pattern.compile("<input\\b([^>]*?)/?>", Pattern.DOTALL);
    private static final Pattern BUTTON = Pattern.compile("<button\\b([^>]*)>(.*?)</button>", Pattern.DOTALL);
    private static final Pattern SELECT = Pattern.compile("<select\\b([^>]*)>(.*?)</select>", Pattern.DOTALL);
    private static final Pattern OPTION = Pattern.compile("<option\\b([^>]*)>(.*?)</option>", Pattern.DOTALL);
    private static final Pattern TEXTAREA = Pattern.compile("<textarea\\b([^>]*)>(.*?)</textarea>", Pattern.DOTALL);
    private static final Pattern TABLE = Pattern.compile("<table\\b[^>]*>(.*?)</table>", Pattern.DOTALL);
    private static final Pattern THEAD_TH = Pattern.compile("<th\\b[^>]*>(.*?)</th>", Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<title\\b[^>]*>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern PAGINATION_BAR =
            Pattern.compile("<div class=\"pagination-bar\"[^>]*>(.*?)</div>\\s*</div>", Pattern.DOTALL);

    private static String attr(String attrs, String name) {
        Matcher m = Pattern.compile(name + "=\"([^\"]*)\"").matcher(attrs);
        return m.find() ? m.group(1) : null;
    }

    /** Bỏ thẻ HTML + gộp whitespace để lấy text hiển thị ổn định. */
    private static String textOf(String html) {
        String noTags = html.replaceAll("(?s)<[^>]*>", " ");
        return normalize(noTags);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Trích manifest hành vi từ HTML đã render + mã nguồn template (cho i18n keys).
     */
    public static PageManifest extract(String viewName, String renderedHtml, String templateSource) {
        PageManifest m = new PageManifest();
        m.view = viewName;

        Matcher tm = TITLE.matcher(renderedHtml);
        m.title = tm.find() ? textOf(tm.group(1)) : null;

        m.links = extractLinks(renderedHtml);
        m.forms = extractForms(renderedHtml);
        m.i18nKeys = extractI18nKeys(templateSource);
        m.tables = extractTables(renderedHtml);
        m.pagination = extractPaginationManifest(renderedHtml);
        m.text = extractVisibleText(renderedHtml);
        return m;
    }

    private static List<String> extractLinks(String html) {
        TreeSet<String> hrefs = new TreeSet<>();
        Matcher a = ANCHOR.matcher(html);
        while (a.find()) {
            String href = attr(a.group(1), "href");
            if (href != null) {
                hrefs.add(href);
            }
        }
        return new ArrayList<>(hrefs);
    }

    private static List<FormManifest> extractForms(String html) {
        List<FormManifest> forms = new ArrayList<>();
        Matcher f = FORM.matcher(html);
        while (f.find()) {
            String attrs = f.group(1);
            String body = f.group(2);
            FormManifest fm = new FormManifest();
            fm.action = attr(attrs, "action");
            String method = attr(attrs, "method");
            fm.method = method == null ? "get" : method.toLowerCase(Locale.ROOT);
            fm.fields = new ArrayList<>();

            // inputs (gồm hidden _method=delete...)
            Matcher in = INPUT.matcher(body);
            while (in.find()) {
                String ia = in.group(1);
                String name = attr(ia, "name");
                if (name == null) {
                    continue;
                }
                String type = attr(ia, "type");
                String value = attr(ia, "value");
                boolean checked = ia.contains(" checked");
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                if (type != null) {
                    sb.append('(').append(type).append(')');
                }
                sb.append('=').append(value == null ? "" : value);
                if (checked) {
                    sb.append("[checked]");
                }
                fm.fields.add(sb.toString());
            }

            // selects + option đang chọn
            Matcher se = SELECT.matcher(body);
            while (se.find()) {
                String sa = se.group(1);
                String sBody = se.group(2);
                String name = attr(sa, "name");
                if (name == null) {
                    continue;
                }
                List<String> opts = new ArrayList<>();
                String selectedValue = null;
                Matcher op = OPTION.matcher(sBody);
                while (op.find()) {
                    String oa = op.group(1);
                    String ov = attr(oa, "value");
                    opts.add(ov == null ? "" : ov);
                    if (oa.contains(" selected")) {
                        selectedValue = ov == null ? "" : ov;
                    }
                }
                fm.fields.add("select:" + name + "=options[" + String.join("|", opts)
                        + "];selected=" + (selectedValue == null ? "" : selectedValue));
            }

            // textareas
            Matcher ta = TEXTAREA.matcher(body);
            while (ta.find()) {
                String taa = ta.group(1);
                String name = attr(taa, "name");
                if (name == null) {
                    continue;
                }
                fm.fields.add("textarea:" + name + "=" + normalize(ta.group(2)));
            }

            // submit buttons (nhãn)
            Matcher bt = BUTTON.matcher(body);
            while (bt.find()) {
                String ba = bt.group(1);
                String type = attr(ba, "type");
                if (type == null || "submit".equalsIgnoreCase(type)) {
                    fm.fields.add("submit:" + textOf(bt.group(2)));
                }
            }
            forms.add(fm);
        }
        return forms;
    }

    private static List<TableManifest> extractTables(String html) {
        List<TableManifest> tables = new ArrayList<>();
        Matcher t = TABLE.matcher(html);
        while (t.find()) {
            String body = t.group(1);
            // chỉ lấy header trong <thead> nếu có; nếu không, lấy hàng <th> đầu.
            String headSection = body;
            int theadStart = body.indexOf("<thead");
            if (theadStart >= 0) {
                int theadEnd = body.indexOf("</thead>", theadStart);
                if (theadEnd >= 0) {
                    headSection = body.substring(theadStart, theadEnd);
                }
            }
            List<String> headers = new ArrayList<>();
            Matcher th = THEAD_TH.matcher(headSection);
            while (th.find()) {
                // Bỏ <span class="sort-indicator">▲/▼</span> để header manifest giữ
                // NHÃN CỘT ổn định (không phụ thuộc cột nào đang được sort trong
                // fixture). Trạng thái sort hiện hành đã được nắm qua `links` (href
                // sort + toggle direction), nên không mất thông tin hành vi.
                String cell = th.group(1).replaceAll(
                        "(?s)<span class=\"sort-indicator\"[^>]*>.*?</span>", " ");
                headers.add(textOf(cell));
            }
            if (headers.isEmpty()) {
                continue; // bảng không có header (hiếm) — bỏ qua
            }
            TableManifest tmf = new TableManifest();
            tmf.headers = headers;
            tmf.columnCount = headers.size();
            tables.add(tmf);
        }
        return tables;
    }

    private static PaginationManifest extractPaginationManifest(String html) {
        Matcher pb = PAGINATION_BAR.matcher(html);
        if (!pb.find()) {
            return null;
        }
        String bar = pb.group(0);
        PaginationManifest pm = new PaginationManifest();

        Matcher info = Pattern.compile("<div class=\"pagination-info\">(.*?)</div>", Pattern.DOTALL).matcher(bar);
        pm.info = info.find() ? textOf(info.group(1)) : null;

        pm.controls = new ArrayList<>();
        Matcher ctrlBlock = Pattern.compile(
                "<div class=\"pagination-controls\">(.*?)</div>", Pattern.DOTALL).matcher(bar);
        String controls = ctrlBlock.find() ? ctrlBlock.group(1) : bar;

        // các link (kèm page= param) và span trạng thái (disabled/current)
        Matcher token = Pattern.compile(
                "<a\\b([^>]*)>(.*?)</a>|<span class=\"(page-current|page-disabled)\"[^>]*>(.*?)</span>",
                Pattern.DOTALL).matcher(controls);
        while (token.find()) {
            if (token.group(1) != null) {
                String href = attr(token.group(1), "href");
                String label = textOf(token.group(2));
                String pageParam = "";
                if (href != null) {
                    // href đã render bị HTML-escape (&amp;) nên ký tự ngay trước
                    // "page=" có thể là ';'. Bắt trực tiếp giá trị số (page index
                    // 0-indexed) để không phụ thuộc ký tự phân tách.
                    Matcher pp = Pattern.compile("page=([0-9]+)").matcher(href);
                    if (pp.find()) {
                        pageParam = pp.group(1);
                    }
                }
                pm.controls.add("a:" + label + "(page=" + pageParam + ")");
            } else {
                pm.controls.add(token.group(3) + ":" + textOf(token.group(4)));
            }
        }
        return pm;
    }

    /**
     * Trích các biểu thức i18n {@code #{...}} từ <b>mã nguồn template</b> (không
     * phải HTML đã render). Bao gồm cả khóa tĩnh ({@code #{movie.not_found}}) lẫn
     * khóa động ({@code #{${errorKey}}}, {@code #{__${infoKey}__}}). Tập khóa này
     * phải bất biến qua di trú (Req 5.6, 6.4).
     */
    private static List<String> extractI18nKeys(String templateSource) {
        TreeSet<String> keys = new TreeSet<>();
        if (templateSource != null) {
            // Bỏ HTML comment trước khi quét: #{...} nằm trong <!-- --> chỉ là ví
            // dụ/ghi chú, KHÔNG phải binding i18n thực (Thymeleaf không xử lý
            // comment). Loại bỏ chúng để manifest phản ánh đúng tập khóa được
            // tham chiếu thật (Req 5.6, 6.4) và bền vững khi comment đổi lúc di trú.
            String withoutComments = templateSource.replaceAll("(?s)<!--.*?-->", " ");
            keys.addAll(extractBalancedI18n(withoutComments));
        }
        return new ArrayList<>(keys);
    }

    /** Quét cân bằng ngoặc cho biểu thức {@code #{ ... }} (xử lý {@code ${...}} lồng). */
    private static TreeSet<String> extractBalancedI18n(String src) {
        TreeSet<String> result = new TreeSet<>();
        int i = 0;
        while (i < src.length() - 1) {
            if (src.charAt(i) == '#' && src.charAt(i + 1) == '{') {
                int depth = 1;
                int j = i + 2;
                while (j < src.length() && depth > 0) {
                    char c = src.charAt(j);
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                    }
                    j++;
                }
                if (depth == 0) {
                    result.add(normalize(src.substring(i, j)));
                    i = j;
                    continue;
                }
            }
            i++;
        }
        return result;
    }

    /** Lấy text hiển thị theo thứ tự đọc (chuẩn hóa) từ body, bỏ style/script. */
    private static List<String> extractVisibleText(String html) {
        String body = html;
        int bodyStart = html.indexOf("<body");
        if (bodyStart >= 0) {
            int gt = html.indexOf('>', bodyStart);
            if (gt >= 0) {
                body = html.substring(gt + 1);
            }
        }
        // bỏ style/script
        body = body.replaceAll("(?s)<style\\b[^>]*>.*?</style>", " ");
        body = body.replaceAll("(?s)<script\\b[^>]*>.*?</script>", " ");
        // bỏ comment
        body = body.replaceAll("(?s)<!--.*?-->", " ");

        List<String> out = new ArrayList<>();
        // tách theo thẻ, giữ text node không rỗng
        String[] chunks = body.split("(?s)<[^>]*>");
        for (String chunk : chunks) {
            String t = normalize(chunk);
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    // =====================================================================
    // Đọc mã nguồn template (cho i18n key extraction) + ghi golden JSON
    // =====================================================================

    /** Đọc mã nguồn template gốc từ classpath {@code /templates/<view>.html}. */
    public static String readTemplateSource(String viewName) {
        String resource = "/templates/" + viewName + ".html";
        try (InputStream in = GoldenSnapshotSupport.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy template trên classpath: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Đọc template thất bại: " + resource, e);
        }
    }

    /** Tên file golden cho một view (thay '/' bằng '__'). */
    public static String goldenFileName(String viewName) {
        return viewName.replace('/', '_') + ".json";
    }

    /** Đường dẫn golden cho một view. */
    public static Path goldenPath(String viewName) {
        return GOLDEN_DIR.resolve(goldenFileName(viewName));
    }

    /** Ghi manifest xuống file golden JSON (xác định, UTF-8). */
    public static void writeGolden(PageManifest manifest) {
        try {
            Files.createDirectories(GOLDEN_DIR);
            String json = toJson(manifest);
            Files.write(goldenPath(manifest.view), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Ghi golden thất bại cho " + manifest.view, e);
        }
    }

    /** Render + trích + ghi golden cho một {@link PageSpec}. Trả về manifest. */
    public static PageManifest snapshot(PageSpec spec) {
        String html = render(spec.viewName, spec.model, spec.params);
        String source = readTemplateSource(spec.viewName);
        PageManifest manifest = extract(spec.viewName, html, source);
        writeGolden(manifest);
        return manifest;
    }

    // ---- JSON serializer (xác định, không phụ thuộc thư viện ngoài) ------

    private static String toJson(PageManifest m) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"view\": ").append(jsonStr(m.view)).append(",\n");
        sb.append("  \"title\": ").append(jsonStr(m.title)).append(",\n");
        sb.append("  \"links\": ").append(jsonStrArray(m.links, 1)).append(",\n");
        sb.append("  \"i18nKeys\": ").append(jsonStrArray(m.i18nKeys, 1)).append(",\n");

        // forms
        sb.append("  \"forms\": [");
        for (int i = 0; i < m.forms.size(); i++) {
            FormManifest f = m.forms.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\n");
            sb.append("      \"action\": ").append(jsonStr(f.action)).append(",\n");
            sb.append("      \"method\": ").append(jsonStr(f.method)).append(",\n");
            sb.append("      \"fields\": ").append(jsonStrArray(f.fields, 3)).append("\n");
            sb.append("    }");
        }
        sb.append(m.forms.isEmpty() ? "]" : "\n  ]").append(",\n");

        // tables
        sb.append("  \"tables\": [");
        for (int i = 0; i < m.tables.size(); i++) {
            TableManifest t = m.tables.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\n");
            sb.append("      \"columnCount\": ").append(t.columnCount).append(",\n");
            sb.append("      \"headers\": ").append(jsonStrArray(t.headers, 3)).append("\n");
            sb.append("    }");
        }
        sb.append(m.tables.isEmpty() ? "]" : "\n  ]").append(",\n");

        // pagination
        sb.append("  \"pagination\": ");
        if (m.pagination == null) {
            sb.append("null");
        } else {
            sb.append("{\n");
            sb.append("    \"info\": ").append(jsonStr(m.pagination.info)).append(",\n");
            sb.append("    \"controls\": ").append(jsonStrArray(m.pagination.controls, 2)).append("\n");
            sb.append("  }");
        }
        sb.append(",\n");

        // text
        sb.append("  \"text\": ").append(jsonStrArray(m.text, 1)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String jsonStrArray(List<String> items, int indentLevel) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        String pad = "  ".repeat(indentLevel);
        String inner = "  ".repeat(indentLevel + 1);
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append(inner).append(jsonStr(items.get(i)));
            sb.append(i == items.size() - 1 ? "\n" : ",\n");
        }
        sb.append(pad).append("]");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // =====================================================================
    // Page descriptors + fixtures cố định (16 trang trong phạm vi)
    // =====================================================================

    /** Mô tả một trang để render golden: view name + model + request params. */
    public static final class PageSpec {
        public final String viewName;
        public final Map<String, Object> model;
        public final Map<String, String> params;

        public PageSpec(String viewName, Map<String, Object> model, Map<String, String> params) {
            this.viewName = viewName;
            this.model = model;
            this.params = params;
        }
    }

    /**
     * 16 trang trong phạm vi UI Redesign, với fixture model cố định.
     *
     * <p><b>Giới hạn đã biết — {@code admin/index.html} (task 4.1 note).</b> Trang
     * {@code admin/index.html} <i>đã được di trú</i> ở task 5.1 trong một lần chạy
     * trước, TRƯỚC khi golden baseline này được sinh. Vì vậy manifest golden của
     * trang này phản ánh phiên bản <b>đã di trú</b> (dùng {@code th:replace} cho
     * head/adminHeader/footer + class component dùng chung) chứ không phải phiên
     * bản gốc chưa di trú như 15 trang còn lại.
     *
     * <p>Điều này <b>chấp nhận được</b> đối với vai trò oracle của baseline: manifest
     * chỉ chốt các trục <i>hành vi</i> (links/forms/i18n/text/columns/pagination) —
     * đúng những trục mà di trú phải bảo toàn (Req 5.5, 5.6, 6.1–6.4). Nói cách khác,
     * baseline của {@code admin/index} ghi nhận hành vi <i>hiện tại</i> của trang;
     * {@code GoldenComparisonTest} (task 8.2) sẽ phát hiện mọi thay đổi hành vi
     * <i>sau</i> thời điểm này. Hệ quả: nếu phiên bản di trú 5.1 đã vô tình đổi hành
     * vi so với bản gốc, sai lệch đó sẽ KHÔNG bị baseline này bắt (vì gốc chưa từng
     * được chụp). Đối với 15 trang còn lại (chưa di trú khi sinh baseline), oracle
     * phản ánh đúng hành vi gốc.
     */
    public static List<PageSpec> allPages() {
        List<PageSpec> pages = new ArrayList<>();

        // ---- admin/index (không cần model) ----
        // LƯU Ý: trang này đã được di trú ở task 5.1 (xem javadoc allPages()).
        pages.add(new PageSpec("admin/index", new LinkedHashMap<>(), null));

        // ---- admin/movies/movie-list (Page 8 cột + pagination 0-indexed) ----
        pages.add(new PageSpec("admin/movies/movie-list", movieListModel(), null));

        // ---- admin/movies/movie-detail (readOnly=false, đủ panel) ----
        pages.add(new PageSpec("admin/movies/movie-detail", movieDetailModel(), null));

        // ---- admin/movies/movie-form (mode=create) ----
        pages.add(new PageSpec("admin/movies/movie-form", movieFormModel(), null));

        // ---- admin/movies/assign-branch ----
        pages.add(new PageSpec("admin/movies/assign-branch", assignBranchModel(), null));

        // ---- admin/branches/branch-list (Page 7 cột + pagination) ----
        pages.add(new PageSpec("admin/branches/branch-list", branchListModel(), null));

        // ---- admin/branches/branch-detail (readOnly=false) ----
        pages.add(new PageSpec("admin/branches/branch-detail", branchDetailModel(), null));

        // ---- admin/branches/branch-form (mode=create) ----
        pages.add(new PageSpec("admin/branches/branch-form", branchFormModel(), null));

        // ---- admin/branches/assign-manager ----
        pages.add(new PageSpec("admin/branches/assign-manager", assignManagerModel(), null));

        // ---- admin/branches/assign-staff (đủ model, không placeholder) ----
        pages.add(new PageSpec("admin/branches/assign-staff", assignStaffModel(), null));

        // ---- auth/* ----
        pages.add(new PageSpec("auth/login", new LinkedHashMap<>(), loginParams()));
        pages.add(new PageSpec("auth/register", registerModel(), null));
        pages.add(new PageSpec("auth/forgot-password", forgotPasswordModel(), null));
        pages.add(new PageSpec("auth/reset-password", resetPasswordModel(), null));

        // ---- error/* ----
        pages.add(new PageSpec("error/movie-not-found", movieNotFoundModel(), null));
        pages.add(new PageSpec("error/movie-forbidden", movieForbiddenModel(), null));

        return pages;
    }

    // ---------------------------------------------------------------------
    // Fixtures — giá trị cố định, deterministic (không random, không thời gian thực)
    // ---------------------------------------------------------------------

    private static final LocalDate FIXED_DATE = LocalDate.of(2025, 1, 15);
    private static final LocalDateTime FIXED_DATETIME = LocalDateTime.of(2025, 1, 15, 9, 30);
    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);

    private static Map<String, Object> movieListModel() {
        Map<String, Object> model = new LinkedHashMap<>();

        MovieListItem a = MovieListItem.builder()
                .movieId(101).title("Avengers: Endgame").duration(181)
                .languageName("Tiếng Anh").genreNames(List.of("Hành động", "Phiêu lưu"))
                .releaseDate(FIXED_DATE).status("NowShowing")
                .posterUrl("https://cdn.example.com/poster-101.jpg").createdAt(FIXED_DATETIME)
                .build();
        MovieListItem b = MovieListItem.builder()
                .movieId(102).title("Dune: Part Two").duration(166)
                .languageName("Tiếng Anh").genreNames(List.of("Khoa học viễn tưởng"))
                .releaseDate(FIXED_DATE).status("ComingSoon")
                .posterUrl(null).createdAt(FIXED_DATETIME)
                .build();

        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setKeyword("aveng");
        criteria.setStatus("NowShowing");
        criteria.setSort("title");
        criteria.setDirection("ASC");
        criteria.normalize();

        // Page 0-indexed, 2 trang để pagination render đầy đủ control.
        Page<MovieListItem> page = new PageImpl<>(List.of(a, b), PageRequest.of(0, 10), 12);

        model.put("page", page);
        model.put("criteria", criteria);
        model.put("languages", List.of(
                lang(1, "Tiếng Việt"), lang(2, "Tiếng Anh")));
        model.put("genres", List.of(
                genre(1, "Hành động"), genre(2, "Phiêu lưu")));
        model.put("warningKey", "movie.search.status_ignored");
        model.put("successKey", "movie.create.success");
        return model;
    }

    private static Map<String, Object> movieDetailModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        MovieDetailView movie = MovieDetailView.builder()
                .movieId(101).title("Avengers: Endgame").duration(181)
                .description("Trận chiến cuối cùng chống lại Thanos.")
                .releaseDate(FIXED_DATE).status("NowShowing")
                .posterUrl("https://cdn.example.com/poster-101.jpg")
                .trailerUrl("https://youtu.be/abc123")
                .languageId(2).languageName("Tiếng Anh")
                .genres(List.of(genre(1, "Hành động"), genre(2, "Phiêu lưu")))
                .branches(List.of(BranchAssignmentSummary.builder()
                        .branchId(11).branchName("CGV Vincom").city("Hà Nội")
                        .assignedAt(FIXED_DATETIME).build()))
                .futureShowtimeCount(5).pastShowtimeCount(20)
                .createdAt(FIXED_DATETIME).updatedAt(FIXED_DATETIME)
                .build();
        model.put("movie", movie);
        model.put("readOnly", false);
        model.put("successKey", "movie.status.changed");
        return model;
    }

    private static Map<String, Object> movieFormModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        MovieCreateRequest dto = new MovieCreateRequest();
        dto.setTitle("Phim mới");
        dto.setDuration(120);
        dto.setDescription("Mô tả phim mới.");
        dto.setReleaseDate(FIXED_DATE);
        dto.setLanguageId(2);
        model.put("movie", dto);
        bindEmpty(model, "movie", dto);
        model.put("mode", "create");
        model.put("languages", List.of(lang(1, "Tiếng Việt"), lang(2, "Tiếng Anh")));
        model.put("genres", List.of(genre(1, "Hành động"), genre(2, "Phiêu lưu")));
        return model;
    }

    private static Map<String, Object> assignBranchModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        MovieDetailView movie = MovieDetailView.builder()
                .movieId(101).title("Avengers: Endgame").status("NowShowing").build();
        model.put("movie", movie);
        model.put("candidates", List.of(
                branchListItem(11, "CGV Vincom", "Hà Nội"),
                branchListItem(12, "Lotte Cinema", "Hồ Chí Minh")));
        return model;
    }

    private static Map<String, Object> branchListModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        BranchListItem a = BranchListItem.builder()
                .branchId(11).name("CGV Vincom").city("Hà Nội")
                .address("191 Bà Triệu").phone("0241234567").status("Active")
                .hallCount(8L).createdAt(FIXED_DATETIME).build();
        BranchListItem b = BranchListItem.builder()
                .branchId(12).name("Lotte Cinema").city("Hồ Chí Minh")
                .address("54 Liễu Giai").phone("0289876543").status("Maintenance")
                .hallCount(5L).createdAt(FIXED_DATETIME).build();

        BranchSearchCriteria criteria = new BranchSearchCriteria();
        criteria.setKeyword("cgv");
        criteria.setCity("Hà Nội");
        criteria.setStatus("Active");
        criteria.setSort("name");
        criteria.setDirection("ASC");
        criteria.normalize();

        Page<BranchListItem> page = new PageImpl<>(List.of(a, b), PageRequest.of(0, 10), 12);

        model.put("page", page);
        model.put("criteria", criteria);
        model.put("warningKey", "branch.search.status_ignored");
        return model;
    }

    private static Map<String, Object> branchDetailModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        BranchDetailView branch = BranchDetailView.builder()
                .branchId(11).name("CGV Vincom").address("191 Bà Triệu").city("Hà Nội")
                .phone("0241234567").status("Active")
                .openingTime(OPEN).closingTime(CLOSE).createdAt(FIXED_DATETIME)
                .halls(List.of(HallSummary.builder()
                        .hallId(1).name("Phòng 1").capacity(120).hallType("2D").status("Active").build()))
                .managers(List.of(UserSummary.builder()
                        .userId(21).fullName("Nguyễn Văn A").email("a@example.com")
                        .phone("0901112233").role("Manager").status("Active").build()))
                .staffMembers(List.of(UserSummary.builder()
                        .userId(31).fullName("Trần Thị B").email("b@example.com")
                        .phone("0904445566").role("Staff").status("Active").build()))
                .build();
        model.put("branch", branch);
        model.put("readOnly", false);
        model.put("successKey", "branch.status.changed");
        return model;
    }

    private static Map<String, Object> branchFormModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        BranchCreateRequest dto = new BranchCreateRequest();
        dto.setName("Chi nhánh mới");
        dto.setAddress("123 Đường ABC");
        dto.setCity("Đà Nẵng");
        dto.setPhone("0905123456");
        dto.setOpeningTime(OPEN);
        dto.setClosingTime(CLOSE);
        model.put("branch", dto);
        bindEmpty(model, "branch", dto);
        model.put("mode", "create");
        return model;
    }

    private static Map<String, Object> assignManagerModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        BranchDetailView branch = BranchDetailView.builder()
                .branchId(11).name("CGV Vincom").city("Hà Nội").address("191 Bà Triệu").build();
        model.put("branch", branch);
        model.put("candidates", List.of(
                UserSummary.builder().userId(41).fullName("Lê Văn C").email("c@example.com")
                        .phone("0907778899").role("Manager").status("Active").build(),
                UserSummary.builder().userId(42).fullName("Phạm Thị D").email("d@example.com")
                        .phone(null).role("Manager").status("Active").build()));
        AssignManagerRequest req = new AssignManagerRequest();
        model.put("assignRequest", req);
        bindEmpty(model, "assignRequest", req);
        return model;
    }

    private static Map<String, Object> assignStaffModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        BranchDetailView branch = BranchDetailView.builder()
                .branchId(11).name("CGV Vincom").city("Hà Nội").address("191 Bà Triệu").build();
        model.put("branch", branch);
        model.put("staffCandidates", List.of(
                UserSummary.builder().userId(31).fullName("Trần Thị B").email("b@example.com")
                        .role("Staff").status("Active").build()));
        model.put("managerCandidates", List.of(
                UserSummary.builder().userId(21).fullName("Nguyễn Văn A").email("a@example.com")
                        .role("Manager").status("Active").build()));
        AssignStaffRequest req = new AssignStaffRequest();
        model.put("assignStaffRequest", req);
        bindEmpty(model, "assignStaffRequest", req);
        return model;
    }

    private static Map<String, String> loginParams() {
        // Kích hoạt mọi alert param để text alert được render vào baseline.
        Map<String, String> params = new LinkedHashMap<>();
        params.put("error", "");
        params.put("logout", "");
        params.put("registered", "");
        params.put("resetSuccess", "");
        params.put("activated", "");
        params.put("activationError", "");
        return params;
    }

    private static Map<String, Object> registerModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        RegisterRequestDTO dto = new RegisterRequestDTO();
        model.put("registerDTO", dto);
        bindEmpty(model, "registerDTO", dto);
        return model;
    }

    private static Map<String, Object> forgotPasswordModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        model.put("forgotPasswordDTO", dto);
        bindEmpty(model, "forgotPasswordDTO", dto);
        return model;
    }

    private static Map<String, Object> resetPasswordModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        model.put("resetPasswordDTO", dto);
        bindEmpty(model, "resetPasswordDTO", dto);
        return model;
    }

    private static Map<String, Object> movieNotFoundModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("status", 404);
        model.put("errorKey", "movie.not_found");
        return model;
    }

    private static Map<String, Object> movieForbiddenModel() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("status", 403);
        model.put("errorKey", "movie.access_denied");
        return model;
    }

    // ---- tiện ích fixture ----

    /**
     * Đặt một {@link BindingResult} rỗng cho command object để Thymeleaf
     * ({@code th:object}/{@code th:field}/{@code th:errors}) hoạt động khi render
     * standalone (không qua controller @Valid). Không có lỗi → mọi field render
     * giá trị mặc định, khớp baseline GET form.
     */
    private static void bindEmpty(Map<String, Object> model, String name, Object target) {
        BindingResult br = new BeanPropertyBindingResult(target, name);
        model.put(BindingResult.MODEL_KEY_PREFIX + name, br);
    }

    private static GenreSummary genre(int id, String name) {
        return GenreSummary.builder().genreId(id).name(name).build();
    }

    private static Lang lang(int id, String name) {
        return new Lang(id, name);
    }

    private static BranchListItem branchListItem(int id, String name, String city) {
        return BranchListItem.builder().branchId(id).name(name).city(city).build();
    }

    /**
     * Stand-in nhẹ cho {@code Language} (model JPA) trong fixture dropdown ngôn
     * ngữ: chỉ cần {@code languageId} + {@code name} mà template truy cập, tránh
     * khởi tạo entity JPA nặng. Có getter khớp tên thuộc tính template dùng.
     */
    public static final class Lang {
        private final Integer languageId;
        private final String name;

        public Lang(Integer languageId, String name) {
            this.languageId = languageId;
            this.name = name;
        }

        public Integer getLanguageId() {
            return languageId;
        }

        public String getName() {
            return name;
        }
    }
}

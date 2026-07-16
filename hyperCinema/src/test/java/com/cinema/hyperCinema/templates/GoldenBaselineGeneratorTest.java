package com.cinema.hyperCinema.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cinema.hyperCinema.templates.GoldenSnapshotSupport.PageManifest;
import com.cinema.hyperCinema.templates.GoldenSnapshotSupport.PageSpec;

/**
 * Bộ sinh golden baseline (UI Redesign — task 4.1).
 *
 * <p>Đây là <b>cổng an toàn</b> chạy <i>trước</i> mọi task di trú (5, 7): render
 * 16 Page_Template <b>hiện tại (chưa di trú)</b> qua {@link GoldenSnapshotSupport}
 * với fixture model cố định, trích "behavior manifest" và ghi xuống
 * {@code src/test/resources/golden/}. Các file golden này đóng vai trò
 * <i>oracle trước-di-trú</i> cho {@code GoldenComparisonTest} (task 8.2) — nhờ
 * vậy mọi sai lệch về href/action/method/field/i18n/cột/pagination sau khi di
 * trú sẽ bị phát hiện (Req 5.5, 5.6, 6.1, 6.2, 6.3, 6.4).
 *
 * <p>Test cũng kiểm tra một số <b>bất biến cốt lõi</b> của baseline gốc ngay khi
 * sinh: movie-list 8 cột, branch-list 7 cột, pagination 0-indexed, tập i18n key
 * không rỗng cho các trang flash/error.
 */
class GoldenBaselineGeneratorTest {

    @Test
    @DisplayName("Sinh golden baseline cho 16 trang gốc và kiểm tra bất biến cốt lõi")
    void generateGoldenBaselineForAllPages() {
        List<PageSpec> pages = GoldenSnapshotSupport.allPages();
        assertEquals(16, pages.size(), "Phải có đúng 16 trang trong phạm vi UI Redesign");

        for (PageSpec spec : pages) {
            PageManifest manifest = GoldenSnapshotSupport.snapshot(spec);

            // File golden được ghi ra đĩa
            Path golden = GoldenSnapshotSupport.goldenPath(spec.viewName);
            assertTrue(Files.exists(golden),
                    "Golden phải được ghi cho trang: " + spec.viewName + " (" + golden + ")");

            // Manifest hợp lệ tối thiểu
            assertNotNull(manifest, "Manifest không được null cho " + spec.viewName);
            assertNotNull(manifest.links, "links không được null cho " + spec.viewName);
            assertNotNull(manifest.forms, "forms không được null cho " + spec.viewName);
            assertNotNull(manifest.i18nKeys, "i18nKeys không được null cho " + spec.viewName);
            assertNotNull(manifest.text, "text không được null cho " + spec.viewName);
            assertFalse(manifest.text.isEmpty(), "text hiển thị không được rỗng cho " + spec.viewName);
        }
    }

    @Test
    @DisplayName("movie-list: bảng 8 cột + pagination 0-indexed được chốt vào golden (Req 6.3)")
    void movieListBaselineHasEightColumnsAndZeroIndexedPagination() {
        PageSpec spec = findPage("admin/movies/movie-list");
        PageManifest m = GoldenSnapshotSupport.snapshot(spec);

        assertEquals(1, m.tables.size(), "movie-list phải có đúng 1 bảng danh sách");
        assertEquals(8, m.tables.get(0).columnCount,
                "movie-list phải giữ 8 cột (Req 6.3); headers=" + m.tables.get(0).headers);
        assertEquals(
                List.of("Poster", "Tên phim", "Thời lượng", "Ngôn ngữ", "Thể loại",
                        "Ngày phát hành", "Trạng thái", "Ngày tạo"),
                m.tables.get(0).headers,
                "Thứ tự + tiêu đề cột movie-list phải khớp baseline gốc");

        assertNotNull(m.pagination, "movie-list phải có thanh phân trang");
        // page=0 hiện tại (page-current), link "Sau ›" trỏ page=1 (0-indexed)
        assertTrue(m.pagination.controls.stream().anyMatch(c -> c.startsWith("page-current:1")),
                "Trang hiện tại (0-indexed number=0) hiển thị nhãn '1'; controls=" + m.pagination.controls);
        assertTrue(m.pagination.controls.stream().anyMatch(c -> c.contains("(page=1)")),
                "Phải có control trỏ tới page=1 (0-indexed); controls=" + m.pagination.controls);
    }

    @Test
    @DisplayName("branch-list: bảng 7 cột được chốt vào golden (Req 6.1)")
    void branchListBaselineHasSevenColumns() {
        PageSpec spec = findPage("admin/branches/branch-list");
        PageManifest m = GoldenSnapshotSupport.snapshot(spec);

        assertEquals(1, m.tables.size(), "branch-list phải có đúng 1 bảng danh sách");
        assertEquals(7, m.tables.get(0).columnCount,
                "branch-list phải giữ 7 cột; headers=" + m.tables.get(0).headers);
        assertEquals(
                List.of("Tên chi nhánh", "Thành phố", "Địa chỉ", "Số điện thoại",
                        "Trạng thái", "Số phòng chiếu", "Ngày tạo"),
                m.tables.get(0).headers,
                "Thứ tự + tiêu đề cột branch-list phải khớp baseline gốc");
    }

    @Test
    @DisplayName("movie-form: action + method + field bind được chốt vào golden (Req 6.2)")
    void movieFormBaselineCapturesFormContract() {
        PageSpec spec = findPage("admin/movies/movie-form");
        PageManifest m = GoldenSnapshotSupport.snapshot(spec);

        assertEquals(1, m.forms.size(), "movie-form phải có đúng 1 form");
        assertEquals("post", m.forms.get(0).method, "movie-form (create) phải POST");
        assertEquals("/admin/movies", m.forms.get(0).action,
                "movie-form create phải POST tới /admin/movies");
        // các field then chốt phải có mặt
        List<String> fields = m.forms.get(0).fields;
        assertTrue(fields.stream().anyMatch(f -> f.startsWith("title")),
                "form phải bind field 'title'; fields=" + fields);
        assertTrue(fields.stream().anyMatch(f -> f.startsWith("genreIds")),
                "form phải bind checkbox 'genreIds'; fields=" + fields);
    }

    @Test
    @DisplayName("error/movie-not-found: i18n key + status được chốt vào golden (Req 6.4)")
    void errorPageBaselineCapturesI18nKeys() {
        PageSpec spec = findPage("error/movie-not-found");
        PageManifest m = GoldenSnapshotSupport.snapshot(spec);

        assertFalse(m.i18nKeys.isEmpty(),
                "error/movie-not-found phải tham chiếu ít nhất một khóa #{...}; i18nKeys=" + m.i18nKeys);
        assertTrue(m.text.stream().anyMatch(t -> t.contains("404")),
                "Trang lỗi phải hiển thị mã trạng thái 404; text=" + m.text);
    }

    private static PageSpec findPage(String view) {
        return GoldenSnapshotSupport.allPages().stream()
                .filter(p -> p.viewName.equals(view))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không tìm thấy page spec: " + view));
    }
}

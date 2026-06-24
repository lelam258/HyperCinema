(function () {
    const form = document.querySelector("[data-movie-form]");
    if (!form) {
        return;
    }

    const fileInput = form.querySelector("[data-movie-poster-input]");
    const posterUrl = form.querySelector("[data-movie-poster-url]");
    const statusText = form.querySelector("[data-movie-poster-status]");
    const previewSlot = form.querySelector(".hc-movie-poster-preview");
    const submitButtons = Array.from(form.querySelectorAll('button[type="submit"]'));
    let uploading = false;

    const messages = {
        "movie.poster_file.invalid": "File poster phải là ảnh hợp lệ.",
        "movie.poster_file.too_large": "File poster không được vượt quá 10MB.",
        "movie.poster_upload_failed": "Không thể upload poster lên Cloudinary. Vui lòng thử lại."
    };

    const renderPreview = (src) => {
        if (!previewSlot) {
            return;
        }

        const trimmed = (src || "").trim();
        if (!trimmed) {
            previewSlot.innerHTML = [
                '<div class="hc-movie-poster-placeholder" data-movie-poster-empty>',
                '<i data-lucide="image"></i>',
                "<span>Poster</span>",
                "</div>"
            ].join("");
            if (window.lucide) {
                window.lucide.createIcons();
            }
            return;
        }

        let image = previewSlot.querySelector("img");
        if (!image) {
            previewSlot.innerHTML = '<img alt="Poster phim" data-movie-poster-preview>';
            image = previewSlot.querySelector("img");
        }
        image.src = trimmed;
    };

    const setStatus = (message, tone) => {
        if (!statusText) {
            return;
        }
        statusText.textContent = message || "";
        statusText.dataset.tone = tone || "";
    };

    const setUploading = (value) => {
        uploading = value;
        submitButtons.forEach((button) => {
            button.disabled = value;
            button.classList.toggle("is-disabled", value);
        });
    };

    const csrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    };

    const uploadPoster = async (file) => {
        const uploadUrl = form.dataset.posterUploadUrl;
        if (!uploadUrl) {
            return;
        }

        const body = new FormData();
        body.append("posterFile", file);

        setUploading(true);
        setStatus("Đang upload poster...", "info");

        try {
            const response = await fetch(uploadUrl, {
                method: "POST",
                headers: csrfHeaders(),
                body
            });
            const result = await response.json().catch(() => ({}));

            if (!response.ok || !result.url) {
                const fallback = messages[result.errorKey] || messages["movie.poster_upload_failed"];
                setStatus(fallback, "error");
                return;
            }

            posterUrl.value = result.url;
            fileInput.value = "";
            renderPreview(result.url);
            setStatus("Poster đã upload lên Cloudinary.", "success");
        } catch (error) {
            setStatus(messages["movie.poster_upload_failed"], "error");
        } finally {
            setUploading(false);
        }
    };

    if (fileInput) {
        fileInput.addEventListener("change", () => {
            const file = fileInput.files && fileInput.files[0];
            if (!file) {
                renderPreview(posterUrl ? posterUrl.value : "");
                setStatus("", "");
                return;
            }

            renderPreview(URL.createObjectURL(file));
            uploadPoster(file);
        });
    }

    form.addEventListener("submit", (event) => {
        if (uploading) {
            event.preventDefault();
            setStatus("Vui lòng chờ poster upload xong.", "info");
        }
    });
})();

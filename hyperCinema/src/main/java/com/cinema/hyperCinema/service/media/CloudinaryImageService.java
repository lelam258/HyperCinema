package com.cinema.hyperCinema.service.media;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final Cloudinary cloudinary;

    public String uploadMoviePoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        validateImage(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "hypercinema/movies",
                    "resource_type", "image",
                    "use_filename", true,
                    "unique_filename", true));
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || !StringUtils.hasText(secureUrl.toString())) {
                throw new IllegalStateException("Cloudinary did not return a secure URL.");
            }
            return secureUrl.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read poster image before upload.", ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Cannot upload poster image to Cloudinary.", ex);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("movie.poster_file.too_large");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("movie.poster_file.invalid");
        }
    }
}

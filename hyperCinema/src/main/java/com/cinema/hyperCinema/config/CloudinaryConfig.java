package com.cinema.hyperCinema.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {

    @Bean
    @Conditional(CloudinaryConfiguredCondition.class)
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret()
        ));
    }

    static class CloudinaryConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return hasText(context, "cloudinary.cloud-name")
                    && hasText(context, "cloudinary.api-key")
                    && hasText(context, "cloudinary.api-secret");
        }

        private boolean hasText(ConditionContext context, String propertyName) {
            return StringUtils.hasText(context.getEnvironment().getProperty(propertyName));
        }
    }
}

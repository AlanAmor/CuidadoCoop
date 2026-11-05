package com.cooperativa.cuidado.web;

import com.cooperativa.cuidado.web.seguridad.SeguridadInterceptor;
import jakarta.servlet.SessionTrackingMode;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;
import java.util.Set;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final SeguridadInterceptor seguridadInterceptor;

    public WebConfiguration(SeguridadInterceptor seguridadInterceptor) {
        this.seguridadInterceptor = seguridadInterceptor;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.forLanguageTag("es-AR"));
    }

    @Bean
    public ServletContextInitializer sessionTrackingInitializer() {
        return servletContext -> servletContext.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(seguridadInterceptor);
    }
}

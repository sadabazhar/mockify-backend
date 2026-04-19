package com.mockify.backend.config;

import com.mockify.backend.security.MockifyPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * Registers {@link MockifyPermissionEvaluator} as the application's
 * {@code PermissionEvaluator}, making it the target of every
 * {@code hasPermission(...)} call in {@code @PreAuthorize} and
 * {@code @PostAuthorize} expressions.
 *
 * <p>{@code @EnableMethodSecurity} is already declared on {@link SecurityConfig},
 * so Spring's method security AOP proxy is active. This bean customises the
 * expression handler it uses — nothing else needs to change in SecurityConfig.</p>
 *
 * <p>How Spring routes a {@code hasPermission} call:</p>
 * <pre>
 *   hasPermission(targetId, targetType, permission)
 *       → PermissionEvaluator.hasPermission(auth, targetId, targetType, permission)
 *
 *   hasPermission(domainObject, permission)
 *       → PermissionEvaluator.hasPermission(auth, domainObject, permission)
 * </pre>
 * <p>Only the first form is used in this project — see {@link MockifyPermissionEvaluator}.</p>
 */
@Configuration
@RequiredArgsConstructor
public class MethodSecurityConfig {

    private final MockifyPermissionEvaluator permissionEvaluator;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler =
                new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
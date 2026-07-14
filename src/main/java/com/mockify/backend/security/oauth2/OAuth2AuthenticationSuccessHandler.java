package com.mockify.backend.security.oauth2;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;

    /**
     Called when Google login is successful.
     * Responsible for:
      - creating user (if first time)
      - generating JWT access + refresh tokens
     - returning JSON response to frontend
     */

    @Value("${app.frontend.url}")
    private String FRONTEND_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        //Remove after debugging
        log.info("OAuth Success Handler Called");

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");

        //Remove after debugging
        log.info("Email: {}", email);

        String name = oauthUser.getAttribute("name");
        String providerId = oauthUser.getAttribute("sub");
        String givenName = oauthUser.getAttribute("given_name");
        String familyName = oauthUser.getAttribute("family_name");
        String picture = oauthUser.getAttribute("picture");

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by provider");
            return;
        }

        User user = findOrCreateOAuthUser(email, name, providerId, givenName, familyName, picture);

        //Remove after debugging
        log.info("user details: {}", user);

        // Issue JWTs
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());

        ResponseCookie refreshCookie = cookieUtil.createRefreshToken(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // TODO: SECURITY - Set token in HttpOnly cookie instead of URL params
        String redirectUrl = String.format(
                "%s/oauth2/redirect?access_token=%s&expires_in=%d",
                FRONTEND_URL,
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
                jwtTokenProvider.getAccessTokenExpiration()
        );
        response.sendRedirect(redirectUrl);
    }

    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String providerId, String givenName, String familyName, String picture) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setEmailVerified(true);
            newUser.setRole(UserRole.USER);
            newUser.setName(name != null ? name : email);
            newUser.setProviderName("google");
            newUser.setProviderId(providerId);
            newUser.setFirstName(givenName);
            newUser.setLastName(familyName);
            newUser.setAvatarUrl(picture);
            newUser.setPassword(null);
            newUser.setUsername(email.split("@")[0].toLowerCase());

            return userRepository.save(newUser);
        });
    }
}
package com.mockify.backend.security.oauth2;

import com.mockify.backend.model.User;
import com.mockify.backend.security.CookieUtil;
import com.mockify.backend.security.JwtTokenProvider;
import com.mockify.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles successful OAuth2 authentication.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Retrieve the authenticated OAuth user.</li>
 *     <li>Find or create the corresponding application user.</li>
 *     <li>Generate JWT access and refresh tokens.</li>
 *     <li>Store the refresh token in an HTTP-only cookie.</li>
 *     <li>Redirect the user back to the frontend with the access token.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final AuthService authService;

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

        // Extract normalized user information from the OAuth provider.
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oauthUser.getAttributes());

        // Ensure the provider returned an email address.
        if (userInfo.getEmail() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Email not provided by provider");
            return;
        }

        User user = authService.findOrCreateOAuthUser("google", userInfo);

        // Issue JWTs
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getRole());

        ResponseCookie refreshCookie = cookieUtil.createRefreshToken(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String redirectUrl = String.format(
                "%s/oauth2/redirect?access_token=%s&expires_in=%d",
                FRONTEND_URL,
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
                jwtTokenProvider.getAccessTokenExpiration()
        );
        response.sendRedirect(redirectUrl);
    }

}
package com.fleebug.corerouter.security.oauth2;

import com.fleebug.corerouter.dto.user.response.AuthResponse;
import com.fleebug.corerouter.service.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;

    @Value("${app.oauth2.redirect-success:https://corerouter.me/auth/callback}")
    private String redirectSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth2 authentication");
            return;
        }

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, oauthToken.getName());
        if (client == null || client.getAccessToken() == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 access token not available");
            return;
        }

        String providerAccessToken = client.getAccessToken().getTokenValue();
        AuthResponse authResponse;

        if ("google".equalsIgnoreCase(registrationId)) {
            authResponse = userService.loginWithGoogle(providerAccessToken);
        } else if ("github".equalsIgnoreCase(registrationId)) {
            authResponse = userService.loginWithGithub(providerAccessToken);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported OAuth provider");
            return;
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString(redirectSuccessUrl)
                .queryParam("status", "success")
                .queryParam("provider", registrationId)
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("expiresIn", authResponse.getExpiresIn())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}

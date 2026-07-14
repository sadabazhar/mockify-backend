package com.mockify.backend.security.oauth2;

import java.util.Map;

public interface OAuth2UserInfo {

    String getId();

    String getEmail();

    String getName();

    String getFirstName();

    String getLastName();

    String getImageUrl();

    Map<String, Object> getAttributes();
}

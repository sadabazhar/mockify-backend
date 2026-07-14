package com.mockify.backend.security.oauth2;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return attributes.get("sub") != null ? attributes.get("sub").toString() : null;
    }

    @Override
    public String getEmail() {
        return attributes.get("email") != null ? attributes.get("email").toString() : null;
    }

    @Override
    public String getName() {
        Object name = attributes.get("name");
        if (name != null) return name.toString();
        // fallback
        String givenName = (String) attributes.getOrDefault("given_name", "");
        String familyName = (String) attributes.getOrDefault("family_name", "");
        return (givenName + " " + familyName).trim();
    }

    @Override
    public String getFirstName() {
        return (String) attributes.get("given_name");
    }

    @Override
    public String getLastName() {
        return (String) attributes.get("family_name");
    }

    @Override
    public String getImageUrl() {
        return attributes.get("picture") != null ? attributes.get("picture").toString() : null;
    }
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}


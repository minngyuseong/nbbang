package com.nbbang.security;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nbbang.domain.User;
import com.nbbang.repository.UserRepository;

@Service
public class OAuthUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserRepository users;

    public OAuthUserService(UserRepository users) { this.users = users; }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User source = delegate.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId();
        Profile profile = "kakao".equals(provider) ? kakao(source.getAttributes()) : google(source.getAttributes());
        User user = users.findByProviderAndProviderUserId(provider, profile.id())
                .map(existing -> { existing.updateProfile(profile.email(), profile.name(), profile.image()); return existing; })
                .orElseGet(() -> users.save(new User(provider, profile.id(), profile.email(), profile.name(), profile.image())));
        return new NbbangPrincipal(user.getId(), profile.name(), source.getAttributes(), source.getAuthorities());
    }

    private Profile google(Map<String, Object> attributes) {
        return new Profile(text(attributes.get("sub")), text(attributes.get("email")),
                fallback(text(attributes.get("name")), "사용자"), text(attributes.get("picture")));
    }

    @SuppressWarnings("unchecked")
    private Profile kakao(Map<String, Object> attributes) {
        Map<String, Object> account = map(attributes.get("kakao_account"));
        Map<String, Object> profile = map(account.get("profile"));
        return new Profile(text(attributes.get("id")), text(account.get("email")),
                fallback(text(profile.get("nickname")), "사용자"), text(profile.get("profile_image_url")));
    }

    private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(); }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }
    private String fallback(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private record Profile(String id, String email, String name, String image) {}
}


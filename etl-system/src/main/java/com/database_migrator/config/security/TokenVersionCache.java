package com.database_migrator.config.security;

import com.database_migrator.domain.auth.model.User;
import com.database_migrator.domain.auth.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenVersionCache {

    private final UserRepository userRepository;

    private final Cache<String, Long> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .recordStats()
            .build();

    public Long getTokenVersion(String email) {
        Long cachedVersion = cache.getIfPresent(email);

        if (cachedVersion != null) {
            log.debug("Token version cache HIT for user: {}", email);
            return cachedVersion;
        }

        log.debug("Token version cache MISS for user: {}", email);
        Long version = loadTokenVersionFromDb(email);
        if (version != null) {
            cache.put(email, version);
        }
        return version != null ? version : 0L;
    }

    public void invalidate(String email) {
        cache.invalidate(email);
        log.debug("Token version cache invalidated for user: {}", email);
    }

    private Long loadTokenVersionFromDb(String email) {
        return userRepository.findByEmail(email)
                .map(User::getTokenVersion)
                .orElse(null);
    }
}

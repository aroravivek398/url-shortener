package com.url.shortener.repository;

import com.url.shortener.entity.UrlMapping;
import com.url.shortener.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
        Optional<UrlMapping> findByShortUrl(String shortUrl);
        List<UrlMapping> findByUser(User user);
}
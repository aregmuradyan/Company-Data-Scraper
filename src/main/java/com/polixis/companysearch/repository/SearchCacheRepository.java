package com.polixis.companysearch.repository;

import com.polixis.companysearch.model.SearchCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCacheRepository extends JpaRepository<SearchCache, String> {
}
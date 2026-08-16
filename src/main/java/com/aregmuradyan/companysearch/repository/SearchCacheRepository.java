package com.aregmuradyan.companysearch.repository;

import com.aregmuradyan.companysearch.model.SearchCache;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchCacheRepository extends JpaRepository<SearchCache, String> {
}
package com.polixis.companysearch.repository;

import com.polixis.companysearch.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, String> {
}
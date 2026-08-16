package com.aregmuradyan.companysearch.repository;

import com.aregmuradyan.companysearch.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, String> {
}
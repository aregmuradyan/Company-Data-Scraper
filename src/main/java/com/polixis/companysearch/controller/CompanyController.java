package com.polixis.companysearch.controller;

import com.polixis.companysearch.model.Company;
import com.polixis.companysearch.service.CompanySearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class CompanyController {

    private final CompanySearchService companySearchService;

    public CompanyController(CompanySearchService companySearchService) {
        this.companySearchService = companySearchService;
    }

    @GetMapping("/api/companies/search")
    public String searchCompanies(@RequestParam String query) throws IOException {
        return companySearchService.search(query);
    }
}
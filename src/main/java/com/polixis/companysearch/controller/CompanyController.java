package com.polixis.companysearch.controller;

import com.polixis.companysearch.model.Company;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompanyController {

    @GetMapping("/api/companies/search")
    public Company searchCompanies(@RequestParam String query) {

        Company company = new Company(
                "12345678",
                "HSBC UK BANK PLC",
                "Active"
        );

        return company;
    }
}
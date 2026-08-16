package com.polixis.companysearch;

import com.polixis.companysearch.model.Company;
import com.polixis.companysearch.model.SearchCache;
import com.polixis.companysearch.repository.CompanyRepository;
import com.polixis.companysearch.repository.SearchCacheRepository;
import com.polixis.companysearch.service.CompanySearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CompanyDataScraperApplicationTests {
    @Autowired
    private CompanySearchService companySearchService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SearchCacheRepository searchCacheRepository;

	@Test
	void contextLoads() {
	}

    @Test
    void shouldReturnCachedCompanies() throws Exception {

        Company company = new Company(
                "12345678",
                "TEST COMPANY LTD",
                "Active",
                "Private limited Company",
                "1 January 2026",
                "Test Address",
                new ArrayList<>(),
                new ArrayList<>()
        );

        companyRepository.save(company);

        SearchCache cache = new SearchCache(
                "test-company",
                LocalDateTime.now(),
                List.of(company)
        );

        searchCacheRepository.save(cache);

        List<Company> result =
                companySearchService.search("test-company", false);

        assertEquals(1, result.size());
        assertEquals("12345678", result.get(0).getCompanyNumber());
        assertEquals("TEST COMPANY LTD", result.get(0).getCompanyName());
    }
}

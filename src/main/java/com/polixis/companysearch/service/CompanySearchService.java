package com.polixis.companysearch.service;

import com.polixis.companysearch.model.Company;
import com.polixis.companysearch.model.Officer;
import com.polixis.companysearch.model.Psc;
import com.polixis.companysearch.model.SearchCache;
import com.polixis.companysearch.repository.CompanyRepository;
import com.polixis.companysearch.repository.SearchCacheRepository;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanySearchService {
    private static final long REQUEST_DELAY_MS = 500;
    private static final int MAX_COMPANIES = 100;
    private static final int CACHE_HOURS = 24;
    private static final String USER_AGENT =
            "Polixis-Company-Search-Internship-Task/1.0 | (educational scraper; contact: aregmuradyan.dev@gmail.com)";
    private static final String BASE_URL =
            "https://find-and-update.company-information.service.gov.uk";
    private final CompanyRepository companyRepository;
    private final SearchCacheRepository searchCacheRepository;

    public CompanySearchService(CompanyRepository companyRepository, SearchCacheRepository searchCacheRepository) {
        this.companyRepository = companyRepository;
        this.searchCacheRepository = searchCacheRepository;
    }

    public List<Company> search(String query, boolean forceRefresh) throws IOException, InterruptedException {
        var cachedSearch = searchCacheRepository.findById(query);
        if (!forceRefresh && cachedSearch.isPresent()) {
            SearchCache cache = cachedSearch.get();

            if (cache.getCachedAt().isAfter(LocalDateTime.now().minusHours(CACHE_HOURS))) {
                return cache.getCompanies();
            }
        }
        List<Company> companies = new ArrayList<>();

        String nextPageUrl = BASE_URL + "/search/companies?q=" + query;

        while (nextPageUrl != null && companies.size() < MAX_COMPANIES) {
            Thread.sleep(REQUEST_DELAY_MS);
            Document document = Jsoup.connect(nextPageUrl)
                    .userAgent(USER_AGENT)
                    .get();
            Elements results = document.select("li.type-company");

            for (Element result : results) {
                if (companies.size() >= MAX_COMPANIES) {
                    break;
                }

                try {
                    Element link = result.selectFirst("h3 a");

                    if (link != null) {
                        String companyName = link.text();
                        String companyUrl = link.attr("href");

                        String fullCompanyUrl =
                                BASE_URL + companyUrl;

                        String officersUrl = fullCompanyUrl + "/officers";

                        String pscUrl = fullCompanyUrl + "/persons-with-significant-control";

                        //COMPANY
                        Thread.sleep(REQUEST_DELAY_MS);
                        Document companyDocument = Jsoup.connect(fullCompanyUrl)
                                .userAgent(USER_AGENT)
                                .get();

                        Element statusElement = companyDocument.selectFirst("#company-status");
                        String status = statusElement != null ? statusElement.text() : null;

                        Element addressElement = companyDocument.selectFirst("#roa-address");
                        String address = addressElement != null ? addressElement.text() : null;

                        Element companyTypeElement = companyDocument.selectFirst("#company-type-value");
                        String companyType = companyTypeElement != null ? companyTypeElement.text() : null;

                        Element creationDateElement = companyDocument.selectFirst("#company-creation-date");
                        String creationDate = creationDateElement != null ? creationDateElement.text() : null;

                        Element meta = result.selectFirst("p.meta.crumbtrail");
                        String companyNumber = "";

                        if (meta != null) {
                            String metaText = meta.text();
                            companyNumber = metaText.split(" - ")[0];
                        }

                        //OFFICERS
                        Thread.sleep(REQUEST_DELAY_MS);
                        Document officersDocument = Jsoup.connect(officersUrl)
                                .userAgent(USER_AGENT)
                                .get();
                        Elements appointments = officersDocument.select(".appointments-list > div");
                        List<Officer> officers = new ArrayList<>();
                        for (Element appointment : appointments) {

                            Element nameElement = appointment.selectFirst("[id^=officer-name-]");
                            Element roleElement = appointment.selectFirst("[id^=officer-role-]");
                            Element appointedElement = appointment.selectFirst("[id^=officer-appointed-on-]");

                            String name = nameElement != null ? nameElement.text() : null;
                            String role = roleElement != null ? roleElement.text() : null;
                            String appointedOn = appointedElement != null ? appointedElement.text() : null;

                            Officer officer = new Officer(name, role, appointedOn);

                            officers.add(officer);

                        }

                        //PSCs
                        List<Psc> pscs = new ArrayList<>();
                        try {
                            Thread.sleep(REQUEST_DELAY_MS);
                            Document pscDocument = Jsoup.connect(pscUrl)
                                    .userAgent(USER_AGENT)
                                    .get();

                            Elements pscElements = pscDocument.select(".appointments-list > div");

                            for (Element pscElement : pscElements) {
                                Element nameElement = pscElement.selectFirst("[id^=psc-name-]");
                                Element natureOfControlElement = pscElement.selectFirst("[id^=psc-noc-]");

                                String name = nameElement != null ? nameElement.text() : null;
                                String natureOfControl = natureOfControlElement != null
                                        ? natureOfControlElement.text()
                                        : null;

                                pscs.add(new Psc(name, natureOfControl));
                            }
                        } catch (HttpStatusException e) {
                            System.out.println(
                                    "PSC unavailable for " + companyNumber +
                                            " - HTTP " + e.getStatusCode()
                            );
                        }

                        Company company = new Company(
                                companyNumber,
                                companyName,
                                status,
                                companyType,
                                creationDate,
                                address,
                                officers,
                                pscs
                        );
                        companies.add(company);
                    }
                } catch (HttpStatusException e) {
                        System.out.println("Skipping company due to HTTP " + e.getStatusCode());
                    }
            }
            Element nextPageElement = document.selectFirst("#next-page");
            if (nextPageElement != null) {
                nextPageUrl = BASE_URL + nextPageElement.attr("href");
            } else {
                nextPageUrl = null;
            }
        }
        companyRepository.saveAll(companies);
        SearchCache searchCache = new SearchCache(
                query,
                LocalDateTime.now(),
                companies
        );

        searchCacheRepository.save(searchCache);
        return companies;
    }
}

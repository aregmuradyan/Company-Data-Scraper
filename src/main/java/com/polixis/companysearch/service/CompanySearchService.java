package com.polixis.companysearch.service;

import com.polixis.companysearch.model.Company;
import com.polixis.companysearch.model.Officer;
import com.polixis.companysearch.model.Psc;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanySearchService {
    private static final long REQUEST_DELAY_MS = 1000;

    public List<Company> search(String query) throws IOException, InterruptedException {
        List<Company> companies = new ArrayList<>();

        String nextPageUrl =
                "https://find-and-update.company-information.service.gov.uk/search/companies?q="
                        + query;

        while (nextPageUrl != null && companies.size() < 100) {
            Thread.sleep(REQUEST_DELAY_MS);
            Document document = Jsoup.connect(nextPageUrl)
                    .userAgent("Company-Search-Service/1.0")
                    .get();
            Elements results = document.select("li.type-company");

            //System.out.println("Results on page: " + results.size());

            for (Element result : results) {
                if (companies.size() >= 100) {
                    break;
                }

                try {
                    Element link = result.selectFirst("h3 a");

                    if (link != null) {
                        String companyName = link.text();
                        String companyUrl = link.attr("href");

                        String fullCompanyUrl =
                                "https://find-and-update.company-information.service.gov.uk"
                                        + companyUrl;

                        String officersUrl = fullCompanyUrl + "/officers";

                        String pscUrl = fullCompanyUrl + "/persons-with-significant-control";

                        Thread.sleep(REQUEST_DELAY_MS);
                        Document companyDocument = Jsoup.connect(fullCompanyUrl)
                                .userAgent("Company-Search-Service/1.0")
                                .get();

                        Thread.sleep(REQUEST_DELAY_MS);
                        Document officersDocument = Jsoup.connect(officersUrl)
                                .userAgent("Company-Search-Service/1.0")
                                .get();

                        Thread.sleep(REQUEST_DELAY_MS);
                        Document pscDocument = Jsoup.connect(pscUrl)
                                .userAgent("Polixis-Company-Search-Internship-Task/1.0 (educational scraper; contact: aregmuradyan.dev@gmail.com)")
                                .get();

                        Element statusElement = companyDocument.selectFirst("#company-status");
                        String status = statusElement != null ? statusElement.text() : null;

                        Element addressElement = companyDocument.selectFirst("#roa-address");
                        String address = addressElement != null ? addressElement.text() : null;

                        Element companyTypeElement = companyDocument.selectFirst("#company-type-value");
                        String companyType = companyTypeElement != null ? companyTypeElement.text() : null;

                        Element creationDateElement = companyDocument.selectFirst("#company-creation-date");
                        String creationDate = creationDateElement != null ? creationDateElement.text() : null;

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

                        Elements pscElements = pscDocument.select(".appointments-list > div");
                        List<Psc> pscs = new ArrayList<>();

                        for (Element pscElement : pscElements) {
                            Element nameElement = pscElement.selectFirst("[id^=psc-name-]");
                            Element natureOfControlElement = pscElement.selectFirst("[id^=psc-noc-]");

                            String name = nameElement != null ? nameElement.text() : null;
                            String natureOfControl = natureOfControlElement != null
                                    ? natureOfControlElement.text()
                                    : null;

                            pscs.add(new Psc(name, natureOfControl));
                        }

                        Element meta = result.selectFirst("p.meta.crumbtrail");
                        String companyNumber = "";

                        if (meta != null) {
                            String metaText = meta.text();
                            companyNumber = metaText.split(" - ")[0];
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
                        //System.out.println("Companies collected: " + companies.size());
                    }
                } catch (HttpStatusException e){
                        System.out.println("Skipping company due to HTTP " + e.getStatusCode());
                    }

                Element nextPageElement = document.selectFirst("#next-page");
                if (nextPageElement != null) {
                    nextPageUrl =
                            "https://find-and-update.company-information.service.gov.uk"
                                    + nextPageElement.attr("href");
                } else {
                    nextPageUrl = null;
                }
            }
        }
        return companies;
    }
}

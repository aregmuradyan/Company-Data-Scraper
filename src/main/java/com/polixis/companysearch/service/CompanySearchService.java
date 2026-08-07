package com.polixis.companysearch.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLOutput;

@Service
public class CompanySearchService {

    public String search(String query) throws IOException {

        String url =
                "https://find-and-update.company-information.service.gov.uk/search/companies?q="
                        + query;

        Document document = Jsoup.connect(url)
                .userAgent("Company-Search-Service/1.0")
                .get();

        var results = document.select("li.type-company");

        for (var result : results) {
            Element link = result.selectFirst("h3 a");

            if (link != null) {
                String companyName = link.text();
                String companyUrl = link.attr("href");

                String fullCompanyUrl =
                        "https://find-and-update.company-information.service.gov.uk"
                                + companyUrl;

                Document companyDocument = Jsoup.connect(fullCompanyUrl)
                        .userAgent("Company-Search-Service/1.0")
                        .get();

                System.out.println(companyDocument.title());
                System.out.println(companyDocument.html());
                Element statusElement = companyDocument.selectFirst("#company-status");
                String status = statusElement != null ? statusElement.text() : null;

                Element addressElement = companyDocument.selectFirst("#roa-address");
                String address = addressElement != null ? addressElement.text() : null;

                Element companyTypeElement = companyDocument.selectFirst("#company-type-value");
                String companyType = companyTypeElement != null ? companyTypeElement.text() : null;

                Element creationDateElement = companyDocument.selectFirst("#company-creation-date");
                String creationDate = creationDateElement != null ? creationDateElement.text() : null;

                System.out.println("Status: " + status);
                System.out.println("Address: " + address);


                Element meta = result.selectFirst("p.meta.crumbtrail");
                String companyNumber = "";

                if (meta != null) {
                    String metaText = meta.text();
                    companyNumber = metaText.split(" - ")[0];
                }

                System.out.println("Name: " + companyName);
                System.out.println("Number: " + companyNumber);
                System.out.println("URL: " + companyUrl);
                System.out.println("Company Type: " + companyType);
                System.out.println("Creation Date: " + creationDate);
                System.out.println();
            }
            break;
        }
        return "Found " + results.size() + " results";
    }
}
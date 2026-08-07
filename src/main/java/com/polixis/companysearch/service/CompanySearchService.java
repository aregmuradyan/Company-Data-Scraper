package com.polixis.companysearch.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

                Element meta = result.selectFirst("p.meta.crumbtrail");
                String companyNumber = "";

                if (meta != null) {
                    String metaText = meta.text();
                    companyNumber = metaText.split(" - ")[0];
                }

                System.out.println("Name: " + companyName);
                System.out.println("Number: " + companyNumber);
                System.out.println("URL: " + companyUrl);
                System.out.println();
            }
        }
        return "Found " + results.size() + " results";
    }
}
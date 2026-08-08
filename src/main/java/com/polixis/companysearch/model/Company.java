package com.polixis.companysearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    private String companyNumber;
    private String companyName;
    private String status;
    private String companyType;
    private String incorporationDate;
    private String registeredOfficeAddress;
    private List<Officer> officers;
    private List<Psc> pscs;
}
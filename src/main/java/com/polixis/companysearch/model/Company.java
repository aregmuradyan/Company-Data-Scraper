package com.polixis.companysearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    private String companyNumber;
    private String companyName;
    private String status;
    private String companyType;
    private String incorporationDate;
    private String registeredOfficeAddress;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Officer> officers;
    @OneToMany(cascade = CascadeType.ALL)
    private List<Psc> pscs;
}
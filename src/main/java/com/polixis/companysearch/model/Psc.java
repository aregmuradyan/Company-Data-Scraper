package com.polixis.companysearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Psc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String natureOfControl;

    public Psc(String name, String natureOfControl) {
        this.name = name;
        this.natureOfControl = natureOfControl;
    }
}

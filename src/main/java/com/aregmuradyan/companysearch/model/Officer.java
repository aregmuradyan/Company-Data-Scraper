package com.aregmuradyan.companysearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Officer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String role;
    private String appointedOn;

    public Officer(String name, String role, String appointedOn) {
        this.name = name;
        this.role = role;
        this.appointedOn = appointedOn;
    }
}

package com.aregmuradyan.companysearch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchCache {

    @Id
    private String query;
    private LocalDateTime cachedAt;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Company> companies;
}

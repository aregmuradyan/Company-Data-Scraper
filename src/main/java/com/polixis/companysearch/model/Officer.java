package com.polixis.companysearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Officer {

    private String name;
    private String role;
    private String appointedOn;
}

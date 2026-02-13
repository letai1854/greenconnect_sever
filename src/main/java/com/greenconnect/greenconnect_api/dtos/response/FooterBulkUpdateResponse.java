package com.greenconnect.greenconnect_api.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FooterBulkUpdateResponse {
    
    private int sectionsCreated;
    private int sectionsUpdated;
    private int sectionsDeleted;
    private int linksCreated;
    private int linksUpdated;
    private int linksDeleted;
}

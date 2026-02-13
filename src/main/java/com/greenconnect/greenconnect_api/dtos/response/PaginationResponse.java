// PaginationResponse.java
package com.greenconnect.greenconnect_api.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse {
    private boolean hasNextPage;
    private String loadMoreEndpoint;
}
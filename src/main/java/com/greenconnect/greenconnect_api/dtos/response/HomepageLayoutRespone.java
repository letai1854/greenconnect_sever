package com.greenconnect.greenconnect_api.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HomepageLayoutRespone {
	private UUID id;
	private String title;
	private String widgetType;
	private String dataSourceKey;
	private String displayLayout;
	private Integer displayOrder;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}

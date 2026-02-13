package com.greenconnect.greenconnect_api.dtos.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Short rating;
    @Size(max = 2000)
    private String comment;
    @NotNull
    private UUID orderDetailId; 
    private List<MediaRequest> mediaList;

    @Data
    public static class MediaRequest {
        @NotBlank
        private String mediaType; // "IMAGE" or "VIDEO"
        @NotBlank
        private String mediaUrl;
    }
}
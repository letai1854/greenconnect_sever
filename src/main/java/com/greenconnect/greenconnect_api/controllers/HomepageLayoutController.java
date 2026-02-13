package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.HomepageLayoutRequest;
import com.greenconnect.greenconnect_api.dtos.response.HomepageLayoutRespone;
import com.greenconnect.greenconnect_api.services.HomepageLayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/homepage-layouts")
@RequiredArgsConstructor
public class HomepageLayoutController {

    private final HomepageLayoutService homepageLayoutService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HomepageLayoutRespone> createHomepageLayout(
            @Valid @RequestBody HomepageLayoutRequest request) {
        HomepageLayoutRespone created = homepageLayoutService.createLayout(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping("/content")
    public ResponseEntity<com.greenconnect.greenconnect_api.dtos.response.HomepageResponse> getHomepageContent() {
        com.greenconnect.greenconnect_api.dtos.response.HomepageResponse resp = homepageLayoutService.getHomepageContent();
        return ResponseEntity.ok(resp);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}/content")
    public ResponseEntity<com.greenconnect.greenconnect_api.dtos.response.HomepageResponse> getHomepageLayoutContent(@org.springframework.web.bind.annotation.PathVariable("id") java.util.UUID id) {
        com.greenconnect.greenconnect_api.dtos.response.HomepageResponse resp = homepageLayoutService.getLayoutContent(id);
        return ResponseEntity.ok(resp);
    }

    /**
     * Public endpoint used by widgets to load more products for a campaign.
     * Example: /homepage-layouts/viewallcampain/{campaignId}?page=0&size=8
     */
    @org.springframework.web.bind.annotation.GetMapping("/viewallcampain/{campaignId}")
    public ResponseEntity<com.greenconnect.greenconnect_api.dtos.response.WidgetResponse> viewAllCampaignProducts(
            @org.springframework.web.bind.annotation.PathVariable("campaignId") java.util.UUID campaignId,
            @org.springframework.web.bind.annotation.RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(value = "size", required = false, defaultValue = "8") int size) {

        com.greenconnect.greenconnect_api.dtos.response.WidgetResponse resp = homepageLayoutService.getCampaignProducts(campaignId, page, size);
        return ResponseEntity.ok(resp);
    }

    // 1) Unlink a banner from a homepage layout
    @org.springframework.web.bind.annotation.DeleteMapping("/{layoutId}/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlinkBannerFromLayout(@org.springframework.web.bind.annotation.PathVariable("layoutId") java.util.UUID layoutId,
                                                       @org.springframework.web.bind.annotation.PathVariable("bannerId") java.util.UUID bannerId) {
        homepageLayoutService.unlinkBannerFromLayout(layoutId, bannerId);
        return ResponseEntity.noContent().build();
    }

    // 2) Link a banner to a homepage layout
    @org.springframework.web.bind.annotation.PostMapping("/{layoutId}/banners/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> linkBannerToLayout(@org.springframework.web.bind.annotation.PathVariable("layoutId") java.util.UUID layoutId,
                                                   @org.springframework.web.bind.annotation.PathVariable("bannerId") java.util.UUID bannerId) {
        homepageLayoutService.linkBannerToLayout(layoutId, bannerId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3) Unlink a campaign from a homepage layout
    @org.springframework.web.bind.annotation.DeleteMapping("/{layoutId}/campaigns/{campaignId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unlinkCampaignFromLayout(@org.springframework.web.bind.annotation.PathVariable("layoutId") java.util.UUID layoutId,
                                                        @org.springframework.web.bind.annotation.PathVariable("campaignId") java.util.UUID campaignId) {
        homepageLayoutService.unlinkCampaignFromLayout(layoutId, campaignId);
        return ResponseEntity.noContent().build();
    }

    // 4) Link a list of campaigns to a homepage layout (append order)
    @org.springframework.web.bind.annotation.PostMapping("/{layoutId}/campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> linkCampaignsToLayout(@org.springframework.web.bind.annotation.PathVariable("layoutId") java.util.UUID layoutId,
                                                     @Valid @RequestBody java.util.List<java.util.UUID> campaignIds) {
        homepageLayoutService.linkCampaignsToLayout(layoutId, campaignIds);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

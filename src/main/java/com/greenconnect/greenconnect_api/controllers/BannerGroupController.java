package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.BannerGroupCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.BannerGroupUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.BannerGroupResponse;
import com.greenconnect.greenconnect_api.services.BannerGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/banner-groups")
@RequiredArgsConstructor
public class BannerGroupController {

    private final BannerGroupService bannerGroupService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerGroupResponse> createGroupWithBanners(
            @Valid @RequestBody BannerGroupCreateRequest request) {
        BannerGroupResponse createdGroup = bannerGroupService.createGroupWithBanners(request);
        return new ResponseEntity<>(createdGroup, HttpStatus.CREATED);
    }

    @PutMapping("/update/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerGroupResponse> updateGroupWithBanners(
            @PathVariable("groupId") java.util.UUID groupId,
            @Valid @RequestBody BannerGroupUpdateRequest request) {
        // Ensure request contains the target group id (prefer path variable as source of truth)
        request.setGroupId(groupId);
        BannerGroupResponse updated = bannerGroupService.updateGroupWithBanners(request);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGroup(@PathVariable("groupId") java.util.UUID groupId) {
        bannerGroupService.deleteGroupById(groupId);
        return ResponseEntity.noContent().build();
    }
}
package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.BannerGroupCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.BannerGroupUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.BannerGroupResponse;
import com.greenconnect.greenconnect_api.entities.Banner;
import com.greenconnect.greenconnect_api.entities.BannerGroup;
import com.greenconnect.greenconnect_api.repositories.BannerGroupRepository;
import com.greenconnect.greenconnect_api.services.BannerGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerGroupServiceImpl implements BannerGroupService {

    private final BannerGroupRepository bannerGroupRepository;

    @Override
    @Transactional // Đảm bảo tất cả các thao tác (tạo nhóm, tạo banner) đều thành công hoặc thất bại cùng nhau
    public BannerGroupResponse createGroupWithBanners(BannerGroupCreateRequest request) {
        // Bước 1: Kiểm tra xem groupKey đã tồn tại chưa để tránh trùng lặp
        if (bannerGroupRepository.findByGroupKey(request.getGroupKey()).isPresent()) {
            throw new IllegalArgumentException("Banner group với key '" + request.getGroupKey() + "' đã tồn tại.");
        }

        // Bước 2: Tạo entity BannerGroup (đối tượng cha)
        BannerGroup newGroup = BannerGroup.builder()
                .groupName(request.getGroupName())
                .groupKey(request.getGroupKey())
                .displayLayout(request.getDisplayLayout())
                .build();

        // Bước 3: Tạo danh sách các entity Banner (các đối tượng con)
        List<Banner> bannerEntities = request.getBanners().stream()
                .map(bannerItem -> {
                    Banner b = new Banner();
                    b.setBannerName(bannerItem.getBannerName());
                    b.setImageUrl(bannerItem.getImageUrl());
                    b.setTargetUrl(bannerItem.getTargetUrl());
                    b.setDisplayOrder(bannerItem.getDisplayOrder());
                    b.setIsActive(bannerItem.isActive());
                    b.setBannerGroup(newGroup); // QUAN TRỌNG: Gán đối tượng cha cho mỗi đối tượng con
                    return b;
                })
                .collect(Collectors.toList());

        // Bước 4: Gán danh sách con vào cho cha (không bắt buộc nhưng là good practice)
        newGroup.setBanners(bannerEntities);

        // Bước 5: Lưu đối tượng cha. Nhờ có `cascade = CascadeType.ALL`, các đối tượng con sẽ tự động được lưu.
        BannerGroup savedGroup = bannerGroupRepository.save(newGroup);

        // Bước 6: Chuyển đổi entity đã lưu sang DTO để trả về
        return mapToResponse(savedGroup);
    }

    @Override
    @Transactional
    public BannerGroupResponse updateGroupWithBanners(BannerGroupUpdateRequest request) {
        // Find existing group
        BannerGroup group = bannerGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("BannerGroup not found: " + request.getGroupId()));

        // Update group fields
        group.setGroupName(request.getGroupName());
        group.setGroupKey(request.getGroupKey());
        group.setDisplayLayout(request.getDisplayLayout());

        // Existing banners map by id
        java.util.Map<java.util.UUID, Banner> existingMap = group.getBanners().stream()
                .collect(java.util.stream.Collectors.toMap(Banner::getId, b -> b));

        // Build new list of banners from request
        java.util.List<Banner> newBanners = new java.util.ArrayList<>();
        for (BannerGroupUpdateRequest.BannerItem item : request.getBanners()) {
            if (item.getBannerId() != null) {
                // update existing
                Banner exist = existingMap.remove(item.getBannerId());
                if (exist == null) {
                    throw new IllegalArgumentException("Banner id not found in group: " + item.getBannerId());
                }
                exist.setBannerName(item.getBannerName());
                exist.setImageUrl(item.getImageUrl());
                exist.setTargetUrl(item.getTargetUrl());
                exist.setDisplayOrder(item.getDisplayOrder());
                exist.setIsActive(item.isActive());
                newBanners.add(exist);
            } else {
                // create new
                Banner b = new Banner();
                b.setBannerName(item.getBannerName());
                b.setImageUrl(item.getImageUrl());
                b.setTargetUrl(item.getTargetUrl());
                b.setDisplayOrder(item.getDisplayOrder());
                b.setIsActive(item.isActive());
                b.setBannerGroup(group);
                newBanners.add(b);
            }
        }

        // Any remaining items in existingMap should be deleted (they were not included in request)
        if (!existingMap.isEmpty()) {
            group.getBanners().removeAll(existingMap.values());
            // With cascade remove they will be deleted when saving group
        }

        // Set group's banners to new ordered list
        group.setBanners(newBanners);

        BannerGroup saved = bannerGroupRepository.save(group);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGroupById(java.util.UUID groupId) {
        if (!bannerGroupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("BannerGroup not found: " + groupId);
        }
        bannerGroupRepository.deleteById(groupId);
    }

    private BannerGroupResponse mapToResponse(BannerGroup group) {
        List<BannerGroupResponse.BannerDetail> bannerDetails = group.getBanners().stream()
                .map(banner -> BannerGroupResponse.BannerDetail.builder()
                        .bannerId(banner.getId())
                        .bannerName(banner.getBannerName())
                        .imageUrl(banner.getImageUrl())
                        .targetUrl(banner.getTargetUrl())
                        .displayOrder(banner.getDisplayOrder())
                        .isActive(banner.getIsActive())
                        .createdAt(banner.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return BannerGroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .groupKey(group.getGroupKey())
                .displayLayout(group.getDisplayLayout())
                .banners(bannerDetails)
                .build();
    }
}
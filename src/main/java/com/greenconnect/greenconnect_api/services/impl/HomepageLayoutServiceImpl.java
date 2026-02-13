package com.greenconnect.greenconnect_api.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.HomepageLayoutRequest;
import com.greenconnect.greenconnect_api.dtos.response.BannerDetailResponse;
import com.greenconnect.greenconnect_api.dtos.response.HomepageLayoutRespone;
import com.greenconnect.greenconnect_api.dtos.response.HomepageResponse;
import com.greenconnect.greenconnect_api.dtos.response.WidgetDataResponse;
import com.greenconnect.greenconnect_api.dtos.response.WidgetResponse;
import com.greenconnect.greenconnect_api.entities.Banner;
import com.greenconnect.greenconnect_api.entities.BannerGroup;
import com.greenconnect.greenconnect_api.entities.HomepageLayout;
import com.greenconnect.greenconnect_api.entities.PromotionCampaign;
import com.greenconnect.greenconnect_api.entities.PromotionProduct;
import com.greenconnect.greenconnect_api.repositories.BannerGroupRepository;
import com.greenconnect.greenconnect_api.repositories.BannerRepository;
import com.greenconnect.greenconnect_api.repositories.HomepageLayoutRepository;
import com.greenconnect.greenconnect_api.repositories.PromotionCampaignRepository;
import com.greenconnect.greenconnect_api.repositories.PromotionProductRepository;
import com.greenconnect.greenconnect_api.services.HomepageLayoutService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomepageLayoutServiceImpl implements HomepageLayoutService {

    private final HomepageLayoutRepository homepageLayoutRepository;
    private final BannerGroupRepository bannerGroupRepository;
    private final PromotionCampaignRepository promotionCampaignRepository;
                        private final BannerRepository bannerRepository;
                                                private final PromotionProductRepository promotionProductRepository;
                                                private final com.greenconnect.greenconnect_api.services.ProductService productService;

    @Override
    @Transactional
    public HomepageLayoutRespone createLayout(HomepageLayoutRequest request) {
        HomepageLayout layout = HomepageLayout.builder()
                .title(request.getTitle())
                .isActive(request.getIsActive())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // attach banner group if provided
        if (request.getBannerGroupId() != null) {
            BannerGroup group = bannerGroupRepository.findById(request.getBannerGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("BannerGroup not found"));
            layout.setBannerGroup(group);
        }

        // attach ordered campaigns if provided
        if (request.getOrderedCampaignIds() != null && !request.getOrderedCampaignIds().isEmpty()) {
            List<PromotionCampaign> campaigns = request.getOrderedCampaignIds().stream()
                    .map(id -> promotionCampaignRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + id)))
                    .collect(Collectors.toList());
            layout.setCampaigns(campaigns);
        }

        HomepageLayout saved = homepageLayoutRepository.save(layout);

        return HomepageLayoutRespone.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .isActive(saved.getIsActive())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HomepageResponse getHomepageContent() {
        // Load active layouts (usually one), build widgets
        List<HomepageLayout> layouts = homepageLayoutRepository.findByIsActiveTrue();
        List<WidgetResponse> widgets = layouts.stream()
                .map(this::buildWidgetFromLayout)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return HomepageResponse.builder().widgets(widgets).build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.HomepageResponse getLayoutContent(java.util.UUID layoutId) {
        HomepageLayout layout = homepageLayoutRepository.findById(layoutId)
                .orElseThrow(() -> new IllegalArgumentException("HomepageLayout not found: " + layoutId));

        java.util.List<com.greenconnect.greenconnect_api.dtos.response.WidgetResponse> widgets = new java.util.ArrayList<>();

        // Banner group widget
        if (layout.getBannerGroup() != null) {
            java.util.List<com.greenconnect.greenconnect_api.dtos.response.BannerDetailResponse> bannerDetails = bannerRepository
                    .findByBannerGroupIdAndIsActiveTrueOrderByDisplayOrderAsc(layout.getBannerGroup().getId()).stream()
                    .map(b -> com.greenconnect.greenconnect_api.dtos.response.BannerDetailResponse.builder()
                            .id(b.getId()).imageUrl(b.getImageUrl()).targetUrl(b.getTargetUrl()).build())
                    .collect(Collectors.toList());

            widgets.add(com.greenconnect.greenconnect_api.dtos.response.WidgetResponse.builder()
                    .widgetId("banner_group_" + layout.getBannerGroup().getId().toString())
                    .widgetType("BANNER_GROUP")
                    .displayLayout(layout.getBannerGroup().getDisplayLayout() != null ? layout.getBannerGroup().getDisplayLayout().name() : null)
                    .title(layout.getTitle())
                    .data(com.greenconnect.greenconnect_api.dtos.response.WidgetDataResponse.builder().banners(bannerDetails).build())
                    .build());
        }

        // Campaign widgets (each campaign -> up to 8 products + pagination)
        if (layout.getCampaigns() != null) {
            for (PromotionCampaign campaign : layout.getCampaigns()) {
                Pageable firstPage = PageRequest.of(0, 8);
                org.springframework.data.domain.Page<PromotionProduct> page = promotionProductRepository.findByCampaignId(campaign.getId(), firstPage);

                                java.util.List<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> products = page.getContent().stream()
                                                .map(pp -> {
                                                        // Use ProductService to build full ProductResponse for each product
                                                        try {
                                                                return productService.getProductById(pp.getProduct().getId());
                                                        } catch (Exception ex) {
                                                                // If product not found or error occurs, skip it by returning null
                                                                return null;
                                                        }
                                                })
                                                .filter(java.util.Objects::nonNull)
                                                .collect(Collectors.toList());

                com.greenconnect.greenconnect_api.dtos.response.PaginationResponse pagination = com.greenconnect.greenconnect_api.dtos.response.PaginationResponse.builder()
                        .hasNextPage(page.hasNext())
                        // new loadMoreEndpoint format requested: /homepage-layouts/viewallcampain/{campaignId}
                        .loadMoreEndpoint(String.format("/homepage-layouts/viewallcampain/%s", campaign.getId()))
                        .build();

                widgets.add(com.greenconnect.greenconnect_api.dtos.response.WidgetResponse.builder()
                        .widgetId("campaign_" + campaign.getId().toString())
                        .widgetType("PROMOTION_CAMPAIGN")
                        .displayLayout(campaign.getCampaignType() != null ? campaign.getCampaignType().name() : null)
                        .title(campaign.getCampaignName())
                        .viewAllUrl(buildViewAllUrl(campaign))
                        .data(com.greenconnect.greenconnect_api.dtos.response.WidgetDataResponse.builder().products(products).build())
                        .pagination(pagination)
                        .build());
            }
        }

                return com.greenconnect.greenconnect_api.dtos.response.HomepageResponse.builder().widgets(widgets).build();
        }

                @Override
                @Transactional(readOnly = true)
                public com.greenconnect.greenconnect_api.dtos.response.WidgetResponse getCampaignProducts(java.util.UUID campaignId, int page, int pageSize) {
                        PromotionCampaign campaign = promotionCampaignRepository.findById(campaignId)
                                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize <= 0 ? 8 : pageSize);
                        org.springframework.data.domain.Page<PromotionProduct> productPage = promotionProductRepository.findByCampaignId(campaign.getId(), pageable);

                        java.util.List<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> products = productPage.getContent().stream()
                                        .map(pp -> {
                                                try {
                                                        return productService.getProductById(pp.getProduct().getId());
                                                } catch (Exception ex) {
                                                        return null;
                                                }
                                        })
                                        .filter(java.util.Objects::nonNull)
                                        .collect(Collectors.toList());

                        com.greenconnect.greenconnect_api.dtos.response.PaginationResponse pagination = com.greenconnect.greenconnect_api.dtos.response.PaginationResponse.builder()
                                        .hasNextPage(productPage.hasNext())
                                        .loadMoreEndpoint(String.format("/homepage-layouts/viewallcampain/%s", campaign.getId()))
                                        .build();

                        return com.greenconnect.greenconnect_api.dtos.response.WidgetResponse.builder()
                                        .widgetId("campaign_" + campaign.getId().toString())
                                        .widgetType("PROMOTION_CAMPAIGN")
                                        .displayLayout(campaign.getCampaignType() != null ? campaign.getCampaignType().name() : null)
                                        .title(campaign.getCampaignName())
                                        .viewAllUrl(buildViewAllUrl(campaign))
                                        .data(com.greenconnect.greenconnect_api.dtos.response.WidgetDataResponse.builder().products(products).build())
                                        .pagination(pagination)
                                        .build();
                }

    private WidgetResponse buildWidgetFromLayout(HomepageLayout layout) {
        if (layout.getBannerGroup() != null) {
            // Build banner group widget
            List<BannerDetailResponse> bannerDetails = bannerRepository
                    .findByBannerGroupIdAndIsActiveTrueOrderByDisplayOrderAsc(layout.getBannerGroup().getId()).stream()
                    .map(b -> BannerDetailResponse.builder().id(b.getId()).imageUrl(b.getImageUrl()).targetUrl(b.getTargetUrl()).build())
                    .collect(Collectors.toList());

            return WidgetResponse.builder()
                    .widgetId(layout.getId().toString())
                    .widgetType(layout.getBannerGroup().getDisplayLayout() != null ? layout.getBannerGroup().getDisplayLayout().name() : "BANNER_GROUP")
                    .displayLayout(layout.getBannerGroup().getDisplayLayout() != null ? layout.getBannerGroup().getDisplayLayout().name() : null)
                    .title(layout.getTitle())
                    .data(WidgetDataResponse.builder().banners(bannerDetails).build())
                    .build();
        }

        // If layout has campaigns, build promotion campaign widgets (one widget per campaign)
        if (layout.getCampaigns() != null && !layout.getCampaigns().isEmpty()) {
            // For simplicity return first campaign as a widget (original design had multiple widgets)
            PromotionCampaign campaign = layout.getCampaigns().get(0);
            // Fetch up to 8 promoted products for this campaign
            Pageable firstPage = PageRequest.of(0, 8);
            org.springframework.data.domain.Page<PromotionProduct> page = promotionProductRepository.findByCampaignId(campaign.getId(), firstPage);

                        java.util.List<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> products = page.getContent().stream()
                                        .map(pp -> {
                                                try {
                                                        return productService.getProductById(pp.getProduct().getId());
                                                } catch (Exception ex) {
                                                        return null;
                                                }
                                        })
                                        .filter(java.util.Objects::nonNull)
                                        .collect(Collectors.toList());

            return WidgetResponse.builder()
                    .widgetId(layout.getId().toString())
                    .widgetType("PROMOTION_CAMPAIGN")
                    .displayLayout(campaign.getCampaignType() != null ? campaign.getCampaignType().name() : null)
                    .title(campaign.getCampaignName())
                    .viewAllUrl(buildViewAllUrl(campaign))
                    .data(WidgetDataResponse.builder().products(products).build())
                    .build();
        }

        return null;
    }

        /**
         * Build a viewAllUrl for a campaign. Prefer stored slug; if missing, generate a URL-friendly
         * slug from the campaign name.
         */
        private String buildViewAllUrl(PromotionCampaign campaign) {
                if (campaign.getUrlViewAll() != null && !campaign.getUrlViewAll().isBlank()) {
                        return campaign.getUrlViewAll();
                }
                String slug = campaign.getSlug();
                if (slug == null || slug.isBlank()) {
                                // generate simple slug from name: lower-case, spaces -> '-', remove non-alphanum/dash
                                slug = campaign.getCampaignName() == null ? "" : campaign.getCampaignName().toLowerCase()
                                                .replaceAll("\\s+", "-")
                                                .replaceAll("[^a-z0-9-]", "");
                }
                return "/promotions/" + slug;
        }

        @Override
        @Transactional
        public void unlinkBannerFromLayout(java.util.UUID layoutId, java.util.UUID bannerId) {
                HomepageLayout layout = homepageLayoutRepository.findById(layoutId)
                                .orElseThrow(() -> new IllegalArgumentException("HomepageLayout not found: " + layoutId));

                if (layout.getBannerGroup() == null) return;

                // Remove the banner from its group if present
                BannerGroup group = layout.getBannerGroup();
                if (group.getBanners() != null) {
                        group.getBanners().removeIf(b -> b.getId().equals(bannerId));
                }
                // persist
                bannerGroupRepository.save(group);
        }

        @Override
        @Transactional
        public void linkBannerToLayout(java.util.UUID layoutId, java.util.UUID bannerId) {
                HomepageLayout layout = homepageLayoutRepository.findById(layoutId)
                                .orElseThrow(() -> new IllegalArgumentException("HomepageLayout not found: " + layoutId));

                Banner banner = bannerRepository.findById(bannerId)
                                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + bannerId));

                // If layout has no banner group, create one
                BannerGroup group = layout.getBannerGroup();
                if (group == null) {
                        group = new BannerGroup();
                        group.setGroupKey("auto-group-" + java.util.UUID.randomUUID());
                        group.setDisplayLayout(null);
                }

                // ensure banner is attached to group
                banner.setBannerGroup(group);

                // add banner to group's list
                if (group.getBanners() == null) group.setBanners(new java.util.ArrayList<>());
                if (group.getBanners().stream().noneMatch(b -> b.getId().equals(banner.getId()))) {
                        group.getBanners().add(banner);
                }

                // persist group and layout
                bannerGroupRepository.save(group);
                layout.setBannerGroup(group);
                homepageLayoutRepository.save(layout);
        }

        @Override
        @Transactional
        public void unlinkCampaignFromLayout(java.util.UUID layoutId, java.util.UUID campaignId) {
                HomepageLayout layout = homepageLayoutRepository.findById(layoutId)
                                .orElseThrow(() -> new IllegalArgumentException("HomepageLayout not found: " + layoutId));

                if (layout.getCampaigns() == null) return;
                layout.setCampaigns(layout.getCampaigns().stream().filter(c -> !c.getId().equals(campaignId)).collect(Collectors.toList()));
                homepageLayoutRepository.save(layout);
        }

        @Override
        @Transactional
        public void linkCampaignsToLayout(java.util.UUID layoutId, java.util.List<java.util.UUID> campaignIds) {
                HomepageLayout layout = homepageLayoutRepository.findById(layoutId)
                                .orElseThrow(() -> new IllegalArgumentException("HomepageLayout not found: " + layoutId));

                List<PromotionCampaign> campaigns = campaignIds.stream()
                                .map(id -> promotionCampaignRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + id)))
                                .collect(Collectors.toList());

                if (layout.getCampaigns() == null) {
                        layout.setCampaigns(new java.util.ArrayList<>());
                }

                // append new campaigns but avoid duplicates
                java.util.Set<java.util.UUID> existing = layout.getCampaigns().stream().map(PromotionCampaign::getId).collect(java.util.stream.Collectors.toSet());
                for (PromotionCampaign c : campaigns) {
                        if (!existing.contains(c.getId())) layout.getCampaigns().add(c);
                }

                homepageLayoutRepository.save(layout);
        }
}

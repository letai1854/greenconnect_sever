package com.greenconnect.greenconnect_api.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.FooterBulkUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterLinkRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterSectionRequest;
import com.greenconnect.greenconnect_api.dtos.response.FooterBulkUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterLinkResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterSectionResponse;
import com.greenconnect.greenconnect_api.entities.FooterLink;
import com.greenconnect.greenconnect_api.entities.FooterSection;
import com.greenconnect.greenconnect_api.enums.IconKey;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.FooterLinkRepository;
import com.greenconnect.greenconnect_api.repositories.FooterSectionRepository;
import com.greenconnect.greenconnect_api.services.FooterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FooterServiceImpl implements FooterService {
    
    private final FooterSectionRepository footerSectionRepository;
    private final FooterLinkRepository footerLinkRepository;
    
    @Override
    @Cacheable(value = "footerCache", key = "#includeInactive")
    public FooterResponse getAllFooter(boolean includeInactive) {
        log.info("Getting all footer data (includeInactive: {})", includeInactive);
        
        List<FooterSection> sections = includeInactive 
            ? footerSectionRepository.findAllSectionsWithLinks()
            : footerSectionRepository.findAllActiveSectionsWithLinks();
        
        List<FooterSectionResponse> sectionResponses = sections.stream()
            .map(this::mapToSectionResponse)
            .collect(Collectors.toList());
        
        return FooterResponse.builder()
            .sections(sectionResponses)
            .build();
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public FooterSectionResponse createSection(FooterSectionRequest request) {
        log.info("Creating new footer section: {}", request.getName());
        
        FooterSection section = FooterSection.builder()
            .name(request.getName())
            .sortOrder(request.getSortOrder())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();
        
        section = footerSectionRepository.save(section);
        log.info("Footer section created with ID: {}", section.getId());
        
        return mapToSectionResponse(section);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public FooterSectionResponse updateSection(UUID sectionId, FooterSectionRequest request) {
        log.info("Updating footer section: {}", sectionId);
        
        FooterSection section = footerSectionRepository.findById(sectionId)
            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SECTION_NOT_FOUND));
        
        if (request.getName() != null) {
            section.setName(request.getName());
        }
        if (request.getSortOrder() != null) {
            section.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            section.setIsActive(request.getIsActive());
        }
        
        section = footerSectionRepository.save(section);
        log.info("Footer section updated: {}", sectionId);
        
        return mapToSectionResponse(section);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public void deleteSection(UUID sectionId) {
        log.info("Deleting footer section: {}", sectionId);
        
        FooterSection section = footerSectionRepository.findById(sectionId)
            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SECTION_NOT_FOUND));
        
        footerSectionRepository.delete(section);
        log.info("Footer section deleted: {}", sectionId);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public FooterLinkResponse createLink(UUID sectionId, FooterLinkRequest request) {
        log.info("Creating new link for section: {}", sectionId);
        
        FooterSection section = footerSectionRepository.findById(sectionId)
            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SECTION_NOT_FOUND));
        
        FooterLink link = FooterLink.builder()
            .section(section)
            .iconKey(request.getIconKey())
            .value(request.getValue())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();
        
        link = footerLinkRepository.save(link);
        log.info("Footer link created with ID: {}", link.getId());
        
        return mapToLinkResponse(link);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public FooterLinkResponse updateLink(UUID linkId, FooterLinkRequest request) {
        log.info("Updating footer link: {}", linkId);
        
        FooterLink link = footerLinkRepository.findById(linkId)
            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));
        
        if (request.getIconKey() != null) {
            link.setIconKey(request.getIconKey());
        }
        if (request.getValue() != null) {
            link.setValue(request.getValue());
        }
        if (request.getIsActive() != null) {
            link.setIsActive(request.getIsActive());
        }
        
        link = footerLinkRepository.save(link);
        log.info("Footer link updated: {}", linkId);
        
        return mapToLinkResponse(link);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public void deleteLink(UUID linkId) {
        log.info("Deleting footer link: {}", linkId);
        
        FooterLink link = footerLinkRepository.findById(linkId)
            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));
        
        footerLinkRepository.delete(link);
        log.info("Footer link deleted: {}", linkId);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "footerCache", allEntries = true)
    public FooterBulkUpdateResponse bulkUpdate(FooterBulkUpdateRequest request) {
        log.info("Starting bulk update for footer");
        
        int sectionsCreated = 0;
        int sectionsUpdated = 0;
        int linksCreated = 0;
        int linksUpdated = 0;
        
        // 1. Xóa links
        if (request.getDeletedLinkIds() != null && !request.getDeletedLinkIds().isEmpty()) {
            footerLinkRepository.deleteAllById(request.getDeletedLinkIds());
            log.info("Deleted {} links", request.getDeletedLinkIds().size());
        }
        
        // 2. Xóa sections
        if (request.getDeletedSectionIds() != null && !request.getDeletedSectionIds().isEmpty()) {
            footerSectionRepository.deleteAllById(request.getDeletedSectionIds());
            log.info("Deleted {} sections", request.getDeletedSectionIds().size());
        }
        
        // 3. Xử lý sections và links
        for (FooterBulkUpdateRequest.BulkSectionData sectionData : request.getSections()) {
            FooterSection section;
            
            if (sectionData.getId() == null) {
                // Tạo mới section
                section = FooterSection.builder()
                    .name(sectionData.getName())
                    .sortOrder(sectionData.getSortOrder())
                    .isActive(sectionData.getIsActive() != null ? sectionData.getIsActive() : true)
                    .build();
                section = footerSectionRepository.save(section);
                sectionsCreated++;
            } else {
                // Cập nhật section
                section = footerSectionRepository.findById(sectionData.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.FOOTER_SECTION_NOT_FOUND));
                section.setName(sectionData.getName());
                section.setSortOrder(sectionData.getSortOrder());
                if (sectionData.getIsActive() != null) {
                    section.setIsActive(sectionData.getIsActive());
                }
                section = footerSectionRepository.save(section);
                sectionsUpdated++;
            }
            
            // Xử lý links
            if (sectionData.getLinks() != null) {
                for (FooterBulkUpdateRequest.BulkLinkData linkData : sectionData.getLinks()) {
                    if (linkData.getId() == null) {
                        // Tạo mới link
                        FooterLink link = FooterLink.builder()
                            .section(section)
                            .iconKey(IconKey.valueOf(linkData.getIconKey().toUpperCase()))
                            .value(linkData.getValue())
                            .isActive(linkData.getIsActive() != null ? linkData.getIsActive() : true)
                            .build();
                        footerLinkRepository.save(link);
                        linksCreated++;
                    } else {
                        // Cập nhật link
                        FooterLink link = footerLinkRepository.findById(linkData.getId())
                            .orElseThrow(() -> new AppException(ErrorCode.FOOTER_LINK_NOT_FOUND));
                        link.setIconKey(IconKey.valueOf(linkData.getIconKey().toUpperCase()));
                        link.setValue(linkData.getValue());
                        if (linkData.getIsActive() != null) {
                            link.setIsActive(linkData.getIsActive());
                        }
                        footerLinkRepository.save(link);
                        linksUpdated++;
                    }
                }
            }
        }
        
        log.info("Bulk update completed - Sections: {}/{} created/updated, Links: {}/{} created/updated",
            sectionsCreated, sectionsUpdated, linksCreated, linksUpdated);
        
        return FooterBulkUpdateResponse.builder()
            .sectionsCreated(sectionsCreated)
            .sectionsUpdated(sectionsUpdated)
            .sectionsDeleted(request.getDeletedSectionIds() != null ? request.getDeletedSectionIds().size() : 0)
            .linksCreated(linksCreated)
            .linksUpdated(linksUpdated)
            .linksDeleted(request.getDeletedLinkIds() != null ? request.getDeletedLinkIds().size() : 0)
            .build();
    }
    
    // Helper methods
    private FooterSectionResponse mapToSectionResponse(FooterSection section) {
        List<FooterLinkResponse> linkResponses = section.getLinks().stream()
            .map(this::mapToLinkResponse)
            .collect(Collectors.toList());
        
        return FooterSectionResponse.builder()
            .id(section.getId())
            .name(section.getName())
            .sortOrder(section.getSortOrder())
            .isActive(section.getIsActive())
            .createdAt(section.getCreatedAt())
            .updatedAt(section.getUpdatedAt())
            .links(linkResponses)
            .build();
    }
    
    private FooterLinkResponse mapToLinkResponse(FooterLink link) {
        return FooterLinkResponse.builder()
            .id(link.getId())
            .sectionId(link.getSection().getId())
            .iconKey(link.getIconKey())
            .value(link.getValue())
            .isActive(link.getIsActive())
            .createdAt(link.getCreatedAt())
            .updatedAt(link.getUpdatedAt())
            .build();
    }
}

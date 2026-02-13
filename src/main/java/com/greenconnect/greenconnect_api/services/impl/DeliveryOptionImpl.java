package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.DeliveryOptionRequest;
import com.greenconnect.greenconnect_api.dtos.response.DeliveryOptionRespone;
import com.greenconnect.greenconnect_api.entities.DeliveryOption;
import com.greenconnect.greenconnect_api.repositories.DeliveryOptionRepository;
import com.greenconnect.greenconnect_api.services.DeliveryOptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryOptionImpl implements DeliveryOptionService {

    private final DeliveryOptionRepository deliveryOptionRepository;

    @Override
    public DeliveryOptionRespone createDeliveryOption(DeliveryOptionRequest request) {
        log.info("Creating delivery option (shipping time slot): {} ", request.getServiceName());

        DeliveryOption entity = DeliveryOption.builder()
                .slug(request.getSlug())
                .serviceName(request.getServiceName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .cutoffTime(request.getCutoffTime())
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .build();

        DeliveryOption saved = deliveryOptionRepository.save(entity);

        DeliveryOptionRespone resp = DeliveryOptionRespone.builder()
                .id(saved.getId())
                .slug(saved.getSlug())
                .serviceName(saved.getServiceName())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .cutoffTime(saved.getCutoffTime())
                .isActive(saved.getIsActive())
                .build();

        log.info("Delivery option created with id={}", resp.getId());
        return resp;
    }

    @Override
    public DeliveryOptionRespone updateDeliveryOption(UUID id, DeliveryOptionRequest request) {
        log.info("Updating delivery option id={}", id);
        DeliveryOption existing = deliveryOptionRepository.findById(id)
                .orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.AppException(
                        com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
                        "Delivery option not found: " + id));

        // Update mutable fields
        if (request.getSlug() != null) existing.setSlug(request.getSlug());
        if (request.getServiceName() != null) existing.setServiceName(request.getServiceName());
        if (request.getStartTime() != null) existing.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) existing.setEndTime(request.getEndTime());
        if (request.getCutoffTime() != null) existing.setCutoffTime(request.getCutoffTime());
        if (request.getIsActive() != null) existing.setIsActive(request.getIsActive());

        DeliveryOption saved = deliveryOptionRepository.save(existing);

        DeliveryOptionRespone resp = DeliveryOptionRespone.builder()
                .id(saved.getId())
                .slug(saved.getSlug())
                .serviceName(saved.getServiceName())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .cutoffTime(saved.getCutoffTime())
                .isActive(saved.getIsActive())
                .build();

        log.info("Delivery option updated id={}", resp.getId());
        return resp;
    }

        @Override
        public List<DeliveryOptionRespone> getActiveDeliveryOptions() {
                log.info("Fetching active delivery options");
                return deliveryOptionRepository.findByIsActiveTrue()
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public List<DeliveryOptionRespone> getAllDeliveryOptions() {
                log.info("Fetching all delivery options (admin)");
                return deliveryOptionRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        private DeliveryOptionRespone toResponse(DeliveryOption saved) {
                return DeliveryOptionRespone.builder()
                                .id(saved.getId())
                                .slug(saved.getSlug())
                                .serviceName(saved.getServiceName())
                                .startTime(saved.getStartTime())
                                .endTime(saved.getEndTime())
                                .cutoffTime(saved.getCutoffTime())
                                .isActive(saved.getIsActive())
                                .build();
        }
}

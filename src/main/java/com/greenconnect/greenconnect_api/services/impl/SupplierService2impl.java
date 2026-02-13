package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.RequestSupplier;
import com.greenconnect.greenconnect_api.dtos.request.RequestSupplier.SupplierCertificationRequest;
import com.greenconnect.greenconnect_api.dtos.request.RequestSupplier.SupplierMediaRequest;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
import com.greenconnect.greenconnect_api.entities.Certification;
import com.greenconnect.greenconnect_api.entities.Supplier;
import com.greenconnect.greenconnect_api.entities.SupplierCertification;
import com.greenconnect.greenconnect_api.entities.SupplierCertificationId;
import com.greenconnect.greenconnect_api.entities.SupplierMedia;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.CertificationRepository;
import com.greenconnect.greenconnect_api.repositories.SupplierResponsitory2;
import com.greenconnect.greenconnect_api.services.SupplierService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService2impl implements SupplierService2 {

	private final SupplierResponsitory2 supplierRepository;
	private final CertificationRepository certificationRepository;

	@Override
	@Transactional
	public SupplierResponse createSupplier(RequestSupplier request) {
		// Validation: unique name
		if (supplierRepository.existsByName(request.getName())) {
			throw new AppException(ErrorCode.SUPPLIER_NAME_ALREADY_EXISTS, "Supplier name already exists");
		}

		Supplier s = Supplier.builder()
				.name(request.getName())
				.description(request.getDescription())
				.logoUrl(request.getLogoUrl())
				.address(request.getAddress())
				.email(request.getEmail())
				.phoneNumber(request.getPhoneNumber())
				.isActive(request.getIsActive() == null ? true : request.getIsActive())
				.build();

		// Media
		List<SupplierMedia> medias = new ArrayList<>();
		if (request.getSupplierMedia() != null) {
			for (SupplierMediaRequest mr : request.getSupplierMedia()) {
				SupplierMedia m = SupplierMedia.builder()
						.supplier(s)
						.mediaType(mr.getMediaType())
						.mediaUrl(mr.getMediaUrl())
						.caption(mr.getCaption())
						.displayOrder(mr.getDisplayOrder() == null ? 0 : mr.getDisplayOrder())
						.build();
				medias.add(m);
			}
			s.setSupplierMedia(medias);
		}

		// Certifications
		if (request.getSupplierCertifications() != null) {
			List<SupplierCertification> certs = new ArrayList<>();
	    for (SupplierCertificationRequest cr : request.getSupplierCertifications()) {
		Certification cert = null;
		// If a nested certification payload is provided, create a new Certification
		if (cr.getCertification() != null) {
		    Certification newCert = Certification.builder()
			    .name(cr.getCertification().getName())
			    .description(cr.getCertification().getDescription())
			    .logoUrl(cr.getCertification().getLogoUrl())
			    .build();
		    cert = certificationRepository.save(newCert);
		} else if (cr.getCertificationId() != null) {
		    cert = certificationRepository.findById(cr.getCertificationId())
			    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND, "Certification not found: " + cr.getCertificationId()));
		} else {
		    throw new AppException(ErrorCode.BAD_REQUEST, "Either certificationId or certification object must be provided");
		}

		SupplierCertificationId scid = SupplierCertificationId.builder()
			.supplierId(null)
			.certificationId(cert.getId())
			.build();

		SupplierCertification sc = SupplierCertification.builder()
			.id(scid)
			.supplier(s)
			.certification(cert)
			.certificateCode(cr.getCertificateCode())
			.expiryDate(cr.getExpiryDate())
			.build();
		certs.add(sc);
	    }
			s.setSupplierCertifications(certs);
		}

		Supplier saved = supplierRepository.save(s);

		// Build simple response
		SupplierResponse resp = SupplierResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.email(saved.getEmail())
				.isActive(saved.getIsActive())
				.build();

		return resp;
	}

	@Override
	public com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2 getSupplierFull(UUID id) {
		// Fetch supplier with media and certifications to ensure collections are initialized and changes are tracked
		Supplier s = null;
		try {
			s = supplierRepository.findByIdWithMediaAndCertifications(id).orElse(null);
		} catch (Exception e) {
			// fallback to simple findById if specialized method is not present/available
			s = supplierRepository.findById(id).orElse(null);
		}
		if (s == null) throw new AppException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier not found: " + id);

		com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2 resp = com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2.builder()
				.id(s.getId())
				.name(s.getName())
				.description(s.getDescription())
				.logoUrl(s.getLogoUrl())
				.address(s.getAddress())
				.email(s.getEmail())
				.phoneNumber(s.getPhoneNumber())
				.isActive(s.getIsActive())
				.createdAt(s.getCreatedAt())
				.updatedAt(s.getUpdatedAt())
				.build();

		if (s.getSupplierMedia() != null) {
			List<com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2.SupplierMediaResp> medias = s.getSupplierMedia().stream().map(m ->
					com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2.SupplierMediaResp.builder()
							.id(m.getId())
							.mediaType(m.getMediaType())
							.mediaUrl(m.getMediaUrl())
							.caption(m.getCaption())
							.displayOrder(m.getDisplayOrder())
							.createdAt(m.getCreatedAt())
							.build()
			).collect(Collectors.toList());
			resp.setSupplierMedia(medias);
		}


		if (s.getSupplierCertifications() != null) {
			List<com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2.SupplierCertificationResp> certs = s.getSupplierCertifications().stream().map(sc ->
					com.greenconnect.greenconnect_api.dtos.response.SupplierRespone2.SupplierCertificationResp.builder()
							.certificationId(sc.getCertification().getId())
							.certificationName(sc.getCertification().getName())
							.certificationDescription(sc.getCertification().getDescription())
							.certificationLogoUrl(sc.getCertification().getLogoUrl())
							.certificateCode(sc.getCertificateCode())
							.expiryDate(sc.getExpiryDate())
							.build()
			).collect(Collectors.toList());
			resp.setSupplierCertifications(certs);
		}

		return resp;
	}

	@Override
	@Transactional
	public com.greenconnect.greenconnect_api.dtos.response.SupplierResponse updateSupplier(UUID id, com.greenconnect.greenconnect_api.dtos.request.UpdateSupplier2 request) {
		Supplier s = supplierRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier not found: " + id));

		// Partial updates for simple fields
		if (request.getName() != null) s.setName(request.getName());
		if (request.getDescription() != null) s.setDescription(request.getDescription());
		if (request.getLogoUrl() != null) s.setLogoUrl(request.getLogoUrl());
		if (request.getAddress() != null) s.setAddress(request.getAddress());
		if (request.getEmail() != null) s.setEmail(request.getEmail());
		if (request.getPhoneNumber() != null) s.setPhoneNumber(request.getPhoneNumber());
		if (request.getIsActive() != null) s.setIsActive(request.getIsActive());

		// Handle media: update existing, create new, remove missing
		if (request.getSupplierMedia() != null) {
			// Map existing media by id
			java.util.Map<UUID, SupplierMedia> existingMap = new java.util.HashMap<>();
			if (s.getSupplierMedia() != null) {
				for (SupplierMedia em : s.getSupplierMedia()) existingMap.put(em.getId(), em);
			}

			List<SupplierMedia> newMediaList = new ArrayList<>();
			for (RequestSupplier.SupplierMediaRequest mr : request.getSupplierMedia()) {
				if (mr.getId() != null && existingMap.containsKey(mr.getId())) {
					// update
					SupplierMedia em = existingMap.get(mr.getId());
					if (mr.getMediaType() != null) em.setMediaType(mr.getMediaType());
					if (mr.getMediaUrl() != null) em.setMediaUrl(mr.getMediaUrl());
					if (mr.getCaption() != null) em.setCaption(mr.getCaption());
					if (mr.getDisplayOrder() != null) em.setDisplayOrder(mr.getDisplayOrder());
					newMediaList.add(em);
					existingMap.remove(mr.getId());
				} else {
					// create new
					SupplierMedia nm = SupplierMedia.builder()
						.supplier(s)
						.mediaType(mr.getMediaType())
						.mediaUrl(mr.getMediaUrl())
						.caption(mr.getCaption())
						.displayOrder(mr.getDisplayOrder() == null ? 0 : mr.getDisplayOrder())
						.build();
					newMediaList.add(nm);
				}
			}
			// Replace contents of the persistent collection so JPA tracks removals (orphanRemoval=true)
			if (s.getSupplierMedia() == null) {
				s.setSupplierMedia(new ArrayList<>());
			}
			List<SupplierMedia> persistentMedia = s.getSupplierMedia();
			persistentMedia.clear();
			for (SupplierMedia nm : newMediaList) {
				nm.setSupplier(s);
				persistentMedia.add(nm);
			}
		}

		// Handle certifications: similar to media; support nested certification creation
		if (request.getSupplierCertifications() != null) {
			java.util.Map<UUID, SupplierCertification> existingCertMap = new java.util.HashMap<>();
			if (s.getSupplierCertifications() != null) {
				for (SupplierCertification sc : s.getSupplierCertifications()) existingCertMap.put(sc.getCertification().getId(), sc);
			}

			List<SupplierCertification> newCertList = new ArrayList<>();
			for (RequestSupplier.SupplierCertificationRequest cr : request.getSupplierCertifications()) {
				Certification cert = null;
				if (cr.getCertification() != null) {
					Certification newCert = Certification.builder()
						.name(cr.getCertification().getName())
						.description(cr.getCertification().getDescription())
						.logoUrl(cr.getCertification().getLogoUrl())
						.build();
					cert = certificationRepository.save(newCert);
				} else if (cr.getCertificationId() != null) {
					cert = certificationRepository.findById(cr.getCertificationId())
						.orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND, "Certification not found: " + cr.getCertificationId()));
				} else {
					throw new AppException(ErrorCode.BAD_REQUEST, "Either certificationId or certification object must be provided");
				}

				SupplierCertification sc = existingCertMap.remove(cert.getId());
				if (sc == null) {
					// create new supplier-cert mapping
					SupplierCertificationId scid = SupplierCertificationId.builder().supplierId(null).certificationId(cert.getId()).build();
					sc = SupplierCertification.builder()
						.id(scid)
						.supplier(s)
						.certification(cert)
						.certificateCode(cr.getCertificateCode())
						.expiryDate(cr.getExpiryDate())
						.build();
				} else {
					// update fields
					if (cr.getCertificateCode() != null) sc.setCertificateCode(cr.getCertificateCode());
					if (cr.getExpiryDate() != null) sc.setExpiryDate(cr.getExpiryDate());
				}
				newCertList.add(sc);
			}
			// Replace contents of the persistent certifications collection so JPA tracks removals
			if (s.getSupplierCertifications() == null) {
				s.setSupplierCertifications(new ArrayList<>());
			}
			List<SupplierCertification> persistentCerts = s.getSupplierCertifications();
			persistentCerts.clear();
			for (SupplierCertification scEntity : newCertList) {
				// ensure supplier reference set
				scEntity.setSupplier(s);
				persistentCerts.add(scEntity);
			}
		}

		Supplier saved = supplierRepository.save(s);

		return com.greenconnect.greenconnect_api.dtos.response.SupplierResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.email(saved.getEmail())
				.isActive(saved.getIsActive())
				.build();
	}

	@Override
	@Transactional
	public com.greenconnect.greenconnect_api.dtos.response.SupplierResponse updateSupplierIsActive(UUID id, Boolean isActive) {
		Supplier s = supplierRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier not found: " + id));
		if (isActive != null) s.setIsActive(isActive);
		Supplier saved = supplierRepository.save(s);
		return com.greenconnect.greenconnect_api.dtos.response.SupplierResponse.builder()
				.id(saved.getId())
				.name(saved.getName())
				.email(saved.getEmail())
				.isActive(saved.getIsActive())
				.build();
	}
	
	@Override
	public List<SupplierResponse> getActiveSuppliersbyCreatedAtDesc() {
		log.info("Lấy danh sách nhà cung cấp active sắp xếp theo thời gian tạo giảm dần");
		List<Supplier> suppliers = supplierRepository.findByIsActiveTrueOrderByCreatedAtDesc();
		return suppliers.stream()
				.map(this::convertToSupplierResponse)
				.collect(Collectors.toList());
	}
	
	@Override
	public List<SupplierResponse> getInactiveSuppliersByCreatedAtDesc() {
		log.info("Lấy danh sách nhà cung cấp inactive sắp xếp theo thời gian tạo giảm dần");
		List<Supplier> suppliers = supplierRepository.findByIsActiveFalseOrderByCreatedAtDesc();
		return suppliers.stream()
				.map(this::convertToSupplierResponse)
				.collect(Collectors.toList());
	}
	
	@Override
	public List<SupplierResponse> getAllSuppliersByCreatedAtDesc() {
		log.info("Lấy tất cả nhà cung cấp sắp xếp theo thời gian tạo giảm dần");
		List<Supplier> suppliers = supplierRepository.findAllByOrderByCreatedAtDesc();
		return suppliers.stream()
				.map(this::convertToSupplierResponse)
				.collect(Collectors.toList());
	}
	
	@Override
	public Page<SupplierResponse> getActiveSuppliersPaginatedByCreatedAt(int page, int size) {
		log.info("Lấy danh sách nhà cung cấp active với phân trang - page: {}, size: {}", page, size);
		Pageable pageable = PageRequest.of(page, size);
		Page<Supplier> suppliers = supplierRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
		return suppliers.map(this::convertToSupplierResponse);
	}
	
	@Override
	public Page<SupplierResponse> getInactiveSuppliersPaginatedByCreatedAt(int page, int size) {
		log.info("Lấy danh sách nhà cung cấp inactive với phân trang - page: {}, size: {}", page, size);
		Pageable pageable = PageRequest.of(page, size);
		Page<Supplier> suppliers = supplierRepository.findByIsActiveFalseOrderByCreatedAtDesc(pageable);
		return suppliers.map(this::convertToSupplierResponse);
	}
	
	@Override
	public Page<SupplierResponse> getAllSuppliersPaginatedByCreatedAt(int page, int size) {
		log.info("Lấy tất cả nhà cung cấp với phân trang - page: {}, size: {}", page, size);
		Pageable pageable = PageRequest.of(page, size);
		Page<Supplier> suppliers = supplierRepository.findAllByOrderByCreatedAtDesc(pageable);
		return suppliers.map(this::convertToSupplierResponse);
	}
	
	@Override
	public Page<SupplierResponse> searchSuppliers(String keyword, String tab, int page, int size) {
		log.info("🔍 Tìm kiếm nhà cung cấp - Từ khóa: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
		
		Pageable pageable = PageRequest.of(page, size);
		Page<Supplier> suppliers;
		
		switch (tab.toLowerCase()) {
			case "active":
				suppliers = supplierRepository.searchActiveSuppliersbyKeyword(keyword, pageable);
				log.info("✅ Tìm kiếm trong nhà cung cấp ACTIVE - Tìm thấy {} kết quả", suppliers.getTotalElements());
				break;
			case "inactive":
				suppliers = supplierRepository.searchInactiveSuppliersByKeyword(keyword, pageable);
				log.info("✅ Tìm kiếm trong nhà cung cấp INACTIVE - Tìm thấy {} kết quả", suppliers.getTotalElements());
				break;
			case "all":
				suppliers = supplierRepository.searchAllSuppliersByKeyword(keyword, pageable);
				log.info("✅ Tìm kiếm trong TẤT CẢ nhà cung cấp - Tìm thấy {} kết quả", suppliers.getTotalElements());
				break;
			default:
				log.warn("⚠️ Tab không hợp lệ: '{}' - Sử dụng 'all' mặc định", tab);
				suppliers = supplierRepository.searchAllSuppliersByKeyword(keyword, pageable);
				break;
		}
		
		return suppliers.map(this::convertToSupplierResponse);
	}
	
	/**
	 * Helper method để convert Supplier entity sang SupplierResponse
	 */
	private SupplierResponse convertToSupplierResponse(Supplier supplier) {
		return SupplierResponse.builder()
				.id(supplier.getId())
				.name(supplier.getName())
				.description(supplier.getDescription())
				.logoUrl(supplier.getLogoUrl())
				.address(supplier.getAddress())
				.email(supplier.getEmail())
				.phoneNumber(supplier.getPhoneNumber())
				.isActive(supplier.getIsActive())
				.createdAt(supplier.getCreatedAt())
				.updatedAt(supplier.getUpdatedAt())
				.build();
	}
}

package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.DeliveryOptionRequest;
import com.greenconnect.greenconnect_api.dtos.response.DeliveryOptionRespone;

import java.util.List;
import java.util.UUID;

public interface DeliveryOptionService {

	DeliveryOptionRespone createDeliveryOption(DeliveryOptionRequest request);

	DeliveryOptionRespone updateDeliveryOption(UUID id, DeliveryOptionRequest request);

	java.util.List<DeliveryOptionRespone> getActiveDeliveryOptions();

	java.util.List<DeliveryOptionRespone> getAllDeliveryOptions();

	// other methods (list, getById, update, delete) can be added later
}

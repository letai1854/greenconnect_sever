package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.ReviewMedia;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, UUID> {
    void deleteByProductReviewId(UUID productReviewId);
}
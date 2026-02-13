package com.greenconnect.greenconnect_api.config;

import org.springframework.stereotype.Component;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JpaEventListener {

    @PrePersist
    public void prePersist(Object entity) {
        log.debug("Creating new entity: {}", entity.getClass().getSimpleName());
    }

    @PostPersist
    public void postPersist(Object entity) {
        log.info("Successfully created entity: {}", entity.getClass().getSimpleName());
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        log.debug("Updating entity: {}", entity.getClass().getSimpleName());
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        log.info("Successfully updated entity: {}", entity.getClass().getSimpleName());
    }

    @PreRemove
    public void preRemove(Object entity) {
        log.debug("Removing entity: {}", entity.getClass().getSimpleName());
    }

    @PostRemove
    public void postRemove(Object entity) {
        log.info("Successfully removed entity: {}", entity.getClass().getSimpleName());
    }

    @PostLoad
    public void postLoad(Object entity) {
        log.debug("Loaded entity from database: {}", entity.getClass().getSimpleName());
    }
}
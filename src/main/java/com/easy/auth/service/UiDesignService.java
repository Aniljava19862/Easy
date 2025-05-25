package com.easy.auth.service;


import com.easy.auth.model.UiDesign;
import com.easy.auth.model.repository.UiDesignRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UiDesignService {

    private final UiDesignRepository uiDesignRepository;

    public UiDesignService(UiDesignRepository uiDesignRepository) {
        this.uiDesignRepository = uiDesignRepository;
    }

    public UiDesign createUiDesign(UiDesign uiDesign) {
        uiDesign.setCreatedAt(LocalDateTime.now());
        uiDesign.setUpdatedAt(LocalDateTime.now());
        // TODO: Set createdByUserId from authenticated user
        return uiDesignRepository.save(uiDesign);
    }

    public List<UiDesign> getAllUiDesigns() {
        return uiDesignRepository.findAll();
    }

    public Optional<UiDesign> getUiDesignById(Long id) {
        return uiDesignRepository.findById(id);
    }

    public UiDesign updateUiDesign(Long id, UiDesign updatedUiDesign) {
        return uiDesignRepository.findById(id)
                .map(existingDesign -> {
                    existingDesign.setName(updatedUiDesign.getName());
                    existingDesign.setDescription(updatedUiDesign.getDescription());
                    existingDesign.setJsonDefinition(updatedUiDesign.getJsonDefinition());
                    existingDesign.setUpdatedAt(LocalDateTime.now());
                    return uiDesignRepository.save(existingDesign);
                })
                .orElseThrow(() -> new RuntimeException("UI Design not found with ID: " + id));
    }

    public void deleteUiDesign(Long id) {
        uiDesignRepository.deleteById(id);
    }
}

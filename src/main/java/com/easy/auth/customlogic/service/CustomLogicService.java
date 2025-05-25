package com.easy.auth.customlogic.service;


import com.easy.auth.customlogic.model.CustomLogic;
import com.easy.auth.customlogic.repository.CustomLogicRepository;
import com.easy.util.GroovySandbox;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class CustomLogicService {

    private final CustomLogicRepository customLogicRepository;
    private final GroovySandbox groovySandbox;

    public CustomLogicService(CustomLogicRepository customLogicRepository, GroovySandbox groovySandbox) {
        this.customLogicRepository = customLogicRepository;
        this.groovySandbox = groovySandbox;
    }

    public CustomLogic createCustomLogic(CustomLogic customLogic) {
        customLogic.setCreatedAt(LocalDateTime.now());
        customLogic.setUpdatedAt(LocalDateTime.now());
        // TODO: Set createdByUserId
        return customLogicRepository.save(customLogic);
    }

    public List<CustomLogic> getAllCustomLogic() {
        return customLogicRepository.findAll();
    }

    public Optional<CustomLogic> getCustomLogicById(Long id) {
        return customLogicRepository.findById(id);
    }

    public CustomLogic updateCustomLogic(Long id, CustomLogic updatedCustomLogic) {
        return customLogicRepository.findById(id)
                .map(existingLogic -> {
                    existingLogic.setName(updatedCustomLogic.getName());
                    existingLogic.setDescription(updatedCustomLogic.getDescription());
                    existingLogic.setCodeContent(updatedCustomLogic.getCodeContent());
                    existingLogic.setLanguage(updatedCustomLogic.getLanguage());
                    existingLogic.setUpdatedAt(LocalDateTime.now());
                    return customLogicRepository.save(existingLogic);
                })
                .orElseThrow(() -> new RuntimeException("Custom Logic not found with ID: " + id));
    }

    public void deleteCustomLogic(Long id) {
        customLogicRepository.deleteById(id);
    }

    /**
     * Executes custom logic.
     * @param id The ID of the custom logic to execute.
     * @param params Parameters to pass to the script (e.g., Map of data).
     * @return Result of the script execution.
     */
    public Object executeCustomLogic(Long id, Map<String, Object> params) {
        CustomLogic logic = customLogicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom Logic not found with ID: " + id));

        if ("GROOVY".equalsIgnoreCase(logic.getLanguage())) {
            return groovySandbox.runScript(logic.getCodeContent(), params);
        } else {
            throw new IllegalArgumentException("Unsupported scripting language: " + logic.getLanguage());
        }
    }
}

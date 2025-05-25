package com.easy.auth.customlogic.controller;


import com.easy.auth.customlogic.model.CustomLogic;
import com.easy.auth.customlogic.service.CustomLogicService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-logic")
public class CustomLogicController {

    private final CustomLogicService customLogicService;

    public CustomLogicController(CustomLogicService customLogicService) {
        this.customLogicService = customLogicService;
    }

    @PostMapping
    public ResponseEntity<CustomLogic> createCustomLogic(@RequestBody CustomLogic customLogic) {
        CustomLogic createdLogic = customLogicService.createCustomLogic(customLogic);
        return new ResponseEntity<>(createdLogic, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomLogic>> getAllCustomLogic() {
        List<CustomLogic> logicList = customLogicService.getAllCustomLogic();
        return ResponseEntity.ok(logicList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomLogic> getCustomLogicById(@PathVariable Long id) {
        return customLogicService.getCustomLogicById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomLogic> updateCustomLogic(@PathVariable Long id, @RequestBody CustomLogic customLogic) {
        CustomLogic updatedLogic = customLogicService.updateCustomLogic(id, customLogic);
        return ResponseEntity.ok(updatedLogic);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomLogic(@PathVariable Long id) {
        customLogicService.deleteCustomLogic(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Object> executeCustomLogic(@PathVariable Long id,
                                                     @RequestBody(required = false) Map<String, Object> params) {
        Object result = customLogicService.executeCustomLogic(id, params != null ? params : Map.of());
        return ResponseEntity.ok(result);
    }
}

package com.easy.auth.controller;



import com.easy.auth.model.UiDesign;
import com.easy.auth.service.UiDesignService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ui-designs")
public class UiDesignController {

    private final UiDesignService uiDesignService;

    public UiDesignController(UiDesignService uiDesignService) {
        this.uiDesignService = uiDesignService;
    }

    @PostMapping
    public ResponseEntity<UiDesign> createUiDesign(@RequestBody UiDesign uiDesign) {
        UiDesign createdDesign = uiDesignService.createUiDesign(uiDesign);
        return new ResponseEntity<>(createdDesign, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UiDesign>> getAllUiDesigns() {
        List<UiDesign> uiDesigns = uiDesignService.getAllUiDesigns();
        return ResponseEntity.ok(uiDesigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UiDesign> getUiDesignById(@PathVariable Long id) {
        return uiDesignService.getUiDesignById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UiDesign> updateUiDesign(@PathVariable Long id, @RequestBody UiDesign uiDesign) {
        UiDesign updatedDesign = uiDesignService.updateUiDesign(id, uiDesign);
        return ResponseEntity.ok(updatedDesign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUiDesign(@PathVariable Long id) {
        uiDesignService.deleteUiDesign(id);
        return ResponseEntity.noContent().build();
    }
}

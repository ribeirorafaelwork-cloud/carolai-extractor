package com.carolai.extractor.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.facade.CustomerExtractorFacade;

@RestController
@RequestMapping("/customer/")
public class CustomerExtractorController {

    private static final Logger log = LogManager.getLogger(TrainingExtractorController.class);

    @Autowired
    private final CustomerExtractorFacade customerExtractorFacade;

    public CustomerExtractorController(CustomerExtractorFacade customerExtractorFacade) {
        this.customerExtractorFacade = customerExtractorFacade;
    }

    @GetMapping("extractAndSave")
    public ResponseEntity<Void> extractAndSave() {
        customerExtractorFacade.extractAndSave();
        
        return ResponseEntity.ok().build();
    }
}

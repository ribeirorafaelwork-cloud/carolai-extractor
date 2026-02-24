package com.carolai.extractor.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.facade.CustomerRelTrainingExtractorFacade;

@RestController
@RequestMapping("/customer/rel/training/")
public class CustomerRelTrainingExtractorController {

    private static final Logger log = LogManager.getLogger(CustomerRelTrainingExtractorController.class);

    @Autowired
    private final CustomerRelTrainingExtractorFacade customerRelTrainingExtractorFacade;

    public CustomerRelTrainingExtractorController(CustomerRelTrainingExtractorFacade customerRelTrainingExtractorFacade) {
        this.customerRelTrainingExtractorFacade = customerRelTrainingExtractorFacade;
    }

    @GetMapping("extractAndSave")
    public ResponseEntity<Void> extractAndSave() {
        customerRelTrainingExtractorFacade.extractAndSave();
        
        return ResponseEntity.ok().build();
    }
}
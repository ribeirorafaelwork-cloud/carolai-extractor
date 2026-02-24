package com.carolai.extractor.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.facade.TrainingExtractorFacade;

@RestController
@RequestMapping("/training/")
public class TrainingExtractorController {

    private static final Logger log = LogManager.getLogger(TrainingExtractorController.class);

    @Autowired
    private final TrainingExtractorFacade trainingExtractorFacade;

    public TrainingExtractorController(TrainingExtractorFacade trainingExtractorFacade) {
        this.trainingExtractorFacade = trainingExtractorFacade;
    }

    @GetMapping("extractAndSave")
    public ResponseEntity<Void> extractAndSave() {
        trainingExtractorFacade.extractAndSave();
        
        return ResponseEntity.ok().build();
    }
}

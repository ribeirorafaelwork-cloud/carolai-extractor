package com.carolai.extractor.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolai.extractor.facade.PhysicalAssessmentFacade;

@RestController
@RequestMapping("/physicalAssessment/")
public class PhysicalAssessmentController {

    private static final Logger log = LogManager.getLogger(PhysicalAssessmentController.class);

    @Autowired
    private final PhysicalAssessmentFacade physicalAssessmentFacade;

    public PhysicalAssessmentController(PhysicalAssessmentFacade physicalAssessmentFacade) {
        this.physicalAssessmentFacade = physicalAssessmentFacade;
    }

    @GetMapping("extractAndSave")
    public ResponseEntity<Void> extractAndSave() {
        physicalAssessmentFacade.extractAndSave();
        
        return ResponseEntity.ok().build();
    }
}
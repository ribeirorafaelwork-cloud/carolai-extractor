package com.carolai.extractor.facade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.carolai.extractor.service.TrainingService;

@Component
public class TrainingExtractorFacade {

    private static final Logger log = LogManager.getLogger(TrainingExtractorFacade.class);

    @Autowired
    private TrainingService trainingService;

    public void extractAndSave() {
        trainingService.extractAndSave();
    }

}


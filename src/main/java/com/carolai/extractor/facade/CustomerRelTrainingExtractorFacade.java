package com.carolai.extractor.facade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.carolai.extractor.service.TrainingPlanService;
import com.carolai.extractor.service.TrainingPlanTrainingService;

@Component
public class CustomerRelTrainingExtractorFacade {

    private static final Logger log = LogManager.getLogger(CustomerRelTrainingExtractorFacade.class);

    @Autowired
    private TrainingPlanService trainingPlanService;

    @Autowired
    private TrainingPlanTrainingService trainingPlanTrainingService;

    public void extractAndSave() {
        trainingPlanService.extractAndSave();
        trainingPlanTrainingService.extractAndSave();
    }
}
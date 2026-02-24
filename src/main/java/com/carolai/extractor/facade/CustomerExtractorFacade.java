package com.carolai.extractor.facade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.carolai.extractor.service.CustomerService;

@Component
public class CustomerExtractorFacade {

    private static final Logger log = LogManager.getLogger(CustomerExtractorFacade.class);

    @Autowired
    private CustomerService customerService;

    public void extractAndSave() {
        customerService.extractAndSave();
    }

}


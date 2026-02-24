package com.carolai.extractor.service;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carolai.extractor.browser.BrowserEngine;
import com.carolai.extractor.browser.PersonalFitSession;
import com.microsoft.playwright.BrowserContext;

@Service
public class LoginService {

    private static final Logger log = LogManager.getLogger(LoginService.class);

    @Autowired
    private PersonalFitSession personalFitSession;

    public void login() {
        try {
            BrowserContext ctx = BrowserEngine.createContext();
            personalFitSession.login(ctx);
        } catch (IOException e) {
            log.error("❌ Erro durante o login: {}", e.getMessage(), e);
        }
    }
}

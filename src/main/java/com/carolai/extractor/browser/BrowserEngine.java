package com.carolai.extractor.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.Arrays;

public class BrowserEngine {

    private static Playwright playwright;
    private static Browser browser;

    public static synchronized BrowserContext createContext() {
        if (playwright == null) {
            playwright = Playwright.create();
        }

        if (browser == null) {
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-web-security",
                        "--disable-features=IsolateOrigins,site-per-process"
                    ))
            );
        }

        return browser.newContext(
            new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(true)
        );
    }
}
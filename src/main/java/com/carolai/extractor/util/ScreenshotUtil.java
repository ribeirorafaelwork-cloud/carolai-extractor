package com.carolai.extractor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.microsoft.playwright.Page;

public class ScreenshotUtil {

    private static final Logger log = LogManager.getLogger(ScreenshotUtil.class);

    private static final String SCREENSHOT_DIR = "/app/screenshots";

    public static Path takeScreenshot(Page page, String namePrefix) {
        try {
            Files.createDirectories(Paths.get(SCREENSHOT_DIR));

            Path screenshotPath = Paths.get(
                    SCREENSHOT_DIR,
                    namePrefix + "_" + System.currentTimeMillis() + ".png"
            );

            page.screenshot(
                new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true)
            );

            log.info("📸 Screenshot salvo em: {}", screenshotPath);
            return screenshotPath;

        } catch (IOException e) {
            log.error("❌ Erro ao salvar screenshot: {}", e.getMessage(), e);
            return null;
        }
    }
}

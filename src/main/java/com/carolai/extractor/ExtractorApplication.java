package com.carolai.extractor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ExtractorApplication {

    private static final Logger log = LogManager.getLogger(ExtractorApplication.class);

    public static void main(String[] args) {

        log.info("🚀 Iniciando carolaiextractor...");
        log.info("🔧 Carregando contexto do Spring…");

        ConfigurableApplicationContext ctx = SpringApplication.run(ExtractorApplication.class, args);
        String[] profiles = ctx.getEnvironment().getActiveProfiles();
        
        log.info("✅ Aplicação carolaiextractor iniciada com sucesso!");
        log.info("📡 Endpoints ativos:");
        log.info("   → Test Login:        http://localhost:8080/test/login");
        log.info("   → Actuator Health:   http://localhost:8080/actuator/health");

        log.info("📚 Ambiente de execução:");
        log.info("   → Java Version: {}", System.getProperty("java.version"));
        log.info("   → Active Profiles: {}", profiles.length == 0 ? "default" : String.join(", ", profiles));
    }
}
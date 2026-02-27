package com.carolai.extractor.dashboard;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.carolai.extractor.facade.CustomerExtractorFacade;
import com.carolai.extractor.facade.CustomerRelTrainingExtractorFacade;
import com.carolai.extractor.facade.PhysicalAssessmentFacade;
import com.carolai.extractor.facade.TrainingExtractorFacade;
import com.carolai.extractor.outbox.OutboxPopulationService;

@Component
public class ExtractionOrchestrator {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String APPENDER_NAME = "sse-bridge";

    private final CustomerExtractorFacade customerFacade;
    private final TrainingExtractorFacade trainingFacade;
    private final CustomerRelTrainingExtractorFacade trainingPlanFacade;
    private final PhysicalAssessmentFacade assessmentFacade;
    private final OutboxPopulationService outboxService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ExtractionOrchestrator(
            CustomerExtractorFacade customerFacade,
            TrainingExtractorFacade trainingFacade,
            CustomerRelTrainingExtractorFacade trainingPlanFacade,
            PhysicalAssessmentFacade assessmentFacade,
            OutboxPopulationService outboxService
    ) {
        this.customerFacade = customerFacade;
        this.trainingFacade = trainingFacade;
        this.trainingPlanFacade = trainingPlanFacade;
        this.assessmentFacade = assessmentFacade;
        this.outboxService = outboxService;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void run(SseEmitter emitter) {
        if (!running.compareAndSet(false, true)) {
            sendEvent(emitter, "error", "{\"message\":\"Extraction already running\"}");
            completeEmitter(emitter);
            return;
        }

        LoggerContext logbackCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = logbackCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        AppenderBase<ILoggingEvent> appender = createSseAppender(emitter, logbackCtx);

        try {
            appender.start();
            rootLogger.addAppender(appender);

            long totalStart = System.currentTimeMillis();
            boolean allSuccess = true;

            allSuccess &= executeStep(emitter, "CUSTOMERS", () -> customerFacade.extractAndSave());
            allSuccess &= executeStep(emitter, "TRAININGS", () -> trainingFacade.extractAndSave());
            allSuccess &= executeStep(emitter, "TRAINING_PLANS", () -> trainingPlanFacade.extractAndSave());
            allSuccess &= executeStep(emitter, "ASSESSMENTS", () -> assessmentFacade.extractAndSave());
            allSuccess &= executeStep(emitter, "OUTBOX_STUDENT", () -> outboxService.populate("STUDENT"));
            allSuccess &= executeStep(emitter, "OUTBOX_TRAINING", () -> outboxService.populate("TRAINING_HISTORY"));
            allSuccess &= executeStep(emitter, "OUTBOX_ASSESSMENT", () -> outboxService.populate("PHYSICAL_ASSESSMENT"));
            allSuccess &= executeStep(emitter, "OUTBOX_OBJECTIVE", () -> outboxService.populate("OBJECTIVE"));
            allSuccess &= executeStep(emitter, "OUTBOX_EXERCISE", () -> outboxService.populate("EXERCISE"));

            long totalMs = System.currentTimeMillis() - totalStart;
            sendEvent(emitter, "complete",
                    "{\"success\":" + allSuccess + ",\"totalMs\":" + totalMs + "}");

        } finally {
            rootLogger.detachAppender(appender);
            appender.stop();
            running.set(false);
            completeEmitter(emitter);
        }
    }

    private boolean executeStep(SseEmitter emitter, String name, Runnable action) {
        sendEvent(emitter, "step",
                "{\"name\":\"" + name + "\",\"status\":\"running\"}");

        long start = System.currentTimeMillis();
        try {
            action.run();
            long ms = System.currentTimeMillis() - start;
            sendEvent(emitter, "step",
                    "{\"name\":\"" + name + "\",\"status\":\"ok\",\"durationMs\":" + ms + "}");
            return true;
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Unknown error";
            sendEvent(emitter, "step",
                    "{\"name\":\"" + name + "\",\"status\":\"error\",\"durationMs\":" + ms
                            + ",\"error\":\"" + errorMsg + "\"}");
            return false;
        }
    }

    private AppenderBase<ILoggingEvent> createSseAppender(SseEmitter emitter, LoggerContext logbackCtx) {
        AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
            @Override
            protected void append(ILoggingEvent event) {
                String loggerName = event.getLoggerName();
                if (loggerName == null || !loggerName.startsWith("com.carolai.extractor")) {
                    return;
                }

                String shortLogger = loggerName.substring(loggerName.lastIndexOf('.') + 1);
                String level = event.getLevel().toString();
                String message = event.getFormattedMessage();
                if (message != null) {
                    message = message.replace("\\", "\\\\").replace("\"", "\\\"")
                            .replace("\n", "\\n").replace("\r", "");
                }
                String timestamp = LocalTime.now().format(TIME_FMT);

                String json = "{\"level\":\"" + level
                        + "\",\"logger\":\"" + shortLogger
                        + "\",\"message\":\"" + message
                        + "\",\"timestamp\":\"" + timestamp + "\"}";

                sendEvent(emitter, "log", json);
            }
        };
        appender.setName(APPENDER_NAME);
        appender.setContext(logbackCtx);
        return appender;
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // Client disconnected — continue extraction, just stop sending
        }
    }

    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}

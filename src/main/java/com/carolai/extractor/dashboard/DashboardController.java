package com.carolai.extractor.dashboard;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/extraction")
public class DashboardController {

    private final ExtractionOrchestrator orchestrator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DashboardController(ExtractionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream() {
        if (orchestrator.isRunning()) {
            return ResponseEntity.status(409).build();
        }

        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> orchestrator.run(emitter));
        return ResponseEntity.ok(emitter);
    }
}

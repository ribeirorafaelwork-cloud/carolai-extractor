package com.carolai.extractor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external-api")
public class ExternalApiProperties {
        private String physicalAssessmentToken;
        private String physicalAssessmentEndpoint;
        private String physicalAssessmentBaseUrl;

        public String getPhysicalAssessmentToken() {
                return physicalAssessmentToken;
        }

        public void setPhysicalAssessmentToken(String physicalAssessmentToken) {
                this.physicalAssessmentToken = physicalAssessmentToken;
        }

        public String getPhysicalAssessmentEndpoint() {
                return physicalAssessmentEndpoint;
        }

        public void setPhysicalAssessmentEndpoint(String physicalAssessmentEndpoint) {
                this.physicalAssessmentEndpoint = physicalAssessmentEndpoint;
        }

        public String getPhysicalAssessmentBaseUrl() {
                return physicalAssessmentBaseUrl;
        }

        public void setPhysicalAssessmentBaseUrl(String physicalAssessmentBaseUrl) {
                this.physicalAssessmentBaseUrl = physicalAssessmentBaseUrl;
        }
        
}
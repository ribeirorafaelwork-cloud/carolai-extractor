package com.carolai.extractor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "firestore")
public class FirestoreProperties {
        private String baseUrl;
        private String trainingCollection;
        private String customerCollection;
        private String trainingPlanCollection;
        private String trainingPlanTrainingSubcollection;
        private String personalId;
        private String typeProfile;
        private String idClientApp;

        public String getBaseUrl() {
                return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
        }


        public String getTrainingCollection() {
                return trainingCollection;
        }

        public void setTrainingCollection(String trainingCollection) {
                this.trainingCollection = trainingCollection;
        }

        public String getCustomerCollection() {
                return customerCollection;
        }

        public void setCustomerCollection(String customerCollection) {
                this.customerCollection = customerCollection;
        }

        public String getPersonalId() {
                return personalId;
        }
        
        public void setPersonalId(String personalId) {
                this.personalId = personalId;
        }

        public String getTypeProfile() {
                return typeProfile;
        }

        public void setTypeProfile(String typeProfile) {
                this.typeProfile = typeProfile;
        }

        public String getIdClientApp() {
                return idClientApp;
        }

        public void setIdClientApp(String idClientApp) {
                this.idClientApp = idClientApp;
        }

        public String getTrainingPlanCollection() {
                return trainingPlanCollection;
        }

        public void setTrainingPlanCollection(String trainingPlanCollection) {
                this.trainingPlanCollection = trainingPlanCollection;
        }

        public String getTrainingPlanTrainingSubcollection() {
                return trainingPlanTrainingSubcollection;
        }

        public void setTrainingPlanTrainingSubcollection(String trainingPlanTrainingSubcollection) {
                this.trainingPlanTrainingSubcollection = trainingPlanTrainingSubcollection;
        }
        
}
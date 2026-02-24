package com.carolai.extractor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "extractor.api.auth")
public class AuthApiProperties {

    private String url;
    private String key;
    private String email;
    private String password;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; } 

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

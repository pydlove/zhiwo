package com.example.blogger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private VisibleTabs visibleTabs = new VisibleTabs();

    public VisibleTabs getVisibleTabs() {
        return visibleTabs;
    }

    public void setVisibleTabs(VisibleTabs visibleTabs) {
        this.visibleTabs = visibleTabs;
    }

    public List<String> getProcessManageVisibleTabs() {
        if (visibleTabs == null || visibleTabs.processManage == null || visibleTabs.processManage.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(visibleTabs.processManage.split(","));
    }

    public static class VisibleTabs {
        private String processManage;

        public String getProcessManage() {
            return processManage;
        }

        public void setProcessManage(String processManage) {
            this.processManage = processManage;
        }
    }
}

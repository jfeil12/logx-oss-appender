package org.logx.compatibility.config;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置一致性验证报告
 */
public class ConfigConsistencyReport {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigConsistencyReport.class);

    private final List<ConsistentParameter> consistentParameters = new ArrayList<>();
    private final List<InconsistentParameter> inconsistentParameters = new ArrayList<>();

    public void addConsistentParameter(String framework, String parameter) {
        consistentParameters.add(new ConsistentParameter(framework, parameter, ""));
    }

    public void addConsistentParameterWithDescription(String parameter, String description) {
        consistentParameters.add(new ConsistentParameter("", parameter, description));
    }

    public void addInconsistentParameter(String framework, String parameter, String issue) {
        inconsistentParameters.add(new InconsistentParameter(framework, parameter, issue));
    }

    public List<ConsistentParameter> getConsistentParameters() {
        return new ArrayList<>(consistentParameters);
    }

    public List<InconsistentParameter> getInconsistentParameters() {
        return new ArrayList<>(inconsistentParameters);
    }

    public boolean isConsistent() {
        return inconsistentParameters.isEmpty();
    }

    public void printReport() {
        logger.info("=== 配置一致性验证报告 ===");
        
        logger.info("\n一致的配置参数:");
        for (ConsistentParameter param : consistentParameters) {
            if (param.getFramework().isEmpty()) {
                logger.info("  - {}: {}", param.getParameter(), param.getDescription());
            } else {
                logger.info("  - {}: {}", param.getFramework(), param.getParameter());
            }
        }
        
        logger.info("\n不一致的配置参数:");
        for (InconsistentParameter param : inconsistentParameters) {
            logger.info("  - {}: {} ({})", param.getFramework(), param.getParameter(), param.getIssue());
        }
        
        logger.info("\n总体结果: {}", (isConsistent() ? "一致" : "不一致"));
    }

    /**
     * 合并另一个报告的结果
     */
    public void merge(ConfigConsistencyReport other) {
        this.consistentParameters.addAll(other.consistentParameters);
        this.inconsistentParameters.addAll(other.inconsistentParameters);
    }

    /**
     * 一致的配置参数
     */
    public static class ConsistentParameter {
        private final String framework;
        private final String parameter;
        private final String description;

        public ConsistentParameter(String framework, String parameter, String description) {
            this.framework = framework;
            this.parameter = parameter;
            this.description = description;
        }

        public String getFramework() {
            return framework;
        }

        public String getParameter() {
            return parameter;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 不一致的配置参数
     */
    public static class InconsistentParameter {
        private final String framework;
        private final String parameter;
        private final String issue;

        public InconsistentParameter(String framework, String parameter, String issue) {
            this.framework = framework;
            this.parameter = parameter;
            this.issue = issue;
        }

        public String getFramework() {
            return framework;
        }

        public String getParameter() {
            return parameter;
        }

        public String getIssue() {
            return issue;
        }
    }
}
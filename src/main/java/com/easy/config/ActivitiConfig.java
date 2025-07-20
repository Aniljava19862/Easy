/*package com.easy.config;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ActivitiConfig {

    private final DataSource dataSource;

    public ActivitiConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public ProcessEngineConfiguration processEngineConfiguration() {
        ProcessEngineConfigurationImpl config = new ProcessEngineConfigurationImpl() {
            @Override
            public CommandInterceptor createTransactionInterceptor() {
                return null;
            }
        };
        config.setDataSource(dataSource);
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false); // Can be true for asynchronous execution
        // Add other configurations as needed
        return config;
    }
}
*/
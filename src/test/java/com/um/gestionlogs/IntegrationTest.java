package com.um.gestionlogs;

import com.um.gestionlogs.config.AsyncSyncConfiguration;
import com.um.gestionlogs.config.EmbeddedSQL;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { GestionlogsApp.class, AsyncSyncConfiguration.class, com.um.gestionlogs.config.JacksonHibernateConfiguration.class }
)
@EmbeddedSQL
public @interface IntegrationTest {}

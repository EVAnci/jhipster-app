package com.um.gestionlogs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LogGenerator {

    private static final Logger log = LoggerFactory.getLogger(LogGenerator.class);

    @EventListener(ApplicationReadyEvent.class)
    public void generateLogs() {
        log.debug("Mensaje de depuracion");
        log.info("Aplicacion iniciada correctamente");
        log.warn("Advertencia de prueba");
        log.error("Error simulado para pruebas");
    }
}

package com.bookplus.inventory.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el job de Spring Batch que libera las reservas vencidas, cada 5 minutos.
 * El parámetro de tiempo hace que cada ejecución sea única (Spring Batch no re-ejecuta
 * un job con los mismos parámetros).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpireReservationsScheduler {

    private final JobLauncher jobLauncher;
    private final Job         expireReservationsJob;

    @Scheduled(fixedDelay = 5 * 60 * 1000)  // cada 5 minutos
    public void launch() {
        try {
            jobLauncher.run(expireReservationsJob, new JobParametersBuilder()
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception ex) {
            log.error("No se pudo lanzar expireReservationsJob: {}", ex.getMessage());
        }
    }
}

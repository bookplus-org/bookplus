package com.bookplus.inventory.config;

import com.bookplus.inventory.application.batch.ReservationExpiryService;
import com.bookplus.inventory.domain.model.StockReservation;
import com.bookplus.inventory.domain.port.out.LoadReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;

/**
 * Job de Spring Batch "expireReservationsJob": libera las reservas de stock vencidas.
 *
 * Procesa por chunks (lotes) de 50, es tolerante a fallos (salta y registra errores
 * sin abortar todo el job) y reiniciable, ya que Spring Batch persiste el estado de
 * cada ejecución en sus tablas de metadatos. Lo dispara {@code ReservationExpiryJobLauncher}.
 */
@Configuration
@RequiredArgsConstructor
public class ReservationExpiryBatchConfig {

    private static final int CHUNK = 50;

    private final LoadReservationPort       loadReservationPort;
    private final ReservationExpiryService  expiryService;

    /** Lee las reservas PENDING vencidas en el momento de arrancar el step. */
    @Bean
    @StepScope
    public ItemReader<StockReservation> expiredReservationReader() {
        return new IteratorItemReader<>(loadReservationPort.findExpiredPending(Instant.now()));
    }

    /** Defensa: solo procesa las que siguen PENDING (descarta las ya resueltas). */
    @Bean
    public ItemProcessor<StockReservation, StockReservation> pendingOnlyProcessor() {
        return reservation -> reservation.isPending() ? reservation : null;
    }

    /** Persiste la liberación delegando en el servicio de dominio. */
    @Bean
    public ItemWriter<StockReservation> reservationExpiryWriter() {
        return chunk -> chunk.forEach(expiryService::expire);
    }

    @Bean
    public Step expireReservationsStep(JobRepository jobRepository,
                                       PlatformTransactionManager txManager,
                                       ItemReader<StockReservation> expiredReservationReader,
                                       ItemProcessor<StockReservation, StockReservation> pendingOnlyProcessor,
                                       ItemWriter<StockReservation> reservationExpiryWriter) {
        return new StepBuilder("expireReservationsStep", jobRepository)
                .<StockReservation, StockReservation>chunk(CHUNK, txManager)
                .reader(expiredReservationReader)
                .processor(pendingOnlyProcessor)
                .writer(reservationExpiryWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public Job expireReservationsJob(JobRepository jobRepository, Step expireReservationsStep) {
        return new JobBuilder("expireReservationsJob", jobRepository)
                .start(expireReservationsStep)
                .build();
    }
}

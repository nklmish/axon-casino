package com.nklmish.demo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nklmish.demo.process.WithdrawalApprovalSaga
import io.axoniq.axonhub.client.AxonHubConfiguration
import io.axoniq.axonhub.client.PlatformConnectionManager
import io.axoniq.axonhub.client.event.axon.AxonHubEventStore
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.EventHandlingConfiguration
import org.axonframework.config.SagaConfiguration
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.eventhandling.scheduling.java.SimpleEventScheduler
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.*
import java.util.concurrent.Executors


@SpringBootApplication
@EnableScheduling
class AxonCasinoApplication {

    @Autowired
    fun config(eventHandlingConfiguration: EventHandlingConfiguration) {
        eventHandlingConfiguration.usingTrackingProcessors()
    }

    @Bean
    @Profile("process")
    fun withdrawalApprovalSagaConfiguration(): SagaConfiguration<WithdrawalApprovalSaga> {
        return SagaConfiguration.trackingSagaManager(WithdrawalApprovalSaga::class.java)
    }

    @Bean
    @Profile("process", "game")
    fun eventScheduler(eventBus: EventBus, transactionManager: TransactionManager): EventScheduler {
        return SimpleEventScheduler(Executors.newSingleThreadScheduledExecutor(), eventBus, transactionManager)
    }

    @Bean
    fun eventSerializer(): JacksonSerializer {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(KotlinModule())
        return JacksonSerializer(objectMapper)
    }

    @Bean
    fun eventStore(axonHubConfiguration: AxonHubConfiguration,
                   platformConnectionManager: PlatformConnectionManager,
                   @Qualifier("eventSerializer") serializer: Serializer): EventStore {
        return AxonHubEventStore(axonHubConfiguration, platformConnectionManager, serializer)
    }

}

fun main(args: Array<String>) {
    runApplication<AxonCasinoApplication>(*args)
}

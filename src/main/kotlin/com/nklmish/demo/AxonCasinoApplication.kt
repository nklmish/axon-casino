package com.nklmish.demo

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nklmish.demo.process.WithdrawalApprovalSaga
import com.thoughtworks.xstream.XStream
import io.axoniq.axonhub.client.boot.EventStoreAutoConfiguration
import io.axoniq.axonhub.client.boot.MessagingAutoConfiguration
import io.axoniq.axonhub.client.boot.SubscriptionQueryAutoConfiguration
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.config.SagaConfiguration
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.eventhandling.scheduling.java.SimpleEventScheduler
import org.axonframework.eventhandling.scheduling.quartz.EventJobDataBinder
import org.axonframework.eventhandling.scheduling.quartz.QuartzEventScheduler
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.SimpleSerializedObject
import org.axonframework.serialization.SimpleSerializedType
import org.axonframework.serialization.json.JacksonSerializer
import org.axonframework.serialization.xml.XStreamSerializer
import org.quartz.JobDataMap
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.stereotype.Component
import java.util.concurrent.Executors


@Configuration
@ComponentScan
@EnableScheduling
class AxonCasinoApplication {

    @Autowired
    fun config(eventProcessingConfiguration: EventProcessingConfiguration) {
        eventProcessingConfiguration.usingTrackingProcessors()
    }

    @Bean
    @Profile("process")
    fun withdrawalApprovalSagaConfiguration(): SagaConfiguration<WithdrawalApprovalSaga> {
        return SagaConfiguration.trackingSagaManager(WithdrawalApprovalSaga::class.java)
    }

//    @Bean
//    @Profile("process", "game")
//    fun eventScheduler(eventBus: EventBus, transactionManager: TransactionManager): EventScheduler {
//        return SimpleEventScheduler(Executors.newSingleThreadScheduledExecutor(), eventBus, transactionManager)
//    }

    @Bean
    @Profile("process", "game")
    fun eventScheduler(eventBus: EventBus, transactionManager: TransactionManager, scheduler: Scheduler, eventJobDataBinder: EventJobDataBinder): EventScheduler {
        val quartzEventScheduler = QuartzEventScheduler()
        quartzEventScheduler.setEventBus(eventBus)
        quartzEventScheduler.setTransactionManager(transactionManager)
        quartzEventScheduler.setScheduler(scheduler)
        quartzEventScheduler.setEventJobDataBinder(eventJobDataBinder)
        return quartzEventScheduler
    }

    @Bean
    fun eventSerializer(objectMapper: ObjectMapper): JacksonSerializer {
        return JacksonSerializer(objectMapper)
    }

    @Bean
    fun kotlinModule(): Module {
        return KotlinModule()
    }

    @Autowired
    fun configure(serializer: Serializer) {
        if (serializer is XStreamSerializer) {
            val xStream = serializer.xStream
            XStream.setupDefaultSecurity(xStream)
            xStream.allowTypesByWildcard(arrayOf("org.axonframework.**", "com.nklmish.demo.**"))
        }
    }

}

@Component
class SchedulerNameCustomizer(@Value("\${spring.profiles.active}") private val activeProfiles: String) : SchedulerFactoryBeanCustomizer {
    override fun customize(schedulerFactoryBean: SchedulerFactoryBean?) {
        println(schedulerFactoryBean)
        schedulerFactoryBean?.setSchedulerName(activeProfiles)
    }
}

@Component
class AxonSerializerEventJobDataBinder(private val serializer: Serializer) : EventJobDataBinder {
    override fun toJobData(eventMessage: Any): JobDataMap {
        val serializedEvent = serializer.serialize(eventMessage, String::class.java)
        val jobData = JobDataMap()
        jobData["data"] = serializedEvent.data
        jobData["type.name"] = serializedEvent.type.name
        jobData["type.revision"] = serializedEvent.type.revision
        return jobData
    }

    override fun fromJobData(jobData: JobDataMap): Any {
        val type = SimpleSerializedType(jobData["type.name"] as String?, jobData["type.revision"] as String?)
        val serializedEvent = SimpleSerializedObject(jobData["data"] as String?, String::class.java, type)
        return serializer.deserialize(serializedEvent)
    }
}

@Configuration
@Profile("axonhub")
@EnableAutoConfiguration
class EnableHubAutoconfig

@Configuration
@Profile("!axonhub")
@EnableAutoConfiguration(exclude = arrayOf(MessagingAutoConfiguration::class, EventStoreAutoConfiguration::class, SubscriptionQueryAutoConfiguration::class))
class DisableHubAutoconfig

fun main(args: Array<String>) {
    runApplication<AxonCasinoApplication>(*args)
}

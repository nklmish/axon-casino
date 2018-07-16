package com.nklmish.demo

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.nklmish.demo.process.WithdrawalApprovalSaga
import com.thoughtworks.xstream.XStream
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.EventProcessingConfiguration
import org.axonframework.config.SagaConfiguration
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.scheduling.EventScheduler
import org.axonframework.eventhandling.scheduling.quartz.EventJobDataBinder
import org.axonframework.eventhandling.scheduling.quartz.QuartzEventScheduler
import org.axonframework.eventhandling.scheduling.quartz.QuartzEventScheduler.DirectEventJobDataBinder
import org.axonframework.serialization.SerializedObject
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.SimpleSerializedObject
import org.axonframework.serialization.SimpleSerializedType
import org.axonframework.serialization.json.JacksonSerializer
import org.axonframework.serialization.xml.XStreamSerializer
import org.quartz.JobDataMap
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component


@SpringBootApplication
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

fun main(args: Array<String>) {
    runApplication<AxonCasinoApplication>(*args)
}

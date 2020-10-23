package com.sahlone.mmq.config

import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.logging.createContextualLogger
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory

object ValidatedAppConfig : () -> AppConfig {
    override fun invoke(): AppConfig {
        val logger = createContextualLogger()
        try {
            val config = ConfigFactory.load().getConfig("memoryMappedQueue")
            val baseQueueDir: String = config.getString("baseQueueDir")
            val maxQueueSizeBytes: Int = config.getInt("maxQueueSizeBytes")
            val queueName: String = config.getString("queueName")
            return AppConfig(baseQueueDir, maxQueueSizeBytes, queueName)
        } catch (err: ConfigException) {
            logger.errorWithCause(TracingContext(), err) { "Error initialising application. Please check the config." }
            throw err
        }
    }
}

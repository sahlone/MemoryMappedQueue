package com.sahlone.mmq.perftest

import com.sahlone.mmq.MMQueue
import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.perftest.utils.perfTestResult
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

class EnqueuePerfTest : BehaviorSpec({
    val config = AppConfig("./perftest/", 200000000, "perftest-${UUID.randomUUID()}")
    afterTest { _ ->
        File(config.baseQueueDir).deleteRecursively()
    }
    given("a memory mapped queue") {
        `when`("a certain no of messages are inserted") {
            then("test performance") {
                val tracingContext = TracingContext()
                val queue = MMQueue(config)
                val start = System.nanoTime()
                val noOfOperations = 10000000L
                repeat((0 until noOfOperations).count()) {
                    queue.enqueue(tracingContext, UUID.randomUUID().toString().toByteArray())
                }
                val stop = System.nanoTime()
                perfTestResult(start, stop, "Enqueue", noOfOperations)
            }
        }
    }
})

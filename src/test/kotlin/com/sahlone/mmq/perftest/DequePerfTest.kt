package com.sahlone.mmq.perftest

import com.sahlone.mmq.queue.MMQueue
import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.perftest.utils.perfTestResult
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File
import java.util.UUID

class DequePerfTest : BehaviorSpec({
    val config = AppConfig("perftest/", 2000000000, "perftest-${UUID.randomUUID()}")
    afterTest { _ ->
        File(config.baseQueueDir).deleteRecursively()
    }
    given("a memory mapped queue") {
        `when`("a certain no of messages are inserted") {
            then("test performance") {
                val tracingContext = TracingContext()
                val queue = MMQueue(config)
                val noOfOperations = 10000000L
                repeat((0 until noOfOperations).count()) {
                    queue.enqueue(tracingContext, UUID.randomUUID().toString().toByteArray())
                }
                val start = System.nanoTime()
                repeat((0 until noOfOperations).count()) {
                    queue.dequeue(tracingContext)
                }
                val stop = System.nanoTime()
                perfTestResult(start, stop, "Dequeue", noOfOperations)
            }
        }
    }
})

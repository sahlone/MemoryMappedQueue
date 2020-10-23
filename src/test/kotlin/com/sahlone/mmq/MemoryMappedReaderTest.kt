package com.sahlone.mmq

import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.models.EnqDeqResultType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.fp.success
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail
import java.io.File
import java.util.UUID

class MemoryMappedReaderTest : BehaviorSpec({

    val filesize = 200
    val baseConfig = AppConfig("perftest/", filesize, "perftest-${UUID.randomUUID()}")
    val tracingContext = TracingContext()
    afterEach { _ ->
        File(baseConfig.baseQueueDir).deleteRecursively()
    }
    given("a memory mapped queue") {
        `when`("the queue is empty") {
            then("fail to retrieve data") {
                val queue = MMQueue(baseConfig)
                when (val result = queue.dequeue(tracingContext)) {
                    is EnqDeqResult.Companion.Success -> fail("expected a failure")
                    is EnqDeqResult.Companion.Failure -> {
                        result shouldBe EnqDeqResult.Companion.Failure.Companion.QueueEmpty(EnqDeqResultType.DEQUEUE)
                    }
                }
            }
        }
        `when`("the queue has data") {
            then("successfully retrieve data") {
                val queue = MMQueue(baseConfig)
                repeat(filesize) {
                    queue.enqueue(tracingContext, "AN".toByteArray())
                }
                when (queue.dequeue(tracingContext)) {
                    is EnqDeqResult.Companion.Success -> success()
                    is EnqDeqResult.Companion.Failure -> fail("expected a failure here")
                }
            }
            then("successfully retrieve data till queue is empty") {
                val queue = MMQueue(baseConfig)
                repeat(6) {
                    queue.enqueue(tracingContext, "AN".toByteArray())
                }
                repeat(6) {
                    when (val result = queue.dequeue(tracingContext)) {
                        is EnqDeqResult.Companion.Success -> {
                            String(result.data) shouldBe  "AN"
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a failure here")
                    }
                }
            }
        }
    }
})



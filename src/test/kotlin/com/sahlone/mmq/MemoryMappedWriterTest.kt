package com.sahlone.mmq

import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.models.EnqDeqResultType
import com.sahlone.mmq.queue.MMQueue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.fp.success
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.fail
import java.io.File
import java.util.UUID

class MemoryMappedWriterTest : BehaviorSpec({

    val filesize = 200
    val baseConfig = AppConfig("perftest/", filesize, "perftest-${UUID.randomUUID()}")
    val tracingContext = TracingContext()
    afterEach { _ ->
        File(baseConfig.baseQueueDir).deleteRecursively()
    }
    given("a memory mapped queue") {
        `when`("the queue is empty") {
            then("successfully insert data") {
                val queue = MMQueue(baseConfig)
                when (val result = queue.enqueue(tracingContext, "message bytes".toByteArray())) {
                    is EnqDeqResult.Companion.Success -> {
                        result.resultType shouldBe EnqDeqResultType.ENQUEUE
                        String(result.data) shouldBe "message bytes"
                    }
                    is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                }
            }
            then("successfully keep on inserting data till queue is full") {
                val queue = MMQueue(baseConfig)
                val message = "message bytes"
                val noOfOperationsAllowed = filesize / (message.length + (message.length * 4))
                repeat(noOfOperationsAllowed) {
                    when (val result = queue.enqueue(tracingContext, "message bytes".toByteArray())) {
                        is EnqDeqResult.Companion.Success -> {
                            result.resultType shouldBe EnqDeqResultType.ENQUEUE
                            String(result.data) shouldBe "message bytes"
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                    }
                }
            }
        }
        `when`("the queue is full") {
            then("fail to insert data") {
                val queue = MMQueue(baseConfig)
                repeat(filesize) {
                    queue.enqueue(tracingContext, "AN".toByteArray())
                }
                when (queue.enqueue(tracingContext, "message bytes".toByteArray())) {
                    is EnqDeqResult.Companion.Success -> fail("expected a failure for enqueue")
                    is EnqDeqResult.Companion.Failure -> success()
                }
            }

            /*
              if the message size + metadata size is a multiple of queue size, the last element should be inserted
              For eg
              queue size = 100 bytes
              message size = 1 byte
              total message size = 5bytes including 4 bytes metadata
              total allowed messages should be 200/5 -1 = 39
             */
            then("successfully insert data that is a multiple of queue size") {
                val message = "A"
                val queue = MMQueue(baseConfig)
                val noOfOperationsAllowed = filesize / (message.length + (message.length * 4)) - 1
                repeat(noOfOperationsAllowed) {
                    when (val result = queue.enqueue(tracingContext, message.toByteArray())) {
                        is EnqDeqResult.Companion.Success -> {
                            result.resultType shouldBe EnqDeqResultType.ENQUEUE
                            String(result.data) shouldBe message
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                    }
                }
                when (queue.enqueue(tracingContext, "message bytes".toByteArray())) {
                    is EnqDeqResult.Companion.Success -> fail("")
                    is EnqDeqResult.Companion.Failure -> success()
                }
            }
        }
    }
})


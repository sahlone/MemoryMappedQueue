package com.sahlone.mmq

import com.sahlone.mmq.config.AppConfig
import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.models.EnqDeqResult
import com.sahlone.mmq.models.EnqDeqResultType
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.fail
import java.io.File
import java.util.UUID

class MemoryMappedReaderWriterTest : BehaviorSpec({

    val filesize = 20000000
    val baseConfig = AppConfig("perftest/", filesize, "perftest-${UUID.randomUUID()}")
    val tracingContext = TracingContext()
    afterEach { _ ->
        File(baseConfig.baseQueueDir).deleteRecursively()
    }
    given("a memory mapped queue") {
        `when`("the queue is empty") {
            then("insert no fo messages and give them back in order") {
                val queue = MMQueue(baseConfig)
                val messages = (0 until 10000).map {
                    String("message ${UUID.randomUUID().toString()}".toByteArray())
                }
                messages.map {
                    when (val result = queue.enqueue(tracingContext, it.toByteArray())) {
                        is EnqDeqResult.Companion.Success -> {
                            result.resultType shouldBe EnqDeqResultType.ENQUEUE
                            String(result.data) shouldBe it
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                    }
                }
                messages.fold(listOf<String>()) { acc, elem ->
                    when (val result = queue.dequeue(tracingContext)) {
                        is EnqDeqResult.Companion.Success -> {
                            result.resultType shouldBe EnqDeqResultType.DEQUEUE
                            String(result.data) shouldBe elem
                            acc + String(result.data)
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                    }
                }
            }
        }

    }
    given("a concurrent memory mapped queue"){
        `when`("multiple threads are interacting") {
            then("insert no fo messages and give them back in order") {
                val queue = MMQueue(baseConfig)
                val messages = (0 until 10000).map {
                    String("message ${UUID.randomUUID().toString()}".toByteArray())
                }
                val partitions = messages.mapIndexed {index, elem ->
                    index to elem
                }.partition {
                    it.first % 2 == 0
                }
                val firstSet = partitions.first.map {
                    it.second
                }
                val secondSet = partitions.second.map {
                    it.second
                }
                runBlocking {
                    launch {
                        firstSet.map {
                            when (val result = queue.enqueue(tracingContext, it.toByteArray())) {
                                is EnqDeqResult.Companion.Success -> {
                                    result.resultType shouldBe EnqDeqResultType.ENQUEUE
                                    String(result.data) shouldBe it
                                }
                                is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                            }
                        }
                    }
                    launch {
                        secondSet.map {
                            when (val result = queue.enqueue(tracingContext, it.toByteArray())) {
                                is EnqDeqResult.Companion.Success -> {
                                    result.resultType shouldBe EnqDeqResultType.ENQUEUE
                                    String(result.data) shouldBe it
                                }
                                is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                            }
                        }
                    }
                }
                val allMessages = messages.fold(listOf<String>()) { acc, _ ->
                    when (val result = queue.dequeue(tracingContext)) {
                        is EnqDeqResult.Companion.Success -> {
                            acc + String(result.data)
                        }
                        is EnqDeqResult.Companion.Failure -> fail("expected a success for enqueue")
                    }
                }.toSet()
                messages.forEach{
                    allMessages.contains(it) shouldBe true
                }
                allMessages.size shouldBe messages.size
            }
        }
    }
})



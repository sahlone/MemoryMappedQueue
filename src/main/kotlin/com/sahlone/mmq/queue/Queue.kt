package com.sahlone.mmq.queue

import com.sahlone.mmq.logging.TracingContext
import com.sahlone.mmq.models.EnqDeqResult

interface Queue {
    fun enqueue(tracingContext: TracingContext, data: ByteArray): EnqDeqResult
    fun dequeue(tracingContext: TracingContext): EnqDeqResult
}

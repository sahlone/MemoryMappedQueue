package com.sahlone.mmq.perftest.utils

fun perfTestResult(start: Long, stop: Long, operation: String, noOfOperations: Long) {
    println("======$operation Perf test Results=====")
    println("Total no of Operation: " + (stop - start) / 1000000 + " ms")
    println("Total time: " + (stop - start) / 1000000 + " ms")
    println("Latency per op: ${(stop - start) / noOfOperations} ns")
    println("Operation/s: " + (noOfOperations / ((stop - start) / 1000000000.toFloat())).toLong())
    println("========================================")
}

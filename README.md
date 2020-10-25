# Memory Mapped Queue

[![](https://jitpack.io/v/sahlone/MemoryMappedQueue.svg?label=Release)](https://jitpack.io/#sahlone/kson)

Memory mapped queue is a high performance queue based on the concept of [Memory Mapped Files](https://en.wikipedia.org/wiki/Memory-mapped_file).

The Queue is a circular queue which can in theory give unlimited number of operation provided there is space left under certain conditions.
The queue can store the data across system restricts as it is based on the concept of file per queue.
The application can be started as targeting only one queue at a time, so if we need to have multiple queues we need to have multiple application instances running.
### Perf Test results
Configuration used for perf tests. 
Benchmarks are yet to be published for better average results.
```
Machine : Macos Catalina
Processor: 4Core i7 2.8GHZ
Ram: 16GB
```

```
======Perf test Results=====
Total no of Operation: 8132 ms
Total time: 8132 ms
Latency per op: 813 ns
Operation/s: 1229575
========================================
```
Application takes a set of configuration as a startup environment variables that can help to tweak the application.
The application also comes with predefined set of default values  so that the application can be started right away.

```
QUEUE_BASE_DIR  [default: queue/data]  -> Directory within host application where queue data can be written
QUEUE_SIZE_BYTES -> [default:20KB] Maximum size of queue in bytes( limited by the operating system and handware for 32bit vs 64 bit)
QUEUE_NAME ->[default: memory-mapped-queue] name of the queue
```
*Note* Once the Queue have been create the size cannot be changed within application but requires some maintenance work

### Commands
```
PUT text
    Inserts a text line into the FIFO Queue. text is a single line of an arbitrary
    number of case sensitive alphanumeric words ([A-Z]+[a-z]+[0-9]) separated by
    space characters.
GET n
    Return n lines from the head of the queue and remove them from the queue.
SHUTDOWN
    Shutdown the application/server.
```
### Installation
The application can be used as a standalone library  that can be grabbed from [Jitpack](https://jitpack.io/#sahlone/kson)
The library can be obtained from [Jitpack](https://jitpack.io/#sahlone/kson).
```Gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.sahlone:MemoryMappedQueue:$version'
}
```
### Building the Application
The application can be build using gradle build tool to produce an executable/runnable jar that can be located under `build/libs` directory.
To just build the application without running it,the included `build.sh` script can be invoked.
```
$ sh build.sh
```
### Running the Application
The application can also be run directly as an application
The included shell script `run.sh` will build the application and run the application in current working directory
```
$ sh run.sh
```
The above command will create executable jar file in builds folder that can be used to run the application
Remember to have environment variables if you need to change the application properties
### Work in progress
1. Implement Compare and Swap on head and tail for better concurrency control
2. Implement a Application server for multiple client connections


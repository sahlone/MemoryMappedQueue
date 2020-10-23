# Memory Mapped Queue

[![](https://jitpack.io/v/sahlone/MemoryMappedQueue.svg?label=Release)](https://jitpack.io/#sahlone/kson)

Memory mapped queue is a high performance queue based on the concept of [Memory Mapped Files](https://en.wikipedia.org/wiki/Memory-mapped_file).

The Queue is a circular queue which can in theory give unlimited no of operation provided there is space left under cetain conditions.
The queue can store the data across system restrts as it is based on concept of file per queue.
The application can be started as targeting only one queue at a time, so if we need to have multiple queues we need to have multiple application instances running.

Application takes a set of configuration as a strtup environment varibales that can help to tweak the application.
The application also comes with predeined set of default values  so that the application can be started right away.

```
QUEUE_BASE_DIR  [default: queue/data]  -> Directory within host application where queue data can be written
QUEUE_SIZE_BYTES -> [default:20KB] Maximum size of queue in bytes( limited by the operating system and handware for 32bit vs 64 bit)
QUEUE_NAME ->[default: memory-mapped-queue] name of the queue
```
*Note* Once the Queue have been create the size cannot be changed within application but requires some maintainence work

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
### Running the Application
The application can also be run directly as an application
Build the application to produce runnable jar
```
$ ./gradlew clean installShadowDist
```
The above command will create executable jar file in builds folder that can be used to run the application
Remember to have environment variables if you need to change the application properties
```
$ java -jar  -Dlogback.configurationFile=logback.xml build/libs/MemoryMappedQueue-1.0.1-all.jar
```
### Work in progress
1. Implement Compare and Swap on head and tail for better concurrecy control
2. Implement a Application server for multiple client connections


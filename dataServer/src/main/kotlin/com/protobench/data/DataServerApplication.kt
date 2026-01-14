package com.protobench.data

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DataServerApplication

fun main(args: Array<String>) {
    runApplication<DataServerApplication>(*args)
}
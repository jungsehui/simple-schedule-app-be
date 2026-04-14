package com.geekchat.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
class GeekChatServerApplication

fun main(args: Array<String>) {
    runApplication<GeekChatServerApplication>(*args)
}

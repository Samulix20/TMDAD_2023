package com.example.websockets.config

import com.rabbitmq.client.ConnectionFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("custom.rabbitmq")
data class RabbitMqConfig (
    var username : String = "",
    var password : String = "",
    var virtualHost : String = "",
    var host : String = "",
    var port : String = ""
) {
    fun connectionFactory() : ConnectionFactory {
        val cf = ConnectionFactory()
        cf.username = username
        cf.password = password
        cf.virtualHost = virtualHost
        cf.host = host
        cf.port = port.toInt()
        return cf
    }
}
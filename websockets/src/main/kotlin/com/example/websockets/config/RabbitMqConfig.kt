package com.example.websockets.config

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.http.client.Client
import com.rabbitmq.http.client.ClientParameters
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("custom.rabbitmq")
data class RabbitMqConfig (
    var username : String = "",
    var password : String = "",
    var virtualHost : String = "",
    var host : String = "",
    var port : String = "",
    var managementPort : String = ""
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

    fun httpClient() : Client {
        return Client(
            ClientParameters()
                .url("http://$host:$managementPort/api/")
                .username(username)
                .password(password)
        )
    }
}
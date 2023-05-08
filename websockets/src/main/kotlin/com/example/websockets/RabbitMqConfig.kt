package com.example.websockets

import com.example.websockets.dto.ChatMessage
import com.nimbusds.jose.shaded.gson.Gson
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Service
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

@Service
class RabbitMqService (
    config: RabbitMqConfig,
    val socketMessageSender : SimpMessageSendingOperations
) {
    val conn = config.connectionFactory().newConnection()
    val channel = conn.createChannel()
    val gson = Gson()

    init {
        // Declare direct message exchange
        channel.exchangeDeclare("directMsg", "direct", true)
        channel.exchangeDeclare("adminMsg", "fanout", true)
    }
    fun startUserSession(username : String) {
        channel.queueDeclare(username, true, false, false, null)
        // Use username as routing key
        channel.queueBind(username, "directMsg", username)
        channel.queueBind(username, "adminMsg", username)
        // Use username as consumer
        channel.basicConsume(username, true, username,
            object : DefaultConsumer(channel) {
                override fun handleDelivery(
                    consumerTag: String,
                    envelope: Envelope,
                    properties: AMQP.BasicProperties,
                    body: ByteArray
                ) {
                    val msg = gson.fromJson(String(body), ChatMessage::class.java)
                    // Send message back to user
                    socketMessageSender.convertAndSend(
                        "/topic/chat/${msg.receiver}",
                        msg
                    )
                }
            })
    }

    fun publish(msg: ChatMessage, exchange: String = "directMsg") {
        channel.basicPublish(exchange, msg.receiver, null, gson.toJson(msg).toByteArray())
    }

}
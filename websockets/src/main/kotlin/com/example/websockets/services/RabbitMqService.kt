package com.example.websockets.services

import com.example.websockets.config.RabbitMqConfig
import com.example.websockets.dto.ChatMessage
import com.nimbusds.jose.shaded.gson.Gson
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Service
class RabbitMqService (
    val config: RabbitMqConfig,
    val socketMessageSender : SimpMessageSendingOperations
) {
    val gson = Gson()

    private final fun <T> retry(call: () -> T): T {
        while (true) {
            try {
                return call()
            } catch (e: Exception) {
                Thread.sleep(1000)
                continue
            }
        }
    }

    val conn: Connection = retry { config.connectionFactory().newConnection() }
    final val channel = retry { conn.createChannel() }!!
    final val httpClient = retry { config.httpClient() }

    // sessionID -> consumerID
    val sessionCache : HashMap<String, String> = HashMap()

    init {
        // Declare direct message exchange
        channel.exchangeDeclare("directMsg", "direct", true)
        channel.exchangeDeclare("adminMsg", "fanout", true)
    }

    fun createUserQueue(username: String) {
        channel.queueDeclare(username, true, false, false, null)
        // Use username as routing key
        channel.queueBind(username, "directMsg", username)
        channel.queueBind(username, "adminMsg", username)
    }

    fun createGroupExchange(groupName: String, username: String) {
        channel.exchangeDeclare("groups.$groupName", "fanout", true)
        bindUserToGroup(groupName, username)
    }

    fun deleteGroupExchange(groupName: String) {
        channel.exchangeDelete("groups.$groupName")
    }

    fun bindUserToGroup(groupName: String, username: String) {
        channel.queueBind(username, "groups.$groupName", username)
    }

    fun unbindUserFromGroup(groupName: String, username: String) {
        channel.queueUnbind(username, "groups.$groupName", username)
    }

    fun startUserSession(username : String, sessionID : String) {
        // Use username as consumer
        val consumerID = channel.basicConsume(username, true,
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
                        "/topic/chat/$username",
                        msg
                    )
                }
            })
        // Store session
        sessionCache[sessionID] = consumerID
    }

    fun endSession(sessionID: String) {
        val consumerID = sessionCache[sessionID]
        if (consumerID != null) {
            sessionCache.remove(sessionID)
            channel.basicCancel(consumerID)
        }
    }

    fun publish(msg: ChatMessage, exchange: String = "directMsg") {
        // Server echo for direct messages to other users
        if (exchange == "directMsg" && msg.receiver != msg.sender) {
            channel.basicPublish(exchange, msg.sender, null, gson.toJson(msg).toByteArray())
        }
        channel.basicPublish(exchange, msg.receiver, null, gson.toJson(msg).toByteArray())
    }

    fun getQueueBindings(username: String) : List<String> {
        val r = mutableListOf<String>()
        httpClient.getQueueBindings(config.virtualHost, username).forEach {
            if (it.source != "directMsg" && it.source != "adminMsg" && it.source != "") {
                r.add(it.source.removePrefix("groups."))
            }
        }
        return r
    }
}

@Component
class WebSocketEventListener (
    val rabbitService: RabbitMqService
) {
    @EventListener
    fun handleSessionDisconnect(event : SessionDisconnectEvent) {
        rabbitService.endSession(event.sessionId)
    }
}

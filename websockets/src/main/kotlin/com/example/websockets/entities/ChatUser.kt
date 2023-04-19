package com.example.websockets.entities

import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["username"])])
class ChatUser(
    val username: String = "",
    val password: String = "",
    val role: String = "USER",
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = -1
)

@Repository
interface ChatUserRepository : CrudRepository<ChatUser, Long> {
    fun findByUsername(name: String?): ChatUser?
}


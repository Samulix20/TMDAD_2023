package com.example.websockets.entities

import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["username"])]
)
class ChatUser(
    @Column(nullable = false)
    val username: String = "",
    @Column(nullable = false)
    val password: String = "",
    @Column(nullable = false)
    val role: String = "USER",
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = -1
)

@Repository
interface ChatUserRepository : CrudRepository<ChatUser, Long> {
    fun findByUsername(name: String?): ChatUser?
}


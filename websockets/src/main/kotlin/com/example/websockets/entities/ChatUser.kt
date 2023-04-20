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
    val id: Long = -1,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "is_member",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "group_id")]
    )
    var groups: MutableSet<ChatGroup> = mutableSetOf(),
)

@Repository
interface ChatUserRepository : CrudRepository<ChatUser, Long> {
    fun findByUsername(name: String?): ChatUser?
}


package com.example.websockets.entities

import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "groups",
    uniqueConstraints = [UniqueConstraint(columnNames = ["name"])]
)
class ChatGroup (
    @Column(nullable = false)
    val name: String = "",
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = -1,
    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    var admin: ChatUser = ChatUser(),
    @OneToMany(
        mappedBy = "group",
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var messages: MutableList<GroupMessage> = mutableListOf<GroupMessage>()

)
@Repository
interface ChatGroupRepository : CrudRepository<ChatGroup, Long> {
    fun findByName(name: String?): ChatGroup?
    fun findByAdmin(admin: ChatUser): List<ChatGroup>
}


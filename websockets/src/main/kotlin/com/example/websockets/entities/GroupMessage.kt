package com.example.websockets.entities

import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Entity
@Table(
    name = "messages"
)
class GroupMessage (
    val content : String = "",
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = -1,
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    var group: ChatGroup = ChatGroup()
)

@Repository
interface GroupMessageRepository : CrudRepository<GroupMessage, Long>

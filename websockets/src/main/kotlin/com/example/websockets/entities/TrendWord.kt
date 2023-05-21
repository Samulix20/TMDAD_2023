package com.example.websockets.entities

import jakarta.persistence.*
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Entity
@Table(
    name = "trend_words"
)
class TrendWord (
    val word: String = "",
    val timestamp: Timestamp = Timestamp.from(Instant.now()),
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = -1
)

@Repository
interface TrendWordRepository : CrudRepository<TrendWord, Long> {
    @Query("SELECT w from TrendWord w where w.timestamp > ?1")
    fun findLastHourWords(ts: Timestamp) : List<TrendWord>
}
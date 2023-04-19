package com.example.websockets.controllers

import com.example.websockets.services.HashService
import com.example.websockets.services.TokenService
import com.example.websockets.dto.TokenResponse
import com.example.websockets.dto.UserLoginInfo
import com.example.websockets.entities.ChatUser
import com.example.websockets.entities.ChatUserRepository
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/users")
class LoginController (
    val db : ChatUserRepository,
    val tokenService: TokenService,
    val hashService: HashService
) {
    @PostMapping("/register")
    fun register(@RequestBody payload: UserLoginInfo): TokenResponse {
        try {
            val savedUser = db.save(ChatUser(payload.username, hashService.hashBcrypt(payload.password)))
            return TokenResponse(tokenService.createToken(savedUser))
        } catch (e: Exception) {
            throw ResponseStatusException(400, "Register failed", null)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody payload: UserLoginInfo): TokenResponse {
        val user = db.findByUsername(payload.username) ?: throw ResponseStatusException(400, "Login Failed", null)
        if(!hashService.checkBcrypt(payload.password, user.password)) {
            throw ResponseStatusException(400, "Login Failed", null)
        }
        return TokenResponse(tokenService.createToken(user))
    }

    @GetMapping("/secured")
    @ResponseBody
    fun secured() : String {
        return "SECURED"
    }
}
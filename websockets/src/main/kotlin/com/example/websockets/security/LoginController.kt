package com.example.websockets.security

import com.example.websockets.users.ChatUser
import com.example.websockets.users.ChatUserRepository
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@CrossOrigin
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
}

data class UserLoginInfo (
    val username: String,
    val password: String
)

data class TokenResponse (
    val token : String
)
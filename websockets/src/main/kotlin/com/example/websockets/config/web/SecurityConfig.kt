package com.example.websockets.config.web

import com.example.websockets.services.TokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig (
    val tokenService: TokenService
) {
    @Bean
    fun filterChain(http: HttpSecurity) : SecurityFilterChain {
        return with(http){
            cors()
            authorizeHttpRequests()
                .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/csrf").permitAll()
                .requestMatchers("/websocket/**").permitAll()
                .anyRequest().authenticated()
            oauth2ResourceServer().jwt()
            authenticationManager { auth ->
                val jwt = auth as BearerTokenAuthenticationToken
                val user = tokenService.userFromToken(jwt.token) ?: throw InvalidBearerTokenException("Invalid token")
                UsernamePasswordAuthenticationToken(user, "", listOf(SimpleGrantedAuthority("USER")))
            }
            sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            csrf().disable()
            headers().frameOptions().disable()
            headers().xssProtection().disable()
            build()
        }
    }
}

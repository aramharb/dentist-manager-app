package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.example.demo.security.AuthenticationException;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.security.JwtTokenService;
import com.example.demo.service.UserPresenceService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtTokenService tokenService;
    private final ThreadPoolTaskScheduler heartbeatScheduler;
    private final String[] allowedOrigins;

    public WebSocketConfig(JwtTokenService tokenService,
            @Qualifier("presenceHeartbeatScheduler") ThreadPoolTaskScheduler heartbeatScheduler,
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        this.tokenService = tokenService;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic")
                .setTaskScheduler(heartbeatScheduler)
                .setHeartbeatValue(new long[] {10000, 10000});
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public static ThreadPoolTaskScheduler presenceHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("session-heartbeat-");
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins);
    }

    private static boolean isOwnPresenceTopic(StompHeaderAccessor accessor) {
        return accessor.getUser() instanceof ClinicPrincipal principal && principal.cabinetId() != null
                && UserPresenceService.presenceTopic(principal.cabinetId()).equals(accessor.getDestination());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new AuthenticationException("Authentication token is required.");
                    }
                    ClinicPrincipal connecting = tokenService.verify(authorization.substring(7).trim());
                    if (connecting.hasRole(ClinicPrincipal.CLIENT_ROLE)) {
                        throw new AuthenticationException("Client accounts cannot open a staff connection.");
                    }
                    accessor.setUser(connecting);
                }
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        && !"/user/queue/messages".equals(accessor.getDestination())
                        && !"/user/queue/activity".equals(accessor.getDestination())
                        && !"/user/queue/staff-actions".equals(accessor.getDestination())
                        && !"/user/queue/schedule".equals(accessor.getDestination())
                        && !"/user/queue/treatments".equals(accessor.getDestination())
                        && !isOwnPresenceTopic(accessor)) {
                    throw new AuthenticationException("Subscription is not allowed.");
                }
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        && ("/user/queue/activity".equals(accessor.getDestination())
                                || "/user/queue/staff-actions".equals(accessor.getDestination()))
                        && (!(accessor.getUser() instanceof ClinicPrincipal principal)
                                || !principal.hasRole("doctor"))) {
                    throw new AuthenticationException("Only a doctor can subscribe to staff actions.");
                }
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                        && ("/user/queue/schedule".equals(accessor.getDestination())
                                || "/user/queue/treatments".equals(accessor.getDestination()))
                        && (!(accessor.getUser() instanceof ClinicPrincipal principal)
                                || (!principal.hasRole("doctor") && !principal.hasRole("secretaire")))) {
                    throw new AuthenticationException("Only clinic staff can subscribe to schedule updates.");
                }
                if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new AuthenticationException("Sending over this WebSocket is not allowed.");
                }
                return message;
            }
        });
    }
}

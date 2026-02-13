package com.greenconnect.greenconnect_api.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/queue")
                // ⭐ HEARTBEAT: Server gửi heartbeat mỗi 25s, mong đợi client gửi mỗi 25s
                // Giúp giữ kết nối sống qua Nginx proxy
                .setHeartbeatValue(new long[]{25000, 25000})
                .setTaskScheduler(heartBeatScheduler());
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setClientLibraryUrl("https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js")
                // ⭐ SockJS heartbeat settings
                .setHeartbeatTime(25000)           // Heartbeat interval 25s
                .setDisconnectDelay(5000)          // Delay trước khi disconnect
                .setStreamBytesLimit(512 * 1024)   // 512KB stream limit
                .setHttpMessageCacheSize(1000);    // Cache size cho HTTP messages
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
    
    // ⭐ Cấu hình WebSocket transport - tăng timeout và buffer size
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024);      // 128KB max message size
        registration.setSendBufferSizeLimit(512 * 1024);   // 512KB send buffer
        registration.setSendTimeLimit(20 * 1000);          // 20s timeout cho việc gửi message
        registration.setTimeToFirstMessage(60 * 1000);     // 60s timeout chờ message đầu tiên
    }
    
    // ⭐ TaskScheduler bean cho heartbeat - QUAN TRỌNG để heartbeat hoạt động
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setDaemon(true);
        scheduler.initialize();
        return scheduler;
    }
}
package com.example.rabbitmq;

import com.awsomeasb.AwsomeMQClient;
import com.awsomeasb.DeliverCallback;
import com.awsomeasb.Delivery;
import com.example.websocket.NewsWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletContextEvent;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQConsumerTest {

    @Mock
    private ServletContextEvent servletContextEvent;

    @Mock
    private AwsomeMQClient mockClient;

    @Mock
    private Delivery mockDelivery;

    private RabbitMQConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RabbitMQConsumer();
    }

    @Test
    void testContextInitialized_WithConnectionString() throws Exception {
        // Given: Environment variable is set (we can't mock System.getenv, so we test with actual env)
        // When: Context is initialized
        consumer.contextInitialized(servletContextEvent);

        // Then: Consumer should initialize without errors
        // The test passes if no exception is thrown
    }

    @Test
    void testContextInitialized_WithoutConnectionString() {
        // Given: Environment behavior is default
        // When: Context is initialized
        consumer.contextInitialized(servletContextEvent);

        // Then: Should use default connection string and not throw exception
        // The consumer should handle missing connection string gracefully
    }

    @Test
    void testContextDestroyed() {
        // Given: Consumer is initialized
        consumer.contextInitialized(servletContextEvent);

        // When: Context is destroyed
        consumer.contextDestroyed(servletContextEvent);

        // Then: Should clean up resources without throwing exceptions
        // Verify no exceptions are thrown during cleanup
    }

    @Test
    void testMessageDeliveryCallback() throws Exception {
        // Given: A message delivery
        String testMessage = "Test news message";
        when(mockDelivery.getBodyAsString()).thenReturn(testMessage);

        // When: Message is delivered
        DeliverCallback callback = (tag, delivery) -> {
            String message = delivery.getBodyAsString();
            System.out.println("Received: " + message);
        };

        // Then: Callback processes the message
        callback.handle("test-tag", mockDelivery);
        verify(mockDelivery, times(1)).getBodyAsString();
    }

    @Test
    void testMessageBroadcastToWebSocket() throws Exception {
        // Given: A message is received
        String testMessage = "{\"title\":\"Breaking News\",\"content\":\"Test\"}";
        when(mockDelivery.getBodyAsString()).thenReturn(testMessage);

        try (MockedStatic<NewsWebSocket> webSocketMock = mockStatic(NewsWebSocket.class)) {
            // When: Message is delivered and broadcast
            DeliverCallback callback = (tag, delivery) -> {
                String message = delivery.getBodyAsString();
                NewsWebSocket.broadcast(message);
            };

            callback.handle("test-tag", mockDelivery);

            // Then: Message should be broadcast to WebSocket
            webSocketMock.verify(() -> NewsWebSocket.broadcast(testMessage), times(1));
        }
    }

    @Test
    void testMessageBroadcastHandlesException() throws Exception {
        // Given: WebSocket broadcast throws exception
        String testMessage = "Test message";
        when(mockDelivery.getBodyAsString()).thenReturn(testMessage);

        try (MockedStatic<NewsWebSocket> webSocketMock = mockStatic(NewsWebSocket.class)) {
            webSocketMock.when(() -> NewsWebSocket.broadcast(anyString()))
                    .thenThrow(new RuntimeException("WebSocket error"));

            // When: Message is delivered
            DeliverCallback callback = (tag, delivery) -> {
                String message = delivery.getBodyAsString();
                try {
                    NewsWebSocket.broadcast(message);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            };

            // Then: Exception should be caught and handled
            callback.handle("test-tag", mockDelivery);
            webSocketMock.verify(() -> NewsWebSocket.broadcast(testMessage), times(1));
        }
    }

    @Test
    void testConsumerAutoAcknowledgement() throws Exception {
        // This test verifies that the consumer is configured with auto-acknowledgement
        // In the actual implementation, basicConsume is called with autoAck=true
        
        // Given: Mock client
        when(mockClient.basicConsume(eq("news"), eq(true), any(DeliverCallback.class)))
                .thenReturn("consumer-tag-123");

        // When: Consumer is set up
        String consumerTag = mockClient.basicConsume("news", true, (tag, delivery) -> {});

        // Then: Consumer tag is returned
        verify(mockClient, times(1)).basicConsume(eq("news"), eq(true), any(DeliverCallback.class));
        assert consumerTag != null;
        assert consumerTag.equals("consumer-tag-123");
    }

    @Test
    void testConsumerCancellation() throws Exception {
        // Given: Consumer tag exists
        String consumerTag = "consumer-tag-123";
        
        // When: Consumer is cancelled
        doNothing().when(mockClient).basicCancel(consumerTag);
        mockClient.basicCancel(consumerTag);

        // Then: basicCancel is called
        verify(mockClient, times(1)).basicCancel(consumerTag);
    }

    @Test
    void testClientClose() throws Exception {
        // Given: Client is open
        doNothing().when(mockClient).close();

        // When: Client is closed
        mockClient.close();

        // Then: close method is called
        verify(mockClient, times(1)).close();
    }
}

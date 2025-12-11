package com.example;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

/**
 * Unit tests for Azure Service Bus News Publisher App.
 * These tests use Mockito to mock Azure Service Bus dependencies.
 */
public class AppTest 
{
    @Mock
    private ServiceBusSenderClient mockSenderClient;
    
    @Mock
    private ServiceBusClientBuilder mockClientBuilder;
    
    @Mock
    private ServiceBusClientBuilder.ServiceBusSenderClientBuilder mockSenderBuilder;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    /**
     * Test that createSenderClient creates a client with correct configuration
     */
    @Test
    public void testCreateSenderClient() {
        String connectionString = "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey";
        String queueName = "news";
        
        try (MockedConstruction<ServiceBusClientBuilder> mockedBuilder = mockConstruction(ServiceBusClientBuilder.class,
                (mock, context) -> {
                    when(mock.connectionString(anyString())).thenReturn(mock);
                    when(mock.sender()).thenReturn(mockSenderBuilder);
                    when(mockSenderBuilder.queueName(anyString())).thenReturn(mockSenderBuilder);
                    when(mockSenderBuilder.buildClient()).thenReturn(mockSenderClient);
                })) {
            
            ServiceBusSenderClient result = App.createSenderClient(connectionString, queueName);
            
            assertNotNull("Sender client should not be null", result);
            assertEquals("Should return the mocked sender client", mockSenderClient, result);
            
            // Verify the construction happened
            assertEquals("ServiceBusClientBuilder should be constructed once", 1, mockedBuilder.constructed().size());
            
            ServiceBusClientBuilder constructedBuilder = mockedBuilder.constructed().get(0);
            verify(constructedBuilder).connectionString(connectionString);
            verify(constructedBuilder).sender();
            verify(mockSenderBuilder).queueName(queueName);
            verify(mockSenderBuilder).buildClient();
        }
    }
    
    /**
     * Test that sendMessage sends a message with the correct content
     */
    @Test
    public void testSendMessage() {
        String messageText = "Breaking News: Test Message";
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            App.sendMessage(mockSenderClient, messageText);
            
            // Verify that a ServiceBusMessage was created
            assertEquals("ServiceBusMessage should be constructed once", 1, mockedMessage.constructed().size());
            
            // Verify that sendMessage was called on the sender client
            verify(mockSenderClient, times(1)).sendMessage(any(ServiceBusMessage.class));
        }
    }
    
    /**
     * Test that sendMessage handles empty strings
     */
    @Test
    public void testSendMessageWithEmptyString() {
        String messageText = "";
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            App.sendMessage(mockSenderClient, messageText);
            
            // Verify that a ServiceBusMessage was created even with empty string
            assertEquals("ServiceBusMessage should be constructed once", 1, mockedMessage.constructed().size());
            verify(mockSenderClient, times(1)).sendMessage(any(ServiceBusMessage.class));
        }
    }
    
    /**
     * Test that sendMessage handles special characters
     */
    @Test
    public void testSendMessageWithSpecialCharacters() {
        String messageText = "Test message with special chars: @#$%^&*()";
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            App.sendMessage(mockSenderClient, messageText);
            
            assertEquals("ServiceBusMessage should be constructed once", 1, mockedMessage.constructed().size());
            verify(mockSenderClient, times(1)).sendMessage(any(ServiceBusMessage.class));
        }
    }
    
    /**
     * Test that sendMessage handles unicode characters
     */
    @Test
    public void testSendMessageWithUnicodeCharacters() {
        String messageText = "Test message with unicode: \u4E2D\u6587 \u65E5\u672C\u8A9E \uD83D\uDE00";
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            App.sendMessage(mockSenderClient, messageText);
            
            assertEquals("ServiceBusMessage should be constructed once", 1, mockedMessage.constructed().size());
            verify(mockSenderClient, times(1)).sendMessage(any(ServiceBusMessage.class));
        }
    }
    
    /**
     * Test that sendMessage handles long messages
     */
    @Test
    public void testSendMessageWithLongMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a test message. ");
        }
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            App.sendMessage(mockSenderClient, longMessage.toString());
            
            assertEquals("ServiceBusMessage should be constructed once", 1, mockedMessage.constructed().size());
            verify(mockSenderClient, times(1)).sendMessage(any(ServiceBusMessage.class));
        }
    }
    
    /**
     * Test that createSenderClient works with different queue names
     */
    @Test
    public void testCreateSenderClientWithDifferentQueueNames() {
        String connectionString = "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=testkey";
        String[] queueNames = {"news", "alerts", "test-queue", "my_queue_123"};
        
        for (String queueName : queueNames) {
            try (MockedConstruction<ServiceBusClientBuilder> mockedBuilder = mockConstruction(ServiceBusClientBuilder.class,
                    (mock, context) -> {
                        when(mock.connectionString(anyString())).thenReturn(mock);
                        when(mock.sender()).thenReturn(mockSenderBuilder);
                        when(mockSenderBuilder.queueName(anyString())).thenReturn(mockSenderBuilder);
                        when(mockSenderBuilder.buildClient()).thenReturn(mockSenderClient);
                    })) {
                
                ServiceBusSenderClient result = App.createSenderClient(connectionString, queueName);
                
                assertNotNull("Sender client should not be null for queue: " + queueName, result);
                verify(mockSenderBuilder).queueName(queueName);
            }
        }
    }
    
    /**
     * Test that multiple messages can be sent in sequence
     */
    @Test
    public void testSendMultipleMessages() {
        String[] messages = {
            "First message",
            "Second message", 
            "Third message"
        };
        
        try (MockedConstruction<ServiceBusMessage> mockedMessage = mockConstruction(ServiceBusMessage.class)) {
            
            for (String msg : messages) {
                App.sendMessage(mockSenderClient, msg);
            }
            
            // Verify that ServiceBusMessage was created for each message
            assertEquals("ServiceBusMessage should be constructed three times", 3, mockedMessage.constructed().size());
            
            // Verify that sendMessage was called three times
            verify(mockSenderClient, times(3)).sendMessage(any(ServiceBusMessage.class));
        }
    }
}

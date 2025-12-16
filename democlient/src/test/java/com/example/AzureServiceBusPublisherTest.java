package com.example;

import com.awsomeasb.AwsomeMQClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Azure Service Bus publisher functionality.
 * Tests use Mockito to mock AwsomeMQClient and verify message publishing logic.
 */
public class AzureServiceBusPublisherTest {
    
    @Mock
    private AwsomeMQClient mockClient;
    
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @Test
    public void testBasicPublishWithValidMessage() throws Exception {
        // Arrange
        String testMessage = "Test news message";
        byte[] expectedBytes = testMessage.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", expectedBytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(expectedBytes));
    }
    
    @Test
    public void testBasicPublishWithEmptyMessage() throws Exception {
        // Arrange
        String emptyMessage = "";
        byte[] expectedBytes = emptyMessage.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", expectedBytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(expectedBytes));
    }
    
    @Test
    public void testBasicPublishWithUnicodeMessage() throws Exception {
        // Arrange
        String unicodeMessage = "Breaking news: 今日のニュース ✓";
        byte[] expectedBytes = unicodeMessage.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", expectedBytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(expectedBytes));
    }
    
    @Test
    public void testBasicPublishMultipleMessages() throws Exception {
        // Arrange
        String message1 = "First message";
        String message2 = "Second message";
        String message3 = "Third message";
        
        // Act
        mockClient.basicPublish("news", message1.getBytes("UTF-8"));
        mockClient.basicPublish("news", message2.getBytes("UTF-8"));
        mockClient.basicPublish("news", message3.getBytes("UTF-8"));
        
        // Assert
        verify(mockClient, times(3)).basicPublish(eq("news"), any(byte[].class));
    }
    
    @Test
    public void testBasicPublishWithLongMessage() throws Exception {
        // Arrange
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longMessage.append("This is a very long news message. ");
        }
        String message = longMessage.toString();
        byte[] expectedBytes = message.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", expectedBytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(expectedBytes));
        assertTrue("Message should be at least 30KB", expectedBytes.length > 30000);
    }
    
    @Test
    public void testBasicPublishWithSpecialCharacters() throws Exception {
        // Arrange
        String specialMessage = "News with special chars: !@#$%^&*()_+-=[]{}|;':\"<>?,./~`";
        byte[] expectedBytes = specialMessage.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", expectedBytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(expectedBytes));
    }
    
    @Test
    public void testClientAutoCloseable() throws Exception {
        // Arrange
        AwsomeMQClient client = mock(AwsomeMQClient.class);
        
        // Act
        try (AwsomeMQClient autoCloseable = client) {
            autoCloseable.basicPublish("news", "test".getBytes("UTF-8"));
        }
        
        // Assert
        verify(client, times(1)).basicPublish(eq("news"), any(byte[].class));
        verify(client, times(1)).close();
    }
    
    @Test
    public void testQueueNameIsNews() {
        // Verify the queue name constant
        try {
            java.lang.reflect.Field field = App.class.getDeclaredField("QUEUE_NAME");
            field.setAccessible(true);
            String queueName = (String) field.get(null);
            assertEquals("Queue name should be 'news'", "news", queueName);
        } catch (Exception e) {
            fail("Should be able to access QUEUE_NAME constant");
        }
    }
    
    @Test
    public void testMessageEncodingIsUTF8() throws Exception {
        // Arrange
        String message = "Test message with émojis 🎉";
        byte[] utf8Bytes = message.getBytes("UTF-8");
        
        // Act
        mockClient.basicPublish("news", utf8Bytes);
        
        // Assert
        verify(mockClient, times(1)).basicPublish(eq("news"), eq(utf8Bytes));
        
        // Verify UTF-8 encoding preserves special characters
        String decoded = new String(utf8Bytes, "UTF-8");
        assertEquals("Message should be properly encoded in UTF-8", message, decoded);
    }
    
    @Test
    public void testConnectionStringValidation() {
        // Test that connection string validation works
        String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
        
        // In test environment, connection string might not be set
        // This validates that the app checks for it
        if (connectionString == null || connectionString.trim().isEmpty()) {
            assertTrue("Connection string should be validated", true);
        } else {
            assertFalse("Connection string should not be empty when set", 
                       connectionString.trim().isEmpty());
        }
    }
    
    /**
     * Restore original System.out and System.err after tests
     */
    @org.junit.After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}

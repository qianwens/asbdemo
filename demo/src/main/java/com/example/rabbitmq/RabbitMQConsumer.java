package com.example.rabbitmq;

import com.awsomeasb.AwsomeMQClient;
import com.awsomeasb.DeliverCallback;
import com.example.websocket.NewsWebSocket;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class RabbitMQConsumer implements ServletContextListener {
    
    private static final String QUEUE_NAME = "news";
    private AwsomeMQClient client;
    private String consumerTag;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================");
        System.out.println("RabbitMQConsumer: Starting initialization...");
        System.out.println("========================================");
        try {
            // Get Azure Service Bus connection string from environment
            String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
            
            if (connectionString == null || connectionString.isEmpty()) {
                System.err.println("WARNING: AZURE_SERVICEBUS_CONNECTION_STRING not set. Using default for testing.");
                connectionString = "Endpoint=sb://localhost.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=test";
            }
            
            System.out.println("Connecting to Azure Service Bus...");
            client = new AwsomeMQClient(connectionString, QUEUE_NAME);
            System.out.println("✓ Connected to Azure Service Bus successfully!");
            
            System.out.println("✓ Queue '" + QUEUE_NAME + "' configured");
            
            System.out.println("✓ Waiting for messages from Azure Service Bus queue: " + QUEUE_NAME);
            System.out.println("========================================");
            
            // Set up consumer
            DeliverCallback deliverCallback = (tag, delivery) -> {
                String message = delivery.getBodyAsString();
                System.out.println("========================================");
                System.out.println(">>> Received from Azure Service Bus: " + message);
                
                // Broadcast to all connected WebSocket clients
                try {
                    NewsWebSocket.broadcast(message);
                    System.out.println(">>> Broadcasted to WebSocket clients");
                } catch (Exception e) {
                    System.err.println(">>> ERROR broadcasting to WebSocket: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("========================================");
            };
            
            consumerTag = client.basicConsume(QUEUE_NAME, true, deliverCallback);
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("ERROR: Failed to connect to Azure Service Bus!");
            System.err.println("========================================");
            e.printStackTrace();
            System.err.println("Error connecting to Azure Service Bus: " + e.getMessage());
            System.err.println("Make sure AZURE_SERVICEBUS_CONNECTION_STRING is set");
            System.err.println("========================================");
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (consumerTag != null && client != null) {
                client.basicCancel(consumerTag);
            }
            if (client != null) {
                client.close();
            }
            System.out.println("Azure Service Bus connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

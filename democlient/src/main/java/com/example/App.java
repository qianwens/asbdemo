package com.example;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Azure Service Bus News Publisher Client
 */
public class App 
{
    private static final String QUEUE_NAME = "news";
    
    public static void main( String[] args )
    {
        System.out.println("=== Azure Service Bus News Publisher ===");
        
        // Get connection string from environment variable
        String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
        
        if (connectionString == null || connectionString.isEmpty()) {
            System.err.println("Error: AZURE_SERVICEBUS_CONNECTION_STRING environment variable is not set.");
            System.err.println("Please set it with: export AZURE_SERVICEBUS_CONNECTION_STRING=\"<your-connection-string>\"");
            return;
        }
        
        System.out.println("Connecting to Azure Service Bus...");
        
        try {
            ServiceBusSenderClient sender = createSenderClient(connectionString, QUEUE_NAME);
            
            System.out.println("Connected successfully!");
            System.out.println("Enter news messages (type 'exit' or 'quit' to stop):\n");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
            try {
                while (true) {
                    System.out.print("News > ");
                    String message = reader.readLine();
                    
                    if (message == null || message.trim().equalsIgnoreCase("exit") 
                        || message.trim().equalsIgnoreCase("quit")) {
                        System.out.println("\nExiting...");
                        break;
                    }
                    
                    if (message.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Send message to queue
                    sendMessage(sender, message);
                    System.out.println("✓ Published: " + message);
                }
            } finally {
                sender.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error connecting to Azure Service Bus: " + e.getMessage());
            System.err.println("\nMake sure your connection string is correct and the queue exists.");
            e.printStackTrace();
        }
    }
    
    /**
     * Create a Service Bus sender client.
     * Extracted as a separate method to facilitate testing.
     */
    static ServiceBusSenderClient createSenderClient(String connectionString, String queueName) {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueName)
            .buildClient();
    }
    
    /**
     * Send a message to the Service Bus queue.
     * Extracted as a separate method to facilitate testing.
     */
    static void sendMessage(ServiceBusSenderClient sender, String messageText) {
        ServiceBusMessage message = new ServiceBusMessage(messageText);
        sender.sendMessage(message);
    }
}

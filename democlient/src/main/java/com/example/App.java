package com.example;

import com.awsomeasb.AwsomeMQClient;

import java.io.BufferedReader;
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
        
        String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
        
        if (connectionString == null || connectionString.trim().isEmpty()) {
            System.err.println("Error: AZURE_SERVICEBUS_CONNECTION_STRING environment variable is not set.");
            System.err.println("\nPlease set the connection string:");
            System.err.println("  export AZURE_SERVICEBUS_CONNECTION_STRING='<your-connection-string>'");
            return;
        }
        
        System.out.println("Connecting to Azure Service Bus...");
        
        try (AwsomeMQClient client = new AwsomeMQClient(connectionString, QUEUE_NAME)) {
            
            System.out.println("Connected successfully!");
            System.out.println("Enter news messages (type 'exit' or 'quit' to stop):\n");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
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
                
                // Publish message to queue
                client.basicPublish(QUEUE_NAME, message.getBytes("UTF-8"));
                System.out.println("✓ Published: " + message);
            }
            
        } catch (Exception e) {
            System.err.println("Error connecting to Azure Service Bus: " + e.getMessage());
            System.err.println("\nMake sure:");
            System.err.println("1. AZURE_SERVICEBUS_CONNECTION_STRING is set correctly");
            System.err.println("2. The Service Bus namespace and queue exist");
            System.err.println("3. You have appropriate permissions");
            e.printStackTrace();
        }
    }
}

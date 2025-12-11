package com.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ News Publisher Client
 */
public class App 
{
    private static final String RABBITMQ_HOST = "localhost";
    private static final String QUEUE_NAME = "news";
    
    public static void main( String[] args )
    {
        System.out.println("=== RabbitMQ News Publisher ===");
        System.out.println("Connecting to RabbitMQ at " + RABBITMQ_HOST + "...");
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        // Optional: Set username and password if required
        // factory.setUsername("guest");
        // factory.setPassword("guest");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // Declare queue (idempotent - safe to call even if queue exists)
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            
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
                channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
                System.out.println("✓ Published: " + message);
            }
            
        } catch (IOException | TimeoutException e) {
            System.err.println("Error connecting to RabbitMQ: " + e.getMessage());
            System.err.println("\nMake sure RabbitMQ is running on " + RABBITMQ_HOST);
            System.err.println("You can start it with: docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management");
            e.printStackTrace();
        }
    }
}

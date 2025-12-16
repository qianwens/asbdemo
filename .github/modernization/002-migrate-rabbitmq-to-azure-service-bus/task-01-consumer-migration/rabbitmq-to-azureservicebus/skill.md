---
name: Migrate from RabbitMQ to Azure Service Bus
description: Migrate from RabbitMQ with AMQP to Azure Service Bus for messaging.
---

# RabbitMQ to Azure Service Bus Migration Instructions

## Overview

This document provides comprehensive instructions for migrating from RabbitMQ to Azure Service Bus.

### Maven Project

copy file awesomeasb-1.0.0.jar to classpath.

Add to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.awsomeasb</groupId>
        <artifactId>awesomeasb</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Choose your SLF4J implementation -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

### Gradle Project

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.awsomeasb:awesomeasb:1.0.0'
    implementation 'org.slf4j:slf4j-simple:2.0.9'
}
```

## 3. Example Usage in Your App

```java
import com.awsomeasb.AwsomeMQClient;
import com.awsomeasb.DeliverCallback;

public class MyApplication {
    public static void main(String[] args) {
        String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
        String queueName = "myqueue";
        
        try (AwsomeMQClient client = new AwsomeMQClient(connectionString, queueName)) {
            
            // Publish a message
            client.basicPublish(queueName, "Hello World!".getBytes());
            
            // Consume messages
            DeliverCallback callback = (tag, delivery) -> {
                System.out.println("Received: " + delivery.getBodyAsString());
            };
            
            String consumerTag = client.basicConsume(queueName, true, callback);
            Thread.sleep(10000);
            client.basicCancel(consumerTag);
        }
    }
}
```

### Using Managed Identity

```java
import com.awsomeasb.AwsomeMQClient;

// Uses DefaultAzureCredential (Azure CLI, Managed Identity, etc.)
try (AwsomeMQClient client = new AwsomeMQClient(
        "myservicebus.servicebus.windows.net",
        "myqueue", 
        null)) {
    
    client.basicPublish("myqueue", "Secure message".getBytes());
}
```


## API Reference

### Main Classes

- **AwsomeMQClient** - Main client for publishing and consuming messages
- **DeliverCallback** - Callback interface for message delivery
- **CancelCallback** - Callback interface for consumer cancellation
- **Delivery** - Wrapper for received messages

### Core Methods

```java
// Publishing
void basicPublish(String routingKey, byte[] body)
void basicPublish(String routingKey, Map<String, Object> properties, byte[] body)

// Consuming
String basicConsume(String queueName, boolean autoAck, DeliverCallback callback)
String basicConsume(String queueName, boolean autoAck, DeliverCallback callback, CancelCallback cancelCallback)

// Consumer management
void basicCancel(String consumerTag)

// Message acknowledgment
void basicAck(Delivery delivery)
void basicNack(Delivery delivery, boolean requeue)
```

## Troubleshooting

**Issue: SLF4J warnings**
```
Solution: Add an SLF4J implementation to your project (slf4j-simple, logback, log4j2, etc.)
```

**Issue: Authentication failures with Managed Identity**
```
Solution: 
- Run 'az login' for local development
- Grant Azure Service Bus Data Sender/Receiver roles
- Verify namespace URL format: 'namespace.servicebus.windows.net'
```

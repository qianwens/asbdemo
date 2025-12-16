---
name: Migrate from RabbitMQ(AMQP) to Azure Service Bus
description: Migrate from RabbitMQ with AMQP to Azure Service Bus for messaging.
---

# RabbitMQ to Azure Service Bus Migration Instructions

## Overview

This document provides comprehensive instructions for migrating from RabbitMQ to Azure Service Bus using Spring AMQP and Spring Messaging framework.

### Migration Scope

- **Source**: RabbitMQ with Spring AMQP
- **Target**: Azure Service Bus with Spring Messaging
- **Framework**: Spring Boot with Spring Cloud Azure
- **Approach**: Via Spring AMQP and Spring Messaging

### Tags

- RabbitMQ
- Azure Service Bus
- Message Broker
- Message Queue
- Messaging
- AMQP
- Spring

---

## 1. Migrate RabbitMQ Dependencies

### Target Files
- `pom.xml` (Maven)
- `build.gradle` or `build.gradle.kts` (Gradle)

### Detection Pattern
Files containing: `spring-boot-starter-amqp`, `spring-rabbit`, or `spring-amqp`

### Migration Steps

#### 1.1 Remove RabbitMQ Dependencies

Remove all dependencies with the following artifactIds:
- `spring-boot-starter-amqp`
- `spring-rabbit`
- `spring-rabbit-test`
- `spring-amqp`

**Important**: Delete the dependency blocks completely - do not comment them out.

#### 1.2 Add Azure Service Bus Dependencies

**Add Managed Dependency (BOM)**:
```xml
<!-- Maven pom.xml -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.azure.spring</groupId>
            <artifactId>spring-cloud-azure-dependencies</artifactId>
            <version>5.22.0</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```gradle
// Gradle build.gradle or build.gradle.kts
dependencies {
    implementation platform("com.azure.spring:spring-cloud-azure-dependencies:5.22.0")
}
```

**Version Notes**:
- For Spring Boot 2.x: Use version `4.20.0`
- For Spring Boot 3.x: Use version `5.22.0` or check for the latest version
- Define a property named `spring-cloud-azure.version` for the BOM version

**Add Required Dependencies**:
```xml
<!-- Maven -->
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-cloud-azure-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.azure.spring</groupId>
    <artifactId>spring-messaging-azure-servicebus</artifactId>
</dependency>
```

```gradle
// Gradle
implementation 'com.azure.spring:spring-cloud-azure-starter'
implementation 'com.azure.spring:spring-messaging-azure-servicebus'
```

**Important**: Only update lines related to RabbitMQ or Service Bus - keep other dependencies unchanged and minimize changes.

---

## 2. Migrate RabbitMQ Properties

### Target Files
- `application.yml`
- `application.yaml`
- `application.properties`
- Configuration files containing `rabbitmq`

### Detection Pattern
Files containing: `rabbitmq` (case-insensitive)

### Migration Steps

#### 2.1 Migrate Connection Settings

**Remove RabbitMQ Properties**:
Remove properties matching `spring.rabbitmq.*` including:
- `spring.rabbitmq.host`
- `spring.rabbitmq.port`
- `spring.rabbitmq.addresses`
- `spring.rabbitmq.username`
- `spring.rabbitmq.password`
- `spring.rabbitmq.virtual-host`
- `spring.rabbitmq.ssl.enabled`

Do NOT replace these with `spring.servicebus.*` prefix.

**Add Service Bus Connection Settings**:

```yaml
spring:
  cloud:
    azure:
      credential:
        managed-identity-enabled: true
        client-id: ${AZURE_CLIENT_ID}
      servicebus:
        entity-type: queue  # or "topic" - see topology analysis below
        namespace: ${SERVICE_BUS_NAMESPACE}
```

**Entity Type Selection**:
- **Queue**: If only Spring beans of `Queue` objects exist (no Exchange or Binding)
- **Topic**: If Spring beans of `Exchange` or `Binding` objects exist

#### 2.2 Update Comments and Property Names

1. Find comment lines containing `rabbitmq`
2. Replace `rabbitmq` with `Service Bus` in comments
3. For properties with `rabbitmq` in the name, replace with `servicebus`

#### 2.3 Docker Compose Files

If using docker-compose with RabbitMQ images:
- Remove the RabbitMQ container
- Remove related usages

**Important**: 
- Pay attention to YAML structural correctness when adding properties
- DO NOT optimize unrelated code blocks
- KEEP commented-out code
- Minimize the amount of changes

---

## 3. Migrate RabbitMQ ConnectionFactory

### Target Files
Java files containing: `rabbit.connection.`

### Detection Pattern
```java
org.springframework.amqp.rabbit.connection.ConnectionFactory
com.rabbitmq.client.ConnectionFactory
```

### Migration Steps

#### 3.1 Remove ConnectionFactory Beans

Completely remove:
- ConnectionFactory beans
- ConnectionFactory class members
- ConnectionFactory variables
- Related variables: `host`, `port`, `username`, `password`, `virtual-host`, `ssl.enabled`

**Example of code to remove**:
```java
@Bean
public ConnectionFactory connectionFactory() {
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
    connectionFactory.setHost(rabbitMQHost);
    connectionFactory.setPort(rabbitMQPort);
    connectionFactory.setUsername(rabbitMQUsername);
    connectionFactory.setPassword(rabbitMQPassword);
    connectionFactory.setVirtualHost(rabbitMQVirtualHost);
    return connectionFactory;
}
```

**DO NOT** create a Service Bus connection factory bean as a replacement. The Azure Service Bus library provides auto-configuration for connection management.

#### 3.2 Clean Up Imports

Remove all AMQP-related imports:
- Imports starting with `org.springframework.amqp`
- Imports starting with `com.rabbitmq`
- Any other unused RabbitMQ-related imports

---

## 4. Migrate RabbitMQ Listeners

### Target Files
Java files containing: `RabbitListener`, `RabbitHandler`, `EnableRabbit`, `AmqpHeaders`

### Migration Steps

#### 4.1 Update Annotations

**Remove**:
- `@EnableRabbit`

**Add**:
- `@EnableAzureMessaging` (class level)

**Replace**:
- `@RabbitListener` → `@ServiceBusListener`

#### 4.2 Update @ServiceBusListener Parameters

Analyze the RabbitMQ topology from dependency files:

**Queue Topology** (only Queue beans, no Exchange/Binding):
```java
// Before
@RabbitListener(queues = "queueName")

// After
@ServiceBusListener(destination = "queueName")
```

**Topic/Subscription Topology** (with Exchange or Binding beans):
Analyze binding relationships between queues and exchanges.

```java
// Before
@RabbitListener(queues = "queueName")

// After
@ServiceBusListener(destination = "topicName", group = "subscriptionName")
```

Where:
- RabbitMQ Exchange name → Service Bus Topic name
- RabbitMQ Queue name → Service Bus Subscription name

**With @QueueBinding**:
```java
// Before
@RabbitListener(bindings = @QueueBinding(
    key = "contractEvents.*",
    value = @Queue("queueName"),
    exchange = @Exchange(value = "exchangeName", type = ExchangeTypes.TOPIC)
))

// After
@ServiceBusListener(destination = "exchangeName", group = "queueName")
```

If only Exchange name is present, generate Subscription name by adding "sub" suffix to Topic name.

**Note**: Do NOT migrate RabbitListener's `group` parameter to ServiceBusListener.

#### 4.3 Update Listener Method Parameters

Migrate parameters according to these rules (don't add new parameters not in original):

| RabbitMQ Parameter | Service Bus Parameter |
|-------------------|----------------------|
| `T msg` | `T msg` (unchanged) |
| Custom domain object | Keep unchanged |
| `Channel channel` | `@Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context` |
| `org.springframework.amqp.core.Message` | `org.springframework.messaging.Message<T>` |
| `@Headers Map<String, Object> headers` | `@Headers Map<String, Object> headers` (unchanged) |

**Channel Migration**:
```java
// Before
public void listener(T payload, Channel channel) {
    channel.basicAck(...);
    channel.basicNack(...);
}

// After
public void listener(T payload, @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context) {
    if (context != null) {
        context.complete();  // for basicAck
        context.abandon();   // for basicNack
    }
}
```

#### 4.4 Use ServiceBusProcessorClient for Advanced Features

For RabbitListener functions not supported by ServiceBusListener, use `ServiceBusProcessorClient`:

```java
@Autowired
private ServiceBusClientBuilder clientBuilder;

@Autowired
private DefaultAzureCredential credential;

@Autowired
private AzureServiceBusProperties properties;

@PostConstruct
public void setupProcessor() {
    ServiceBusProcessorClient processor = clientBuilder
        .credential(properties.getFullyQualifiedNamespace(), credential)
        .processor()
        .queueName("queueName")  // or topicName() and subscriptionName()
        .processMessage(this::handleMessage)
        .processError(this::handleError)
        .buildProcessorClient();
    
    processor.start();
}

private void handleMessage(ServiceBusReceivedMessageContext context) {
    // Convert from RabbitListener method handler
}

private void handleError(ServiceBusErrorContext errorContext) {
    // Add logging or custom error handling
}
```

#### 4.5 Clean Up Imports

Remove:
- Imports starting with `org.springframework.amqp`
- Imports starting with `com.rabbitmq`
- Unused RabbitMQ-related imports

Add necessary imports for Service Bus classes.

### Key API References

**RabbitListener Annotation**:
```java
Package: org.springframework.amqp.rabbit.annotation

Properties:
- id: Container identifier
- containerFactory: RabbitListenerContainerFactory bean name
- queues: Queue names, property placeholders, or expressions
- concurrency: Listener container concurrency
- group: Consumer group name (client-side management)
```

**ServiceBusListener Annotation**:
```java
Package: com.azure.spring.messaging.servicebus.implementation.core.annotation

Properties:
- id: Container identifier
- containerFactory: MessageListenerContainerFactory bean name
- destination: Destination name (queue or topic)
- group: Subscription name (for topics)
- concurrency: Override container factory concurrency (int value)
```

**ServiceBusReceivedMessageContext**:
```java
Package: com.azure.messaging.servicebus

Methods:
- complete(): Acknowledge message
- abandon(): Reject message
```

**ServiceBusProcessorClient**:
```java
Package: com.azure.messaging.servicebus

Methods:
- start(): Start processing messages
- close(): Stop processing and close
```

### Migration Examples

**1. Listener with Payload**:
```java
// No change needed
public void listener(T payload) { }
```

**2. Listener with Message**:
```java
// Before
public void listener(Message message) {
    T body = JSON.parseObject(message.getBody(), T.class);
}

// After
public void listener(Message<T> message) {
    T body = message.getPayload();
}
```

**3. Listener with Payload and Headers**:
```java
// No change needed
public void listener(T msg, @Headers Map<String, Object> headers) { }
```

**4. Listener with Channel**:
```java
// Before
public void listener(T payload, Message message, Channel channel) {
    // ...
}

// After
public void listener(T payload, Message<T> message, 
    @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT) ServiceBusReceivedMessageContext context) {
    // ...
}
```

**5. Topic/Subscription Example**:
```java
// Before
@Bean("demoExchange")
public DirectExchange demoExchange() {
    return new DirectExchange("demoExchange");
}

@Bean("demoQueue")
public Queue demoQueue() {
    return QueueBuilder.durable("demoQueue").build();
}

@Bean
public Binding demoBinding() {
    return BindingBuilder.bind(demoQueue()).to(demoExchange()).with("demo.key").noargs();
}

@RabbitListener(queues = "demoQueue")
public void listener(T message) { }

// After
@Bean
ServiceBusAdministrationClient adminClient(AzureServiceBusProperties properties, TokenCredential credential) {
    return new ServiceBusAdministrationClientBuilder()
        .credential(properties.getFullyQualifiedNamespace(), credential)
        .buildClient();
}

@Bean("demoTopic")
public TopicProperties demoTopic(ServiceBusAdministrationClient adminClient) {
    try {
        return adminClient.getTopic("demoTopic");
    } catch (ResourceNotFoundException e) {
        return adminClient.createTopic("demoTopic");
    }
}

@Bean("demoSubscription")
@DependsOn("demoTopic")
public SubscriptionProperties demoSubscription(ServiceBusAdministrationClient adminClient) {
    try {
        return adminClient.getSubscription("demoTopic", "demoSubscription");
    } catch (ResourceNotFoundException e) {
        CorrelationRuleFilter ruleFilter = new CorrelationRuleFilter();
        ruleFilter.setLabel("demo.key");
        CreateRuleOptions createRuleOptions = new CreateRuleOptions().setFilter(ruleFilter);
        return adminClient.createSubscription("demoTopic", "demoSubscription",
            "default_rule_name", new CreateSubscriptionOptions(), createRuleOptions);
    }
}

@ServiceBusListener(destination = "demoTopic", group = "demoSubscription")
public void listener(T message) { }
```

---

## 5. Migrate RabbitMQ MessageConverter

### Target Files
Java files containing: `org.springframework.amqp.support.converter.`

### Migration Steps

#### 5.1 Locate MessageConverter Classes

From package `org.springframework.amqp.support.converter`, apply one of these scenarios:

**Scenario 1: Default Converter Without Customization**

Remove the bean or class member entirely without replacement:

```java
// Remove completely
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Bean
public MessageConverter converter() {
    return new Jackson2JsonMessageConverter();
}
```

**Scenario 2: Jackson2JsonMessageConverter with Customized ObjectMapper**

Replace with `ServiceBusMessageConverter`:

```java
// Before
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Bean
public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return new Jackson2JsonMessageConverter(objectMapper);
}

// After
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.azure.spring.messaging.servicebus.implementation.support.converter.ServiceBusMessageConverter;

@Bean
public ServiceBusMessageConverter serviceBusMessageConverter() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return new ServiceBusMessageConverter(objectMapper);
}
```

#### 5.2 Remove RabbitTemplate/AmqpTemplate Beans

Remove RabbitTemplate or AmqpTemplate bean definitions completely. DO NOT create a ServiceBusTemplate bean as replacement.

```java
// Remove this code entirely
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Bean
public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
    return rabbitTemplate;
}
```

ServiceBusTemplate is automatically provided by Azure ServiceBus auto-configuration.

#### 5.3 Clean Up Imports

Remove:
- Imports starting with `org.springframework.amqp`
- Imports starting with `com.rabbitmq`

**Important**:
- DO NOT optimize unrelated code blocks
- KEEP commented-out code
- Minimize changes

---

## 6. Migrate RabbitTemplate Send Methods

### Target Files
Java files containing: `RabbitTemplate`, `AmqpTemplate`, `AmqpHeaders` (case-insensitive)

### Migration Steps

#### 6.1 Replace RabbitTemplate Methods with ServiceBusTemplate

Replace all RabbitMQ API references with equivalent Service Bus APIs.

**Key Migration Rules**:
1. When constructing Service Bus messages, check for routing key in context
   - If routing key present: Set as message header `ServiceBusMessageHeaders.SUBJECT`
   - If no routing key: Do not set SUBJECT header

2. Migrate message properties/headers:
   - Use Spring standard message headers when available (no migration needed)
   - If RabbitMQ-specific, find Spring standard headers first
   - Otherwise use Service Bus message headers
   - Remove RabbitMQ properties with no equivalent

3. ServiceBusTemplate does not accept empty or null parameters

4. DO NOT optimize unrelated code - KEEP commented code and minimize changes

5. Add necessary imports for all newly added classes

#### 6.2 Handle Unsupported Methods

**Receive Methods (Not Supported)**:

`RabbitTemplate.receive()` and `RabbitTemplate.receiveAndConvert()` must use `ServiceBusReceiverClient`:

```java
@Autowired
private ServiceBusClientBuilder clientBuilder;

@Autowired
private DefaultAzureCredential credential;

@Autowired
private AzureServiceBusProperties properties;

private ServiceBusReceiverClient receiverClient;

@PostConstruct
public void setupReceiver() {
    receiverClient = clientBuilder
        .credential(properties.getFullyQualifiedNamespace(), credential)
        .receiver()
        .queueName("queueName")  // or topicName() and subscriptionName()
        .buildClient();
}

// Replace receive code
IterableStream<ServiceBusReceivedMessage> messages = receiverClient.receiveMessages(10);
for (ServiceBusReceivedMessage message : messages) {
    // Process message
    receiverClient.complete(message);
}

@PreDestroy
public void cleanup() {
    if (receiverClient != null) {
        receiverClient.close();
    }
}
```

**SendAndReceive Methods (RPC Pattern)**:

`RabbitTemplate.sendAndReceive()` and `RabbitTemplate.convertSendAndReceive()` can be migrated to `ServiceBusTemplate.sendAndReceive()`:

Prerequisites:
1. Define a Bean for session-enabled consumer:
```java
@Bean
PropertiesSupplier<ConsumerIdentifier, ConsumerProperties> consumerPropertiesSupplier() {
    return key -> {
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setSessionEnabled(true);
        return consumerProperties;
    };
}
```

2. Analyze codebase to find RabbitMQ reply_to entity and convert to Service Bus entity

3. Set `MessageHeaders.REPLY_CHANNEL` header with reply entity name before calling sendAndReceive:
```java
Map<String, Object> headers = new HashMap<>();
headers.put(MessageHeaders.REPLY_CHANNEL, "replyQueueName");
Message<T> message = MessageBuilder.createMessage(payload, new MessageHeaders(headers));

ServiceBusReceivedMessage reply = serviceBusTemplate.sendAndReceive(
    "destination", 
    ServiceBusEntityType.QUEUE, 
    message
);
```

**Receive and Reply Pattern**:

For sending within `@RabbitListener` where target is from message header (e.g., `amqp_replyTo`):

```java
// Get reply destination from header
String replyTo = (String) message.getHeaders().get(MessageHeaders.REPLY_CHANNEL);

// Set session ID from incoming message
Message<?> replyMessage = MessageBuilder
    .withPayload(responsePayload)
    .setHeader(ServiceBusMessageHeaders.SESSION_ID, 
        message.getHeaders().get(ServiceBusMessageHeaders.REPLY_TO_SESSION_ID))
    .build();

serviceBusTemplate.send(replyTo, replyMessage);
```

**Callback Methods (Not Compatible)**:

Remove directly without migration:
- `RabbitTemplate.setReturnsCallback()`
- `RabbitTemplate.setConfirmCallback()`

#### 6.3 Clean Up Imports

Remove:
- Imports starting with `org.springframework.amqp`
- Imports starting with `com.rabbitmq`
- Any other unused RabbitMQ-related imports

### API References

**RabbitTemplate Interface**:
```java
Package: org.springframework.amqp.rabbit.core

Methods:
- RabbitTemplate()
- RabbitTemplate(ConnectionFactory connectionFactory)
- setMessageConverter(MessageConverter messageConverter)
- convertAndSend(Object object)
- convertAndSend(String routingKey, Object object)
- convertAndSend(String exchange, String routingKey, Object object)
- setReturnsCallback(ReturnCallback returnCallback)
- setConfirmCallback(ConfirmCallback confirmCallback)
- receive(): Message
- receiveAndConvert(): Object
```

**ServiceBusTemplate Interface**:
```java
Package: com.azure.spring.messaging.servicebus.core

Methods:
- setMessageConverter(AzureMessageConverter<ServiceBusReceivedMessage, ServiceBusMessage> messageConverter)
- send(String destination, Message<T> message): void
- sendAsync(String destination, Message<T> message): void
- sendAndReceive(String destination, ServiceBusEntityType entityType, Message<T> message): ServiceBusReceivedMessage
```

**ServiceBusReceiverClient**:
```java
Package: com.azure.messaging.servicebus

Methods:
- receiveMessages(int maxMessages): IterableStream<ServiceBusReceivedMessage>
- abandon(ServiceBusReceivedMessage message): void
- complete(ServiceBusReceivedMessage message): void
```

**Message Interface**:
```java
Package: org.springframework.messaging

Methods:
- getPayload(): T
- getHeaders(): MessageHeaders
```

**MessageBuilder Interface**:
```java
Package: org.springframework.messaging.support

Methods:
- setHeader(String headerName, Object headerValue): MessageBuilder<T>
- withPayload(T payload): MessageBuilder<T>
- build(): Message<T>
```

### Supported Message Headers

**Spring Standard Headers**:
```java
Package: org.springframework.messaging.MessageHeaders

- CONTENT_TYPE: RFC2045 Content-Type descriptor
- REPLY_CHANNEL: Address for reply messages
```

**Service Bus Headers**:
```java
Package: com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders

- CORRELATION_ID: Correlation ID
- REPLY_TO_SESSION_ID: ReplyToGroupId property
- SCHEDULED_ENQUEUE_TIME: Datetime for scheduled enqueue (use for delayed messages)
- SESSION_ID: Session identifier for session-aware entity
- TIME_TO_LIVE: Message expiration duration
- SUBJECT: Message subject (use for routing key)
```

### Migration Examples

**1. Convert and Send with Routing Key**:
```java
// Before
rabbitTemplate.convertAndSend("topic", "routing.key", strMessage);

// After
Message<String> message = MessageBuilder.withPayload(strMessage)
    .setHeader(ServiceBusMessageHeaders.SUBJECT, "routing.key")
    .build();
serviceBusTemplate.send("topic", message);
```

**2. Convert and Send without Routing Key**:
```java
// Before
rabbitTemplate.convertAndSend("queueName", strMessage);

// After
Message<String> message = MessageBuilder.withPayload(strMessage).build();
serviceBusTemplate.send("queueName", message);
```

**3. Migrate Message Properties to Spring Headers**:
```java
// Before
MessageProperties properties = new MessageProperties();
properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
Message responseMessage = new Message(responseString.getBytes(), properties);

// After
Message<String> responseMessage = MessageBuilder.withPayload(responseString)
    .setHeader(MessageHeaders.CONTENT_TYPE, "application/json")
    .build();
```

**4. Migrate Delayed Message**:
```java
// Before
long delayTime = ChronoUnit.MILLIS.between(LocalDateTime.now(), localDateTime);
MessageProperties properties = new MessageProperties();
properties.setDelay((int) delayTime);
Message message = new Message(payload, messageProperties);
rabbitTemplate.convertAndSend("destination", "binding-key", message);

// After
Message<String> message = MessageBuilder.withPayload(payload)
    .setHeader(ServiceBusMessageHeaders.SUBJECT, "binding-key")
    .setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, 
        OffsetDateTime.now().plus(Duration.ofMillis(delayTime)))
    .build();
serviceBusTemplate.send("destination", message);
```

**5. RPC Pattern (SendAndReceive)**:
```java
// Before
Message msg = new Message(message.getBytes());
log.info("RPC INVOCATION TO RABBITMQ");
Object response = rabbitTemplate.sendAndReceive(
    inputBinding.getExchangeName(), 
    inputBinding.getRoutineKey(), 
    msg
);
if (response instanceof byte[]) {
    return new String((byte[]) response, StandardCharsets.UTF_8);
}

// After
// 1. Add Bean for session support
@Bean
PropertiesSupplier<ConsumerIdentifier, ConsumerProperties> consumerPropertiesSupplier() {
    return key -> {
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setSessionEnabled(true);
        return consumerProperties;
    };
}

// 2. Set REPLY_CHANNEL and send
Map<String, Object> headers = new HashMap<>();
headers.put(MessageHeaders.REPLY_CHANNEL, RPC_QUEUE_REPLY_NAME);
Message<byte[]> message = MessageBuilder.createMessage(
    message.getBytes(), 
    new MessageHeaders(headers)
);

ServiceBusReceivedMessage replyMessage = serviceBusTemplate.sendAndReceive(
    inputBinding.getExchangeName(), 
    ServiceBusEntityType.QUEUE, 
    message
);

if (replyMessage != null && replyMessage.getBody() != null) {
    return replyMessage.getBody().toString();
}
```

**6. Receive and Reply Pattern**:
```java
// Before
@RabbitListener(bindings = {
    @QueueBinding(
        value = @Queue,
        exchange = @Exchange(value = EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
        key = "prepend"
    )
})
public void onMessage(StringMessage msg, Message message) {
    String text = msg.getBody();
    System.out.println("PrependHello.onMessage - " + text);
    String result = "hello, " + text;
    template.convertAndSend(
        message.getMessageProperties().getReplyTo(), 
        new StringMessage(result)
    );
}

// After
@ServiceBusListener(destination = EXCHANGE_NAME, group = SUBSCRIPTION_PREPEND)
public void onMessage(StringMessage msg, Message<?> message) {
    String text = msg.getBody();
    System.out.println("PrependHello.onMessage - " + text);
    String result = "hello, " + text;

    Message<?> sbMessage = MessageBuilder
        .withPayload(new StringMessage(result))
        .setHeader(ServiceBusMessageHeaders.SESSION_ID, 
            message.getHeaders().get(ServiceBusMessageHeaders.REPLY_TO_SESSION_ID))
        .build();

    serviceBusTemplate.send(
        message.getHeaders().get(MessageHeaders.REPLY_CHANNEL).toString(), 
        sbMessage
    );
}
```

---

## 7. Migrate RabbitMQ Resources

### Target Files
Java files containing: `amqp.core.`

### Migration Steps

#### 7.1 Analyze Resource Topology

Describe the code to identify queues, exchanges, and binding relationships:

**Queue-Only Topology**:
- Only Beans of `org.springframework.amqp.core.Queue`
- Migrate to Service Bus `QueueProperties`

**Topic/Subscription Topology**:
- Beans of Queue + Binding + Exchange
- Migrate Queue + Binding → `SubscriptionProperties`
- Migrate Exchange → `TopicProperties`
- Do NOT create Service Bus queues in this scenario

#### 7.2 Resource Mapping

| RabbitMQ Resource | Service Bus Resource |
|------------------|---------------------|
| `AmqpAdmin` / `RabbitAdmin` | `ServiceBusAdministrationClient` |
| `TopicExchange` / `DirectExchange` / `CustomExchange` | `TopicProperties` |
| `Queue` (with Binding) | `SubscriptionProperties` |
| `Queue` (without Binding) | `QueueProperties` |
| `RabbitListenerContainerFactory` | `PropertiesSupplier<ConsumerIdentifier, ProcessorProperties>` |
| `RabbitTemplate` (with custom properties) | `PropertiesSupplier<String, ProducerProperties>` |

#### 7.3 Initialize ServiceBusAdministrationClient

```java
@Bean
public ServiceBusAdministrationClient adminClient(
        AzureServiceBusProperties properties, 
        TokenCredential credential) {
    return new ServiceBusAdministrationClientBuilder()
        .credential(properties.getFullyQualifiedNamespace(), credential)
        .buildClient();
}
```

Autowire `AzureServiceBusProperties` and `TokenCredential` beans from Spring Context.

#### 7.4 Clean Up

- Remove all `org.springframework.amqp` imports
- Remove all unused imports
- Add necessary imports for Service Bus classes
- Perform all migrations in the original file (don't create new files)

### API References

**Azure Service Bus Classes**:
```java
Package: com.azure.spring.cloud.autoconfigure.implementation.servicebus.properties
- AzureServiceBusProperties

Package: com.azure.core.credential
- TokenCredential

Package: com.azure.core.exception
- ResourceNotFoundException

Package: com.azure.messaging.servicebus.administration.models
- TopicProperties
- SubscriptionProperties
- QueueProperties
- CorrelationRuleFilter
- CreateRuleOptions
- CreateSubscriptionOptions

Package: com.azure.messaging.servicebus.administration
- ServiceBusAdministrationClient
- ServiceBusAdministrationClientBuilder

Package: org.springframework.context.annotation
- @DependsOn
```

**PropertiesSupplier Interface**:
```java
Package: com.azure.spring.messaging

public interface PropertiesSupplier<K, V> {
    V getProperties(K key);
}
```

**ConsumerIdentifier Class**:
```java
Package: com.azure.spring.messaging

Constructors:
- ConsumerIdentifier(String destination)  // For Service Bus Queue
- ConsumerIdentifier(String destination, String group)  // For Topic
```

**ProcessorProperties Class**:
```java
Package: com.azure.spring.messaging.servicebus.core.properties

Methods:
- setMaxConcurrentCalls(Integer maxConcurrentCalls)
- setAutoComplete(Boolean autoComplete)
- setPrefetchCount(Integer prefetchCount)
- setSubscriptionName(String subscriptionName)
- getRetry(): AmqpRetryProperties
```

**ProducerProperties Class**:
```java
Package: com.azure.spring.messaging.servicebus.core.properties

Methods:
- getRetry(): AmqpRetryProperties
```

**AmqpRetryProperties Interface**:
```java
Package: com.azure.spring.cloud.core.properties.retry

Methods:
- setTryTimeout(Duration tryTimeout)
- setMode(RetryMode mode)  // "FIXED" or "EXPONENTIAL"
- getFixed(): FixedRetryProperties
- getExponential(): ExponentialRetryProperties
```

**FixedRetryProperties**:
```java
Package: com.azure.spring.cloud.core.properties.retry

Methods:
- setMaxRetries(Integer maxRetries)
- setDelay(Duration delay)
```

**ExponentialRetryProperties**:
```java
Package: com.azure.spring.cloud.core.properties.retry

Methods:
- setMaxRetries(Integer maxRetries)
- setBaseDelay(Duration baseDelay)
- setMaxDelay(Duration maxDelay)
```

### Migration Examples

**1. Create Topic (from Exchange)**:
```java
// Before
@Bean
DirectExchange exchange() {
    return new DirectExchange("exchangeName");
}

// After
@Bean
public TopicProperties topicProperties(
        ServiceBusAdministrationClient adminClient,
        String topicName) {
    try {
        return adminClient.getTopic(topicName);
    } catch (ResourceNotFoundException e) {
        return adminClient.createTopic(topicName);
    }
}
```

**2. Create Subscription with Filter (from Queue + Binding)**:
```java
// Before
@Bean
public Queue queue(String queueName) {
    return new Queue(queueName);
}

@Bean
public Binding binding(Queue queue, DirectExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(routingKey);
}

// After
@Bean
@DependsOn("topicProperties")
public SubscriptionProperties subscription(
        ServiceBusAdministrationClient adminClient,
        String topicName,
        String subscriptionName,
        String routeKey) {
    try {
        return adminClient.getSubscription(topicName, subscriptionName);
    } catch (ResourceNotFoundException e) {
        CreateSubscriptionOptions subOptions = new CreateSubscriptionOptions();
        CorrelationRuleFilter filter = new CorrelationRuleFilter()
            .setLabel(routeKey);
        CreateRuleOptions ruleOptions = new CreateRuleOptions()
            .setFilter(filter);
        return adminClient.createSubscription(
            topicName, 
            subscriptionName, 
            "RouteKey", 
            subOptions, 
            ruleOptions
        );
    }
}
```

**3. Create Queue (Queue without Binding)**:
```java
// Before
@Bean
public Queue queue(String queueName) {
    return new Queue(queueName);
}

// After
@Bean
public QueueProperties queue(
        ServiceBusAdministrationClient adminClient, 
        String queueName) {
    try {
        return adminClient.getQueue(queueName);
    } catch (ResourceNotFoundException e) {
        return adminClient.createQueue(queueName);
    }
}
```

---

## 8. Remove RabbitMQ Test Components

### Target Files
Java files containing: `RabbitMQContainer`

### Migration Steps

#### 8.1 Locate and Remove

Remove `RabbitMQContainer` usage and cleanup related code:
- Remove RabbitMQContainer class references
- Remove related test setup code
- Remove related imports

**Important**:
- Delete the code blocks completely - do NOT comment them out
- DO NOT optimize unrelated code blocks
- KEEP other commented-out code
- Minimize the amount of changes

---

## 9. Migrate Variable Names

### Target Files
Java files with variables containing: `rabbitmq` (case-insensitive)

### Detection Pattern
Match `rabbitmq` but not in `package`, `import`, or `class` declarations

### Migration Steps

#### 9.1 Rename Variables

Locate variables that consume from environment variables or configuration files:
- If variable name contains `rabbitmq`, rename to use `servicebus`

**Important Restrictions**:
- DO NOT update strings in package names
- DO NOT update strings in class names
- DO NOT update strings in function names
- DO NOT optimize unrelated code blocks
- KEEP commented-out code
- Minimize changes

---

## General Migration Guidelines

### Code Modification Principles

Throughout the migration process, follow these principles:

1. **Minimal Changes**: Only modify code directly related to the migration
2. **Preserve Comments**: Keep all commented-out code unchanged
3. **Delete, Don't Comment**: When removing RabbitMQ code, delete it completely
4. **Exact Line Updates**: Only update lines related to RabbitMQ or Service Bus
5. **Import Management**: Always clean up unused imports and add required new imports
6. **Same File Migration**: Perform all migrations in original files, don't create new files

### Testing Recommendations

After migration:

1. Verify all dependencies are correctly updated
2. Ensure environment variables are set:
   - `AZURE_CLIENT_ID`
   - `SERVICE_BUS_NAMESPACE`
3. Test message sending and receiving functionality
4. Verify topic/subscription or queue topology matches expected behavior
5. Validate session-enabled scenarios if using RPC patterns
6. Test error handling and message acknowledgment

### Common Pitfalls

1. **Don't create ServiceBusTemplate beans**: Auto-configuration provides them
2. **Don't migrate RabbitListener's `group` parameter**: It has different meaning in Service Bus
3. **Don't use empty/null parameters**: ServiceBusTemplate requires valid values
4. **Don't forget routing key migration**: Use `SUBJECT` header when routing key exists
5. **Session requirement for RPC**: Must enable sessions for sendAndReceive scenarios

### Version Compatibility

- **Spring Boot 2.x**: Use spring-cloud-azure-dependencies version 4.20.0
- **Spring Boot 3.x**: Use spring-cloud-azure-dependencies version 5.22.0 or latest

---

## Summary

This migration guide covers the complete transition from RabbitMQ to Azure Service Bus:

1. ✅ Dependencies (pom.xml/build.gradle)
2. ✅ Configuration properties (application.yml/properties)
3. ✅ ConnectionFactory removal
4. ✅ Listener annotations and methods
5. ✅ MessageConverter migration
6. ✅ Template methods (send/receive)
7. ✅ Resource creation (queues, topics, subscriptions)
8. ✅ Test component cleanup
9. ✅ Variable naming conventions

Follow the sections in order for a systematic migration approach, or reference individual sections as needed for specific components.

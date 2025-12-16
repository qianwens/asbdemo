---
name: Migrate from AWS S3 to Azure Blob Storage
description: Migrate from AWS S3 to Azure Blob Storage for scalable and secure object storage in Azure.
---

# AWS S3 to Azure Blob Storage Migration Guide

## Overview

This guide provides comprehensive instructions for migrating a Java application from AWS S3 storage to Azure Blob Storage. The migration covers all aspects including dependencies, client configuration, object operations, and exception handling.

**Target Hierarchy:** Storage Tasks  
**Tags:** Azure Blob Storage, Storage, AWS S3

---

## Table of Contents

1. [Dependency Migration](#1-dependency-migration)
   - [Maven (pom.xml)](#maven-pomxml)
   - [Gradle (build.gradle)](#gradle-buildgradle)
2. [Configuration Migration](#2-configuration-migration)
3. [Client Builder Migration](#3-client-builder-migration)
4. [Bucket/Container Operations](#4-bucketcontainer-operations)
   - [Create Bucket/Container](#create-bucketcontainer)
   - [Delete Bucket/Container](#delete-bucketcontainer)
   - [Head Bucket/Container](#head-bucketcontainer)
   - [List Buckets/Containers](#list-bucketscontainers)
5. [Object/Blob Operations](#5-objectblob-operations)
   - [Put Object/Upload Blob](#put-objectupload-blob)
   - [Get Object/Download Blob](#get-objectdownload-blob)
   - [Copy Object/Copy Blob](#copy-objectcopy-blob)
   - [Delete Object/Delete Blob](#delete-objectdelete-blob)
   - [Head Object/Get Blob Properties](#head-objectget-blob-properties)
   - [List Objects/List Blobs](#list-objectslist-blobs)
6. [Advanced Operations](#6-advanced-operations)
   - [Multipart Upload/Block Blob Upload](#multipart-uploadblock-blob-upload)
   - [Presigned URLs/SAS Tokens](#presigned-urlssas-tokens)
   - [Access Policies/SAS Tokens](#access-policiessas-tokens)
   - [Restore Object/Copy Blob](#restore-objectcopy-blob)
7. [Object Model Migration](#7-object-model-migration)
8. [Exception Handling](#8-exception-handling)

---

## Important Package Information

**CRITICAL:** Always use the correct package imports for Azure Storage Blob classes:

```java
// Core Azure Blob Storage classes - CORRECT IMPORTS
import com.azure.storage.blob.BlobClient;               // NOT in the models package
import com.azure.storage.blob.BlobContainerClient;      // NOT in the models package
import com.azure.storage.blob.BlobServiceClient;        // NOT in the models package
import com.azure.storage.blob.BlobServiceClientBuilder; // NOT in the models package

// Models and other supporting classes
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

// Specialized blob clients
import com.azure.storage.blob.specialized.BlobClientBase;    // For specialized operations
import com.azure.storage.blob.specialized.BlockBlobClient;   // For block blob operations

// Batch operations
import com.azure.storage.blob.batch.BlobBatchClient;

// Authentication
import com.azure.identity.DefaultAzureCredentialBuilder;
```

**⚠️ IMPORTANT:** The import `com.azure.storage.blob.models.BlobClient` is **INCORRECT**. Always use `com.azure.storage.blob.BlobClient` instead.

---

## 1. Dependency Migration

### Maven (pom.xml)

**AWS S3 Dependencies to Remove:**

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
<dependency>
    <groupId>io.awspring.cloud</groupId>
    <artifactId>spring-cloud-aws-starter-s3</artifactId>
</dependency>
<dependency>
    <groupId>com.amazonaws</groupId>
    <artifactId>aws-java-sdk-s3</artifactId>
</dependency>
```

**Azure Blob Storage Dependencies to Add:**

```xml
<!-- Managed dependency (add to dependencyManagement section) -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-sdk-bom</artifactId>
            <version>1.2.36</version> <!-- Use the latest version available -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Dependencies (add to dependencies section) -->
<dependencies>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-storage-blob</artifactId>
    </dependency>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-storage-blob-batch</artifactId>
    </dependency>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
        <version>1.16.3</version>
    </dependency>
</dependencies>
```

**Note:** All three Azure-related dependencies should be provided: one for blob operations, one for batch operations, and one for authentication credentials.

### Gradle (build.gradle)

**AWS S3 Dependencies to Remove:**

```gradle
implementation 'software.amazon.awssdk:s3:X.Y.Z'
implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3:X.Y.Z'
implementation 'com.amazonaws:aws-java-sdk-s3:X.Y.Z'
```

Or in Kotlin DSL:

```kotlin
implementation("software.amazon.awssdk:s3:X.Y.Z")
implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:X.Y.Z")
implementation("com.amazonaws:aws-java-sdk-s3:X.Y.Z")
```

**Azure Blob Storage Dependencies to Add:**

```gradle
implementation platform('com.azure:azure-sdk-bom:1.2.36')

implementation 'com.azure:azure-storage-blob'
implementation 'com.azure:azure-storage-blob-batch'
implementation 'com.azure:azure-identity'
```

Or in Kotlin DSL:

```kotlin
implementation(platform("com.azure:azure-sdk-bom:1.2.36"))

implementation("com.azure:azure-storage-blob")
implementation("com.azure:azure-storage-blob-batch")
implementation("com.azure:azure-identity:1.16.3")
```

---

## 2. Configuration Migration

### application.properties / application.yaml

When migrating configuration files, pay attention to S3-related configurations and replace them with Azure Blob Storage equivalents.

**Common Mappings:**

- `s3.bucket.name` → `blob.container.name`
- `s3.key.name` → `blob.name`

**Note:** 
- AWS S3 has a unique property called `location`, which is usually not needed in Azure Blob Storage.
- Azure Blob Storage sometimes requires the storage account name or entire endpoint to be provided.
- Only modify configurations when you are certain they are S3-related.

**Example Migration:**

```properties
# Before (S3)
s3.bucket.name=my-bucket
s3.key.name=my-object
s3.region=us-west-2

# After (Azure Blob)
blob.container.name=my-container
blob.name=my-blob
blob.storage.account.name=mystorageaccount
blob.endpoint=https://mystorageaccount.blob.core.windows.net
```

---

## 3. Client Builder Migration

### S3Client → BlobServiceClient

**AWS S3 Client Creation:**

```java
// S3Client (AWS SDK v2)
S3Client s3Client = S3Client.builder()
    .region(Region.US_WEST_2)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build();
```

**Azure Blob Storage Client Creation:**

```java
// Set your storage account endpoint
String endpoint = "https://yourstorageaccount.blob.core.windows.net";

// Create BlobServiceClient using credential and endpoint
BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
    .endpoint(endpoint)
    .credential(new DefaultAzureCredentialBuilder().build())
    .buildClient();
```

**Important Notes:**

1. **Do NOT use connection strings:** Avoid using `connectionString()` to initialize `BlobServiceClient`. Use `DefaultAzureCredential` instead for better security.
2. **Do NOT use BlockBlobClient directly:** Use `BlobClient` for standard operations.
3. **BlobServiceClient is NOT AutoCloseable:** Do not include it in try-with-resources like `try(BlobServiceClient xxx)`.
4. **Default authentication:** By default, use `DefaultAzureCredential` + endpoint as parameters to initialize the blob client.

### AWS S3 Client Builder API Reference

**Interface: S3Client**  
Package: `software.amazon.awssdk.services.s3`

- `static S3ClientBuilder builder()` - Create a builder for configuring an S3Client

**Interface: S3ClientBuilder**  
Package: `software.amazon.awssdk.services.s3`

- `S3ClientBuilder credentialsProvider(AwsCredentialsProvider credentialsProvider)` - Configure credentials
- `S3ClientBuilder region(Region region)` - Configure the region
- `S3Client build()` - Build the client

### Azure Blob Storage Client Builder API Reference

**Class: BlobServiceClientBuilder**  
Package: `com.azure.storage.blob`

- `BlobServiceClient buildClient()` - Build a BlobServiceClient
- `BlobServiceClientBuilder connectionString(String connectionString)` - Set connection string (not recommended)
- `BlobServiceClientBuilder credential(TokenCredential credential)` - Set TokenCredential for authorization (recommended)
- `BlobServiceClientBuilder credential(StorageSharedKeyCredential credential)` - Set StorageSharedKeyCredential
- `BlobServiceClientBuilder endpoint(String endpoint)` - Set the blob service endpoint

---

## 4. Bucket/Container Operations

### Create Bucket/Container

**AWS S3:**

```java
CreateBucketRequest request = CreateBucketRequest.builder()
    .bucket("my-bucket")
    .build();
CreateBucketResponse response = s3Client.createBucket(request);
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
containerClient.create();  // Creates container, fails if exists
// OR
boolean created = containerClient.createIfNotExists();  // Creates only if doesn't exist
```

**API Reference:**

**AWS Interface: CreateBucketRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `CreateBucketRequest.Builder bucket(String bucket)` - Set bucket name

**Azure Class: BlobContainerClient**  
Package: `com.azure.storage.blob`

- `void create()` - Create container (fails if exists)
- `boolean createIfNotExists()` - Create container if not exists (returns true if created, false if already exists)

### Delete Bucket/Container

**AWS S3:**

```java
DeleteBucketRequest request = DeleteBucketRequest.builder()
    .bucket("my-bucket")
    .build();
DeleteBucketResponse response = s3Client.deleteBucket(request);
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
containerClient.delete();  // Deletes container
// OR
boolean deleted = containerClient.deleteIfExists();  // Deletes only if exists
```

**API Reference:**

**AWS Interface: DeleteBucketRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `DeleteBucketRequest.Builder bucket(String bucket)` - Set bucket name

**Azure Class: BlobContainerClient**  
Package: `com.azure.storage.blob`

- `void delete()` - Delete container
- `boolean deleteIfExists()` - Delete container if exists (returns true if deleted, false if not found)

### Head Bucket/Container

**AWS S3:**

```java
HeadBucketRequest request = HeadBucketRequest.builder()
    .bucket("my-bucket")
    .build();
HeadBucketResponse response = s3Client.headBucket(request);
String region = response.bucketRegion();
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlobContainerProperties properties = containerClient.getProperties();
// OR with additional options
Response<BlobContainerProperties> response = containerClient.getPropertiesWithResponse(
    leaseId, timeout, context);
```

**API Reference:**

**AWS Class: HeadBucketRequest**  
Package: `software.amazon.awssdk.services.s3.model`

- `String bucket()` - Get bucket name

**AWS Class: HeadBucketResponse**  
Package: `software.amazon.awssdk.services.s3.model`

- `String bucketRegion()` - Get bucket region

**Azure Class: BlobContainerClient**  
Package: `com.azure.storage.blob`

- `BlobContainerProperties getProperties()` - Get container properties
- `Response<BlobContainerProperties> getPropertiesWithResponse(String leaseId, Duration timeout, Context context)` - Get properties with response metadata

### List Buckets/Containers

**AWS S3:**

```java
ListBucketsRequest request = ListBucketsRequest.builder()
    .prefix("my-prefix")
    .maxBuckets(100)
    .bucketRegion("us-west-2")
    .build();
ListBucketsResponse response = s3Client.listBuckets(request);
List<Bucket> buckets = response.buckets();
```

**Azure Blob Storage:**

```java
// List all containers
PagedIterable<BlobContainerItem> containers = blobServiceClient.listBlobContainers();

// OR with options
ListBlobContainersOptions options = new ListBlobContainersOptions()
    .setPrefix("my-prefix")
    .setMaxResultsPerPage(100);
PagedIterable<BlobContainerItem> containers = blobServiceClient.listBlobContainers(options, timeout);

// Iterate through containers
for (BlobContainerItem container : containers) {
    String name = container.getName();
    Map<String, String> metadata = container.getMetadata();
}
```

**API Reference:**

**AWS Interface: ListBucketsRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `ListBucketsRequest.Builder bucketRegion(String bucketRegion)` - Filter by region
- `ListBucketsRequest.Builder maxBuckets(Integer maxBuckets)` - Set max results
- `ListBucketsRequest.Builder prefix(String prefix)` - Filter by prefix

**Azure Class: BlobServiceClient**  
Package: `com.azure.storage.blob`

- `PagedIterable<BlobContainerItem> listBlobContainers()` - List all containers
- `PagedIterable<BlobContainerItem> listBlobContainers(ListBlobContainersOptions options, Duration timeout)` - List with options

**Azure Class: ListBlobContainersOptions**  
Package: `com.azure.storage.blob.models`

- `ListBlobContainersOptions setPrefix(String prefix)` - Filter by prefix
- `ListBlobContainersOptions setMaxResultsPerPage(Integer maxResultsPerPage)` - Set max results per page
- `ListBlobContainersOptions setDetails(BlobContainerListDetails details)` - Set details to include

**Azure Class: BlobContainerItem**  
Package: `com.azure.storage.blob.models`

- `String getName()` - Get container name
- `Map<String, String> getMetadata()` - Get container metadata
- `BlobContainerItemProperties getProperties()` - Get container properties

---

## 5. Object/Blob Operations

### Put Object/Upload Blob

**AWS S3:**

```java
PutObjectRequest request = PutObjectRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .contentType("text/plain")
    .contentLength(1024L)
    .metadata(Map.of("key", "value"))
    .build();
PutObjectResponse response = s3Client.putObject(request, RequestBody.fromFile(file));
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlobClient blobClient = containerClient.getBlobClient("my-blob");

// Simple upload
blobClient.uploadFromFile("path/to/file");

// OR with BinaryData
BinaryData data = BinaryData.fromFile(Paths.get("path/to/file"));
blobClient.upload(data, true);  // true = overwrite

// OR with InputStream
try (InputStream inputStream = new FileInputStream(file)) {
    blobClient.upload(inputStream, file.length(), true);
}

// OR with options (contentType, metadata, etc.)
BlobHttpHeaders headers = new BlobHttpHeaders()
    .setContentType("text/plain");

BlobParallelUploadOptions options = new BlobParallelUploadOptions(data)
    .setHeaders(headers)
    .setMetadata(Map.of("key", "value"));

Response<BlockBlobItem> response = blobClient.uploadWithResponse(options, timeout, context);
```

**Important Notes:**

1. **BlobParallelUploadOptions constructor:** Use ONLY one of these two forms:
   - `new BlobParallelUploadOptions(binaryDataVariable)` OR
   - `new BlobParallelUploadOptions(inputStreamVariable)`
   
   Do NOT pass a `File` type directly! Use setter methods for additional parameters like `setHeaders()`.

2. **Package:** `BlobParallelUploadOptions` is in `com.azure.storage.blob.options`

3. **Imports:** Import `BinaryData` from `com.azure.core.util` or `InputStream` from `java.io` based on your choice.

**API Reference:**

**AWS Interface: PutObjectRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `PutObjectRequest.Builder bucket(String bucket)` - Set bucket name
- `PutObjectRequest.Builder key(String key)` - Set object key
- `PutObjectRequest.Builder contentType(String contentType)` - Set content type
- `PutObjectRequest.Builder contentLength(Long contentLength)` - Set content length
- `PutObjectRequest.Builder metadata(Map<String, String> metadata)` - Set metadata

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `void upload(BinaryData data)` - Upload data (no overwrite)
- `void upload(BinaryData data, boolean overwrite)` - Upload with overwrite option
- `void upload(InputStream data)` - Upload from stream
- `void upload(InputStream data, long length, boolean overwrite)` - Upload from stream with length
- `void uploadFromFile(String filePath)` - Upload from file
- `void uploadFromFile(String filePath, boolean overwrite)` - Upload from file with overwrite
- `Response<BlockBlobItem> uploadWithResponse(BlobParallelUploadOptions options, Duration timeout, Context context)` - Upload with full options

**Azure Class: BlobParallelUploadOptions**  
Package: `com.azure.storage.blob.options`

- Constructor: `BlobParallelUploadOptions(BinaryData data)`
- Constructor: `BlobParallelUploadOptions(InputStream dataStream)`
- `BlobParallelUploadOptions setHeaders(BlobHttpHeaders headers)` - Set HTTP headers
- `BlobParallelUploadOptions setMetadata(Map<String, String> metadata)` - Set metadata

**Azure Class: BlobHttpHeaders**  
Package: `com.azure.storage.blob.models`

- `BlobHttpHeaders setContentType(String contentType)` - Set content type

### Get Object/Download Blob

**AWS S3:**

```java
GetObjectRequest request = GetObjectRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .range("bytes=0-1023")  // Optional range
    .build();
ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlobClient blobClient = containerClient.getBlobClient("my-blob");

// Simple download to file
BlobProperties properties = blobClient.downloadToFile("path/to/file", true);  // true = overwrite

// OR download to memory (for blobs up to 2GB)
BinaryData content = blobClient.downloadContent();

// OR download to stream
try (OutputStream outputStream = new FileOutputStream("path/to/file")) {
    blobClient.downloadStream(outputStream);
}

// OR download with range
BlobRange range = new BlobRange(0, 1024L);  // offset=0, count=1024
blobClient.downloadStreamWithResponse(outputStream, range, options, requestConditions, 
    getRangeContentMd5, timeout, context);
```

**Important Notes:**

1. **Leverage `downloadToFile`:** Use this method to download a blob to a file. Don't create your own implementation.
2. **Range downloads:** Use `downloadStreamWithResponse` or `downloadContentWithResponse` with `BlobRange` parameter.
3. **Package:** `BlobClient` is in `com.azure.storage.blob`, not in the models package.

**API Reference:**

**AWS Interface: GetObjectRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `GetObjectRequest.Builder bucket(String bucket)` - Set bucket name
- `GetObjectRequest.Builder key(String key)` - Set object key
- `GetObjectRequest.Builder range(String range)` - Set byte range

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `BinaryData downloadContent()` - Download entire blob to memory (up to 2GB)
- `void downloadStream(OutputStream stream)` - Download to output stream
- `BlobProperties downloadToFile(String filePath, boolean overwrite)` - Download to file
- `BlobDownloadResponse downloadStreamWithResponse(OutputStream stream, BlobRange range, DownloadRetryOptions options, BlobRequestConditions requestConditions, boolean getRangeContentMd5, Duration timeout, Context context)` - Download with full options
- `BlobDownloadContentResponse downloadContentWithResponse(DownloadRetryOptions options, BlobRequestConditions requestConditions, BlobRange range, boolean getRangeContentMd5, Duration timeout, Context context)` - Download content with options

**Azure Class: BlobRange**  
Package: `com.azure.storage.blob.models`

- Constructor: `BlobRange(long offset)` - Range from offset to end
- Constructor: `BlobRange(long offset, Long count)` - Range with specific byte count

### Copy Object/Copy Blob

**AWS S3:**

```java
CopyObjectRequest request = CopyObjectRequest.builder()
    .sourceBucket("source-bucket")
    .sourceKey("source-object")
    .destinationBucket("dest-bucket")
    .destinationKey("dest-object")
    .build();
CopyObjectResponse response = s3Client.copyObject(request);
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlobClient destinationBlob = containerClient.getBlobClient("destination-blob");

String sourceUrl = "https://sourcestorageaccount.blob.core.windows.net/container/source-blob";

// Simple copy
SyncPoller<BlobCopyInfo, Void> poller = destinationBlob.beginCopy(sourceUrl, Duration.ofSeconds(1));
poller.waitForCompletion();

// OR with options
BlobBeginCopyOptions options = new BlobBeginCopyOptions(sourceUrl);
SyncPoller<BlobCopyInfo, Void> poller = destinationBlob.beginCopy(options);
```

**API Reference:**

**AWS Class: CopyObjectRequest**  
Package: `software.amazon.awssdk.services.s3.model`

- `String destinationBucket()` - Get destination bucket
- `String sourceBucket()` - Get source bucket

**AWS Class: CopyObjectResponse**  
Package: `software.amazon.awssdk.services.s3.model`

- `CopyObjectResult copyObjectResult()` - Get copy result

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `SyncPoller<BlobCopyInfo, Void> beginCopy(String sourceUrl, Duration pollInterval)` - Begin copy operation
- `SyncPoller<BlobCopyInfo, Void> beginCopy(BlobBeginCopyOptions options)` - Begin copy with options

### Delete Object/Delete Blob

**AWS S3 (Single Object):**

```java
DeleteObjectRequest request = DeleteObjectRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .versionId("version-id")  // Optional
    .build();
DeleteObjectResponse response = s3Client.deleteObject(request);
```

**AWS S3 (Multiple Objects):**

```java
Delete delete = Delete.builder()
    .objects(ObjectIdentifier.builder().key("object1").build(),
             ObjectIdentifier.builder().key("object2").build())
    .build();
DeleteObjectsRequest request = DeleteObjectsRequest.builder()
    .bucket("my-bucket")
    .delete(delete)
    .build();
DeleteObjectsResponse response = s3Client.deleteObjects(request);
```

**Azure Blob Storage (Single Blob):**

```java
BlobClient blobClient = containerClient.getBlobClient("my-blob");
blobClient.delete();  // Delete blob

// OR
boolean deleted = blobClient.deleteIfExists();  // Delete only if exists
```

**Azure Blob Storage (Multiple Blobs):**

```java
BlobBatchClient batchClient = new BlobBatchClientBuilder(blobServiceClient).buildClient();

List<String> blobUrls = Arrays.asList(
    "https://account.blob.core.windows.net/container/blob1",
    "https://account.blob.core.windows.net/container/blob2"
);

PagedIterable<Response<Void>> responses = batchClient.deleteBlobs(blobUrls, DeleteSnapshotsOptionType.INCLUDE);
```

**Important Note:** Only `BlobBatchClient` can perform multiple blob deletes via the `deleteBlobs` function.

**API Reference:**

**AWS Interface: DeleteObjectRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `DeleteObjectRequest.Builder bucket(String bucket)` - Set bucket name
- `DeleteObjectRequest.Builder key(String key)` - Set object key
- `DeleteObjectRequest.Builder versionId(String versionId)` - Set version ID

**AWS Interface: DeleteObjectsRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `DeleteObjectsRequest.Builder bucket(String bucket)` - Set bucket name
- `DeleteObjectsRequest.Builder delete(Delete delete)` - Set delete request container

**Azure Class: BlobClientBase**  
Package: `com.azure.storage.blob.specialized`

- `void delete()` - Delete blob
- `boolean deleteIfExists()` - Delete blob if exists

**Azure Class: BlobBatchClient**  
Package: `com.azure.storage.blob.batch`

- `PagedIterable<Response<Void>> deleteBlobs(List<String> blobUrls, DeleteSnapshotsOptionType deleteOptions)` - Delete multiple blobs

### Head Object/Get Blob Properties

**AWS S3:**

```java
HeadObjectRequest request = HeadObjectRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .versionId("version-id")  // Optional
    .build();
HeadObjectResponse response = s3Client.headObject(request);
Long contentLength = response.contentLength();
String eTag = response.eTag();
String versionId = response.versionId();
```

**Azure Blob Storage:**

```java
BlobClient blobClient = containerClient.getBlobClient("my-blob");
BlobProperties properties = blobClient.getProperties();

long blobSize = properties.getBlobSize();
String eTag = properties.getETag();
String versionId = properties.getVersionId();
```

**API Reference:**

**AWS Interface: HeadObjectRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `HeadObjectRequest.Builder bucket(String bucket)` - Set bucket name
- `HeadObjectRequest.Builder key(String key)` - Set object key
- `HeadObjectRequest.Builder versionId(String versionId)` - Set version ID

**AWS Class: HeadObjectResponse**  
Package: `software.amazon.awssdk.services.s3.model`

- `Long contentLength()` - Get object size
- `String eTag()` - Get ETag
- `String versionId()` - Get version ID

**Azure Class: BlobClientBase**  
Package: `com.azure.storage.blob.specialized`

- `BlobProperties getProperties()` - Get blob properties

**Azure Class: BlobProperties**  
Package: `com.azure.storage.blob.models`

- `long getBlobSize()` - Get blob size in bytes
- `String getETag()` - Get ETag
- `String getVersionId()` - Get version ID

### List Objects/List Blobs

**AWS S3:**

```java
ListObjectsV2Request request = ListObjectsV2Request.builder()
    .bucket("my-bucket")
    .prefix("my-prefix")
    .delimiter("/")
    .maxKeys(1000)
    .continuationToken("token")  // For pagination
    .build();
ListObjectsV2Response response = s3Client.listObjectsV2(request);
List<S3Object> objects = response.contents();
String nextToken = response.nextContinuationToken();
```

**Azure Blob Storage:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");

// Simple list
PagedIterable<BlobItem> blobs = containerClient.listBlobs();

// OR with options
ListBlobsOptions options = new ListBlobsOptions()
    .setPrefix("my-prefix")
    .setMaxResultsPerPage(1000);
PagedIterable<BlobItem> blobs = containerClient.listBlobs(options, timeout);

// Iterate through blobs
for (BlobItem blobItem : blobs) {
    String name = blobItem.getName();
    BlobItemProperties props = blobItem.getProperties();
    long size = props.getContentLength();
}
```

**Important Notes:**

1. **Special attention to filters:** Use `ListBlobsOptions` class to configure list operations.
2. **No manual pagination:** List APIs in Azure Storage Blob don't require manual control of page logic or continuation tokens. This is handled by the SDK automatically.

**API Reference:**

**AWS Interface: ListObjectsV2Request.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `ListObjectsV2Request.Builder bucket(String bucket)` - Set bucket name
- `ListObjectsV2Request.Builder prefix(String prefix)` - Filter by prefix
- `ListObjectsV2Request.Builder delimiter(String delimiter)` - Set delimiter
- `ListObjectsV2Request.Builder maxKeys(Integer maxKeys)` - Set max keys
- `ListObjectsV2Request.Builder continuationToken(String token)` - Set continuation token

**AWS Class: ListObjectsV2Response**  
Package: `software.amazon.awssdk.services.s3.model`

- `List<S3Object> contents()` - Get objects
- `String prefix()` - Get prefix
- `String delimiter()` - Get delimiter

**Azure Class: BlobContainerClient**  
Package: `com.azure.storage.blob`

- `PagedIterable<BlobItem> listBlobs()` - List all blobs
- `PagedIterable<BlobItem> listBlobs(ListBlobsOptions options, Duration timeout)` - List with options

**Azure Class: ListBlobsOptions**  
Package: `com.azure.storage.blob.models`

- `ListBlobsOptions setPrefix(String prefix)` - Filter by prefix
- `ListBlobsOptions setMaxResultsPerPage(Integer maxResultsPerPage)` - Set max results per page
- `ListBlobsOptions setDetails(BlobListDetails details)` - Set details to include

**Azure Class: BlobItem**  
Package: `com.azure.storage.blob.models`

- `String getName()` - Get blob name
- `BlobItemProperties getProperties()` - Get blob properties
- `Map<String, String> getMetadata()` - Get blob metadata

---

## 6. Advanced Operations

### Multipart Upload/Block Blob Upload

**AWS S3 Multipart Upload:**

```java
// 1. Initiate multipart upload
CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .metadata(Map.of("key", "value"))
    .build();
CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
String uploadId = createResponse.uploadId();

// 2. Upload parts
List<CompletedPart> completedParts = new ArrayList<>();
for (int i = 1; i <= numberOfParts; i++) {
    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
        .bucket("my-bucket")
        .key("my-object")
        .uploadId(uploadId)
        .partNumber(i)
        .contentLength(partSize)
        .build();
    UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromInputStream(partData, partSize));
    completedParts.add(CompletedPart.builder()
        .partNumber(i)
        .eTag(uploadPartResponse.eTag())
        .build());
}

// 3. Complete multipart upload
CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .uploadId(uploadId)
    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
    .build();
CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(completeRequest);

// 4. Abort multipart upload (if needed)
AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .uploadId(uploadId)
    .build();
s3Client.abortMultipartUpload(abortRequest);
```

**Azure Block Blob Upload:**

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlockBlobClient blockBlobClient = containerClient.getBlobClient("my-blob").getBlockBlobClient();

// 1. Stage blocks
List<String> blockIds = new ArrayList<>();
for (int i = 0; i < numberOfBlocks; i++) {
    String blockId = Base64.getEncoder().encodeToString(String.format("block-%05d", i).getBytes());
    blockIds.add(blockId);
    
    // Stage each block
    blockBlobClient.stageBlock(blockId, blockData, blockSize);
    // OR with options
    blockBlobClient.stageBlockWithResponse(blockId, blockData, blockSize, contentMd5, leaseId, timeout, context);
}

// 2. Commit block list
BlockBlobItem result = blockBlobClient.commitBlockList(blockIds);
```

**Important Note:** Block blob operations should use `BlockBlobClient` to perform staging and committing.

**API Reference:**

**AWS Interface: CreateMultipartUploadRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `CreateMultipartUploadRequest.Builder bucket(String bucket)` - Set bucket name
- `CreateMultipartUploadRequest.Builder key(String key)` - Set object key
- `CreateMultipartUploadRequest.Builder metadata(Map<String, String> metadata)` - Set metadata

**AWS Interface: UploadPartRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `UploadPartRequest.Builder bucket(String bucket)` - Set bucket name
- `UploadPartRequest.Builder key(String key)` - Set object key
- `UploadPartRequest.Builder uploadId(String uploadId)` - Set upload ID
- `UploadPartRequest.Builder partNumber(Integer partNumber)` - Set part number (1-10000)
- `UploadPartRequest.Builder contentLength(Long contentLength)` - Set content length

**AWS Interface: CompleteMultipartUploadRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `CompleteMultipartUploadRequest.Builder bucket(String bucket)` - Set bucket name
- `CompleteMultipartUploadRequest.Builder key(String key)` - Set object key
- `CompleteMultipartUploadRequest.Builder uploadId(String uploadId)` - Set upload ID

**Azure Class: BlockBlobClient**  
Package: `com.azure.storage.blob.specialized`

- `void stageBlock(String base64BlockId, BinaryData data)` - Stage a block
- `void stageBlock(String base64BlockId, InputStream data, long length)` - Stage a block from stream
- `Response<BlockBlobItem> stageBlockWithResponse(BlockBlobStageBlockOptions options, Duration timeout, Context context)` - Stage block with options
- `BlockBlobItem commitBlockList(List<String> base64BlockIds)` - Commit block list

### Presigned URLs/SAS Tokens

**AWS S3 Presigned URLs:**

```java
// AWS SDK v2
S3Presigner presigner = S3Presigner.create();

// For GET operations
GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
    .signatureDuration(Duration.ofMinutes(10))
    .getObjectRequest(GetObjectRequest.builder()
        .bucket("my-bucket")
        .key("my-object")
        .build())
    .build();
PresignedGetObjectRequest presignedGetRequest = presigner.presignGetObject(getObjectPresignRequest);
URL getUrl = presignedGetRequest.url();

// For PUT operations
PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
    .signatureDuration(Duration.ofMinutes(10))
    .putObjectRequest(PutObjectRequest.builder()
        .bucket("my-bucket")
        .key("my-object")
        .build())
    .build();
PresignedPutObjectRequest presignedPutRequest = presigner.presignPutObject(putObjectPresignRequest);
URL putUrl = presignedPutRequest.url();

// AWS SDK v1
AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
Date expiration = new Date(System.currentTimeMillis() + 600000); // 10 minutes

// Simple method
URL url = s3Client.generatePresignedUrl("my-bucket", "my-object", expiration);

// OR with method specification
URL url = s3Client.generatePresignedUrl("my-bucket", "my-object", expiration, HttpMethod.GET);

// OR with full request
GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("my-bucket", "my-object", HttpMethod.GET)
    .withExpiration(expiration)
    .withContentType("text/plain");
URL url = s3Client.generatePresignedUrl(request);
```

**Azure Blob Storage SAS Tokens:**

```java
BlobClient blobClient = containerClient.getBlobClient("my-blob");

// Generate SAS token for blob
BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
    OffsetDateTime.now().plusMinutes(10),  // Expiry time
    BlobSasPermission.parse("r")           // Read permission
);

String sasToken = blobClient.generateSas(sasValues);
String blobUrl = blobClient.getBlobUrl();
String sasUrl = blobUrl + "?" + sasToken;

// OR generate user delegation SAS (more secure, requires Azure AD)
UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(
    OffsetDateTime.now(),
    OffsetDateTime.now().plusMinutes(10)
);

String userDelegationSas = blobClient.generateUserDelegationSas(sasValues, userDelegationKey);
String userDelegationSasUrl = blobUrl + "?" + userDelegationSas;
```

**Key Differences:**

1. In AWS S3, presigned URLs are generated directly. In Azure, you generate a SAS token and append it to the blob URL.
2. Azure uses `BlobServiceSasSignatureValues` to specify SAS token parameters.
3. Handle expiry times correctly, as both APIs use different time unit approaches.

**API Reference:**

**AWS Interface: S3Presigner**  
Package: `software.amazon.awssdk.services.s3.presigner`

- `PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request)` - Generate presigned GET URL
- `PresignedPutObjectRequest presignPutObject(PutObjectPresignRequest request)` - Generate presigned PUT URL

**AWS Interface: GetObjectPresignRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.presigner.model`

- `GetObjectPresignRequest.Builder signatureDuration(Duration duration)` - Set URL validity duration
- `GetObjectPresignRequest.Builder getObjectRequest(GetObjectRequest request)` - Set GET request

**AWS Class: GeneratePresignedUrlRequest**  
Package: `com.amazonaws.services.s3.model`

- Constructor: `GeneratePresignedUrlRequest(String bucketName, String key)`
- Constructor: `GeneratePresignedUrlRequest(String bucketName, String key, HttpMethod method)`
- `void setExpiration(Date expiration)` - Set expiration date
- `void setMethod(HttpMethod method)` - Set HTTP method
- `void setContentType(String contentType)` - Set content type

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `String getBlobUrl()` - Get blob URL
- `String generateSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues)` - Generate SAS token
- `String generateUserDelegationSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues, UserDelegationKey userDelegationKey)` - Generate user delegation SAS

### Access Policies/SAS Tokens

**AWS S3 Access Policies:**

```java
// Put bucket policy
String policyJson = "{...}";  // JSON policy document
PutBucketPolicyRequest putRequest = PutBucketPolicyRequest.builder()
    .bucket("my-bucket")
    .policy(policyJson)
    .build();
s3Client.putBucketPolicy(putRequest);

// Get bucket policy
GetBucketPolicyRequest getRequest = GetBucketPolicyRequest.builder()
    .bucket("my-bucket")
    .build();
GetBucketPolicyResponse policyResponse = s3Client.getBucketPolicy(getRequest);

// Delete bucket policy
DeleteBucketPolicyRequest deleteRequest = DeleteBucketPolicyRequest.builder()
    .bucket("my-bucket")
    .build();
s3Client.deleteBucketPolicy(deleteRequest);

// Access Control Policy with grants
AccessControlPolicy acp = AccessControlPolicy.builder()
    .owner(Owner.builder().id("owner-id").build())
    .grants(
        Grant.builder()
            .permission(Permission.READ)
            .grantee(Grantee.builder().id("grantee-id").build())
            .build()
    )
    .build();
```

**Azure Blob Storage SAS Tokens:**

In Azure Storage Blob, access policies are managed using Shared Access Signature (SAS) tokens instead of JSON policy documents.

```java
BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient("my-container");
BlobClient blobClient = containerClient.getBlobClient("my-blob");

// Generate SAS token with specific permissions
BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
    OffsetDateTime.now().plusDays(1),  // Expiry time
    BlobSasPermission.parse("rw")      // Read and write permissions
);

// You can add IP restrictions, protocols, etc.
BlobServiceSasQueryParameters sasQueryParams = sasValues.generateSasQueryParameters(
    new StorageSharedKeyCredential(accountName, accountKey)
);

String sasToken = blobClient.generateSas(sasValues);
```

**Important Notes:**

1. The package of `BlobClient` is `com.azure.storage.blob`, not in the models package.
2. The package of `BlobContainerClient` is `com.azure.storage.blob.BlobContainerClient`.

**API Reference:**

**AWS Interface: PutBucketPolicyRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `PutBucketPolicyRequest.Builder bucket(String bucket)` - Set bucket name
- `PutBucketPolicyRequest.Builder policy(String policy)` - Set policy JSON

**AWS Interface: AccessControlPolicy.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `AccessControlPolicy.Builder grants(Collection<Grant> grants)` - Set grants
- `AccessControlPolicy.Builder owner(Owner owner)` - Set owner

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `String generateSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues)` - Generate SAS token
- `String generateSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues, Context context)` - Generate SAS with context

### Restore Object/Copy Blob

**AWS S3 Restore Object (from Glacier):**

```java
RestoreObjectRequest request = RestoreObjectRequest.builder()
    .bucket("my-bucket")
    .key("my-object")
    .versionId("version-id")  // Optional
    .build();
RestoreObjectResponse response = s3Client.restoreObject(request);
```

**Azure Blob Storage Copy Blob (Rehydrate from Archive):**

```java
BlobClient blobClient = containerClient.getBlobClient("my-blob");

String sourceUrl = "https://sourcestorageaccount.blob.core.windows.net/container/archived-blob";

BlobBeginCopyOptions options = new BlobBeginCopyOptions(sourceUrl)
    .setTier(AccessTier.HOT)                          // Destination tier
    .setRehydratePriority(RehydratePriority.HIGH);    // Rehydration priority

SyncPoller<BlobCopyInfo, Void> poller = blobClient.beginCopy(options);
poller.waitForCompletion();
```

**API Reference:**

**AWS Interface: RestoreObjectRequest.Builder**  
Package: `software.amazon.awssdk.services.s3.model`

- `RestoreObjectRequest.Builder bucket(String bucket)` - Set bucket name
- `RestoreObjectRequest.Builder key(String key)` - Set object key
- `RestoreObjectRequest.Builder versionId(String versionId)` - Set version ID

**Azure Class: BlobClient**  
Package: `com.azure.storage.blob`

- `SyncPoller<BlobCopyInfo, Void> beginCopy(BlobBeginCopyOptions options)` - Begin copy with options

**Azure Class: BlobBeginCopyOptions**  
Package: `com.azure.storage.blob.options`

- Constructor: `BlobBeginCopyOptions(String sourceUrl)`
- `BlobBeginCopyOptions setTier(AccessTier tier)` - Set destination tier
- `BlobBeginCopyOptions setRehydratePriority(RehydratePriority rehydratePriority)` - Set rehydration priority

**Azure Class: RehydratePriority**  
Package: `com.azure.storage.blob.models`

- `RehydratePriority.HIGH` - High priority rehydration
- `RehydratePriority.STANDARD` - Standard priority rehydration

**Azure Class: AccessTier**  
Package: `com.azure.storage.blob.models`

- `AccessTier.HOT` - Hot access tier
- `AccessTier.COOL` - Cool access tier
- `AccessTier.COLD` - Cold access tier
- `AccessTier.ARCHIVE` - Archive access tier

---

## 7. Object Model Migration

### S3Object → BlobItem

**AWS S3Object:**

```java
S3Object s3Object = S3Object.builder()
    .key("my-object")
    .size(1024L)
    .eTag("etag-value")
    .build();

String key = s3Object.key();
Long size = s3Object.size();
String eTag = s3Object.eTag();
```

**Azure BlobItem:**

```java
// BlobItem is typically returned from list operations
PagedIterable<BlobItem> blobs = containerClient.listBlobs();
for (BlobItem blobItem : blobs) {
    String name = blobItem.getName();
    BlobItemProperties properties = blobItem.getProperties();
    Long contentLength = properties.getContentLength();
    String eTag = properties.getETag();
    Map<String, String> metadata = blobItem.getMetadata();
}

// Or get properties directly from BlobClient
BlobClient blobClient = containerClient.getBlobClient("my-blob");
BlobProperties blobProperties = blobClient.getProperties();
long blobSize = blobProperties.getBlobSize();  // Note: getBlobSize(), not getContentLength()
```

**Important Note:**  
Be careful with `BlobItem.getProperties().getContentLength()` vs `blobClient.getProperties().getBlobSize()`.

**API Reference:**

**AWS Class: S3Object**  
Package: `software.amazon.awssdk.services.s3.model`

- `String eTag()` - Get ETag
- `Long size()` - Get object size

**Azure Class: BlobItem**  
Package: `com.azure.storage.blob.models`

- `String getName()` - Get blob name
- `BlobItemProperties getProperties()` - Get blob properties
- `Map<String, String> getMetadata()` - Get blob metadata

**Azure Class: BlobItemProperties**  
Package: `com.azure.storage.blob.models`

- `Long getContentLength()` - Get content length (size in bytes)
- `String getETag()` - Get ETag
- `AccessTier getAccessTier()` - Get access tier
- `String getContentEncoding()` - Get content encoding

---

## 8. Exception Handling

### S3 Exceptions → Azure Blob Exceptions

**AWS S3 Exceptions:**

```java
try {
    // S3 operations
} catch (BucketAlreadyExistsException e) {
    // Bucket already exists
} catch (NoSuchBucketException e) {
    // Bucket not found
} catch (NoSuchKeyException e) {
    // Key/object not found
} catch (InvalidWriteOffsetException e) {
    // Invalid write offset
} catch (InvalidRequestException e) {
    // Invalid request (multiple reasons)
} catch (S3Exception e) {
    // General S3 exception
    String message = e.getMessage();
} catch (AwsServiceException e) {
    // General AWS service exception
}
```

**Azure Blob Storage Exceptions:**

```java
try {
    // Blob operations
} catch (BlobStorageException e) {
    BlobErrorCode errorCode = e.getErrorCode();
    
    if (errorCode == BlobErrorCode.CONTAINER_ALREADY_EXISTS) {
        // Container already exists
    } else if (errorCode == BlobErrorCode.CONTAINER_NOT_FOUND) {
        // Container not found
    } else if (errorCode == BlobErrorCode.BLOB_NOT_FOUND) {
        // Blob not found
    } else if (errorCode == BlobErrorCode.INVALID_BLOB_OR_BLOCK) {
        // Invalid blob or block
    } else if (errorCode == BlobErrorCode.APPEND_POSITION_CONDITION_NOT_MET) {
        // Append position condition not met
    }
    
    String message = e.getMessage();
}
```

**API Reference:**

**AWS Exceptions:**

- **BucketAlreadyExistsException** (Package: `software.amazon.awssdk.services.s3.model`)  
  The requested bucket name is not available.

- **NoSuchBucketException** (Package: `software.amazon.awssdk.services.s3.model`)  
  The specified bucket does not exist.

- **NoSuchKeyException** (Package: `software.amazon.awssdk.services.s3.model`)  
  The specified key does not exist.

- **InvalidWriteOffsetException** (Package: `software.amazon.awssdk.services.s3.model`)  
  The write offset value does not match the current object size.

- **InvalidRequestException** (Package: `software.amazon.awssdk.services.s3.model`)  
  Invalid request (multiple potential reasons).

- **S3Exception** (Package: `software.amazon.awssdk.services.s3.model`)  
  Base exception for S3 operations.

- **AwsServiceException** (Package: `software.amazon.awssdk.awscore.exception.AwsServiceException`)  
  Base exception for AWS service operations.

**Azure Exceptions:**

- **BlobStorageException** (Package: `com.azure.storage.blob.models`)  
  Base exception for Blob Storage operations.

**Azure Error Codes:**

- **BlobErrorCode** (Package: `com.azure.storage.blob.models`)
  - `CONTAINER_ALREADY_EXISTS` - Container already exists
  - `CONTAINER_NOT_FOUND` - Container not found
  - `BLOB_NOT_FOUND` - Blob not found
  - `INVALID_BLOB_OR_BLOCK` - Invalid blob or block
  - `APPEND_POSITION_CONDITION_NOT_MET` - Append position condition not met

---

## Summary

This migration guide provides comprehensive instructions for migrating from AWS S3 to Azure Blob Storage. Key points to remember:

1. **Always import the correct packages** - BlobClient is in `com.azure.storage.blob`, not in models
2. **Use DefaultAzureCredential** instead of connection strings for authentication
3. **BlobServiceClient is not AutoCloseable** - don't use it in try-with-resources
4. **Azure handles pagination automatically** - no need to manually manage continuation tokens
5. **SAS tokens replace presigned URLs** - but the concept is similar
6. **Exception handling uses error codes** - check `BlobErrorCode` values
7. **Block blob operations use BlockBlobClient** - for multipart/staged uploads

For additional support and Azure Storage Blob documentation, visit:
- [Azure Storage Blob SDK for Java](https://docs.microsoft.com/en-us/java/api/overview/azure/storage-blob-readme)
- [Azure Storage Blob samples](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/storage/azure-storage-blob/src/samples)

---

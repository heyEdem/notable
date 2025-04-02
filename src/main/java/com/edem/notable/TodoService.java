package com.edem.notable;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final DynamoDbClient dynamoDbClient;

    private static final String TABLE_NAME = "TodoItems";
    private static final String COUNTER_TABLE_NAME = "Counters";

    @PostConstruct
    public void init() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(COUNTER_TABLE_NAME).build());
        } catch (ResourceNotFoundException e) {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(COUNTER_TABLE_NAME)
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("counter_name")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("counter_name")
                                    .keyType(KeyType.HASH)
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();
            dynamoDbClient.createTable(request);
            dynamoDbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(COUNTER_TABLE_NAME).build());

            // Initialize the counter
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("counter_name", AttributeValue.builder().s("todo_id").build());
            item.put("value", AttributeValue.builder().n("0").build());
            PutItemRequest initCounter = PutItemRequest.builder()
                    .tableName(COUNTER_TABLE_NAME)
                    .item(item)
                    .build();
            dynamoDbClient.putItem(initCounter);
        }
    }

    // Get the next ID from the counter
    private Long getNextId() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("counter_name", AttributeValue.builder().s("todo_id").build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(COUNTER_TABLE_NAME)
                .key(key)
                .updateExpression("SET #val = #val + :incr")
                .expressionAttributeNames(Map.of("#val", "value"))
                .expressionAttributeValues(Map.of(":incr", AttributeValue.builder().n("1").build()))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build();

        UpdateItemResponse response = dynamoDbClient.updateItem(request);
        return Long.parseLong(response.attributes().get("value").n());
    }

    // Create a Todo item
    public void createTodo(Todo todo) {
        Map<String, AttributeValue> item = new HashMap<>();
        Long id = getNextId(); // Get the next ID from the counter
        item.put("id", AttributeValue.builder().n(id.toString()).build());
        item.put("title", AttributeValue.builder().s(todo.getTitle()).build());
        item.put("description", AttributeValue.builder().s(todo.getDescription()).build());
        item.put("completed", AttributeValue.builder().bool(todo.isCompleted()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        dynamoDbClient.putItem(request);

        todo.setId(id);
    }

    // Read all Todo items
    public List<Todo> getAllTodos() {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build();
        ScanResponse response = dynamoDbClient.scan(request);
        List<Todo> todos = new ArrayList<>();

        for (Map<String, AttributeValue> item : response.items()) {
            Todo todo = new Todo();
            todo.setId(Long.parseLong(item.get("id").n()));
            todo.setTitle(item.get("title").s());
            todo.setDescription(item.get("description").s());
            todo.setCompleted(Boolean.parseBoolean(item.get("completed").bool().toString()));
            todos.add(todo);
        }
        return todos;
    }

    // Read a single Todo item by ID
    public Todo getTodoById(Long id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().n(id.toString()).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();
        GetItemResponse response = dynamoDbClient.getItem(request);

        if (response.item().isEmpty()) {
            return null;
        }

        Map<String, AttributeValue> item = response.item();
        Todo todo = new Todo();
        todo.setId(Long.parseLong(item.get("id").n()));
        todo.setTitle(item.get("title").s());
        todo.setDescription(item.get("description").s());
        todo.setCompleted(Boolean.parseBoolean(item.get("completed").bool().toString()));
        return todo;
    }

    // Update a Todo item
    public void updateTodo(Todo todo) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().n(todo.getId().toString()).build());

        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("title", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(todo.getTitle()).build())
                .action(AttributeAction.PUT)
                .build());
        updates.put("description", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(todo.getDescription()).build())
                .action(AttributeAction.PUT)
                .build());
        updates.put("completed", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().bool(todo.isCompleted()).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .attributeUpdates(updates)
                .build();
        dynamoDbClient.updateItem(request);
    }

    // Delete a Todo item
    public void deleteTodo(Long id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().n(id.toString()).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();
        dynamoDbClient.deleteItem(request);
    }
}

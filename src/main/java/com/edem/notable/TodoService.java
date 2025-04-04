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

    private static final String TABLE_NAME = "Notable";


    @PostConstruct
    public void init() {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("DynamoDB table "+ TABLE_NAME+ " not found", e);
        }
    }

    // Create a Todo item
    public void createTodo(Todo todo) {
        Map<String, AttributeValue> item = new HashMap<>();
        Long id = System.currentTimeMillis();
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
            Todo todo = Todo.builder()
                    .id(Long.parseLong(item.get("id").n()))
                    .title(item.get("title").s())
                    .description(item.get("description").s())
                    .completed(item.get("completed").bool())
                    .build();
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
        return Todo.builder()
                .id(Long.parseLong(item.get("id").n()))
                .title(item.get("title").s())
                .description(item.get("description").s())
                .completed(item.get("completed").bool())
                .build();
    }

    // New method for completed todos
    public List<Todo> getCompletedTodos() {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("completed = :val")
                .expressionAttributeValues(Map.of(":val", AttributeValue.builder().bool(true).build()))
                .build();
        ScanResponse response = dynamoDbClient.scan(request);
        return mapToTodos(response.items());
    }

    private List<Todo> mapToTodos(List<Map<String, AttributeValue>> items) {
        List<Todo> todos = new ArrayList<>();
        for (Map<String, AttributeValue> item : items) {
            Todo todo = Todo.builder()
                    .id(Long.parseLong(item.get("id").n()))
                    .title(item.get("title").s())
                    .description(item.get("description").s())
                    .completed(item.get("completed").bool())
                    .build();
            todos.add(todo);
        }
        return todos;
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

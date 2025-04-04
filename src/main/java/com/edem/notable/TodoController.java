package com.edem.notable;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // Display the list of todos
    @GetMapping({"/","/home"} )
    public String listTodos(Model model) {
        model.addAttribute("todos", todoService.getAllTodos());
        return "index";
    }

    @GetMapping("/completed")
    public String listCompletedTodos(Model model) {
        model.addAttribute("todos", todoService.getCompletedTodos());
        model.addAttribute("view", "completed");
        return "index";
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleTodo(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Todo todo = todoService.getTodoById(id);
        if (todo != null) {
            todo.setCompleted(body.get("completed"));
            todoService.updateTodo(todo);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    // Create a new todo
    @PostMapping("/create")
    public String createTodo(@ModelAttribute Todo todo) {
        todoService.createTodo(todo);
        return "redirect:/";
    }


    // Update an existing todo
    @PostMapping("/edit/{id}")
    public String updateTodo(@PathVariable Long id, @ModelAttribute Todo todo) {
        todo.setId(id);
        todoService.updateTodo(todo);
        return "redirect:/";
    }

    // Delete a todo
    @GetMapping("/delete/{id}")
    public String deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return "redirect:/";
    }
}
package com.edem.notable;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // Display the list of todos
    @GetMapping("/")
    public String listTodos(Model model) {
        model.addAttribute("todos", todoService.getAllTodos());
        return "index";
    }

    // Show the form to create a new todo
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("todo", new Todo());
        return "create";
    }

    // Create a new todo
    @PostMapping("/create")
    public String createTodo(@ModelAttribute Todo todo) {
        todoService.createTodo(todo);
        return "redirect:/";
    }

    // Show the form to edit an existing todo
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Todo todo = todoService.getTodoById(id);
        if (todo == null) {
            return "redirect:/";
        }
        model.addAttribute("todo", todo);
        return "edit";
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
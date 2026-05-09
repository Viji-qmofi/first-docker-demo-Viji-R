package com.example.docker.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.docker.entity.User;
import com.example.docker.repository.UserRepository;

@RestController
@RequestMapping("/users")

public class UserController {

	
	private final UserRepository repo;
	
	public UserController(UserRepository repo) {
		this.repo = repo;
	}
	
	@PostMapping
	public User save(@RequestBody User user) {
		return repo.save(user);
	}
	
	@GetMapping
	public List<User> getAll(){
		return repo.findAll();
	}
	
	@DeleteMapping("/delete/{user}")
	public void delete(@PathVariable User user) {
		repo.delete(user);
	}
}

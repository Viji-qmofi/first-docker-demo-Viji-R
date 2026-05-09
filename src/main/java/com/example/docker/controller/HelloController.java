package com.example.docker.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController

public class HelloController {

	@GetMapping("/hello")
	public String hello() {
		return "Hello from Spring Boot running inside Docker 🚀";
	}
	


}

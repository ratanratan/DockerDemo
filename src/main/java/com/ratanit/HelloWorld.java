package com.ratanit;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {
	
	@GetMapping("/hello")
	public String sayHello() {
		return "Welcome to Docker Example...saying Hello...";
	}
	
	  @GetMapping("/test")
	    public Map<String, String> hello() {
	        return Map.of(
	                "message", "Hello World from Spring Boot!",
	                "application", "hello-docker",
	                "status", "success"
	        );
	    }
}

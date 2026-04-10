package com.taskforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class TaskForgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskForgeApplication.class, args);
	}

}

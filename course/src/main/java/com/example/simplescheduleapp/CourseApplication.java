package com.example.simplescheduleapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SimpleScheduleAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleScheduleAppApplication.class, args);
	}
}

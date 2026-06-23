package com.cinema.hyperCinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HyperCinemaApplication {

	public static void main(String[] args) {
		SpringApplication.run(HyperCinemaApplication.class, args);
		System.out.println("=================================================");
		System.out.println("HyperCinema started successfully!");
		System.out.println("Access local site: http://localhost:8080");
		System.out.println("=================================================");
	}

}

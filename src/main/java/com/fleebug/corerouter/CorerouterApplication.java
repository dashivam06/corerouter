package com.fleebug.corerouter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorerouterApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorerouterApplication.class, args);
	}

}

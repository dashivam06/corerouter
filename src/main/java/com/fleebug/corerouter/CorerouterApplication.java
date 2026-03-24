package com.fleebug.corerouter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.microsoft.applicationinsights.attach.ApplicationInsights;

@SpringBootApplication
@EnableScheduling
public class CorerouterApplication {
	public static void main(String[] args) {
		ApplicationInsights.attach();
		SpringApplication.run(CorerouterApplication.class, args);
	}

}

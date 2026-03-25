package com.fleebug.corerouter;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CorerouterApplication {

	public static void main(String[] args) {
		String cs = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");

		if (cs == null || cs.isBlank()) {
			System.err.println("Application Insights not configured: missing env var APPLICATIONINSIGHTS_CONNECTION_STRING. Continuing with local logging only.");
		} else {
			System.setProperty("applicationinsights.connection.string", cs);
			try {
				ApplicationInsights.attach();
				System.out.println("Application Insights agent attached.");
			} catch (Exception ex) {
				System.err.println("Application Insights attach failed. Continuing without Azure Insights.");
			}
		}

		SpringApplication.run(CorerouterApplication.class, args);
	}

}

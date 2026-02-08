package com.agent.appointmentscheduler;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class AppointmentschedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentschedulerApplication.class, args);
	}

}

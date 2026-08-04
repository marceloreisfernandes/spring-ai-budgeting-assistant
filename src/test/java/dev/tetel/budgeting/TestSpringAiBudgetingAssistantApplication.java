package dev.tetel.budgeting;

import org.springframework.boot.SpringApplication;

public class TestSpringAiBudgetingAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringAiBudgetingAssistantApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package com.yjham.realtime;

import org.springframework.boot.SpringApplication;

public class TestRealtimeYjhamApplication {

	public static void main(String[] args) {
		SpringApplication.from(RealtimeYjhamApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

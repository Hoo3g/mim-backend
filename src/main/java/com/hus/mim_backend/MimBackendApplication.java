package com.hus.mim_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MimBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MimBackendApplication.class, args);
	}

}

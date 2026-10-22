package com.tugba.radyoloji_raporlama_asistani;

import org.springframework.boot.SpringApplication;

public class TestRadyolojiRaporlamaAsistaniApplication {

	public static void main(String[] args) {
		SpringApplication.from(RadyolojiRaporlamaAsistaniApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

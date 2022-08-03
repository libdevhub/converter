package com.file.converter.converter;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.file.converter.converter.service.FilesStorageService;

@SpringBootApplication
//@ComponentScan({"com.file.converter.converter.service"})
public class ConverterApplication {
	
//	@Resource
//	FilesStorageService storageService;

	public static void main(String[] args) {
		SpringApplication.run(ConverterApplication.class, args);
	}

//	@Override
//    public void run(String... arg) throws Exception {
//      storageService.deleteAll();
//      storageService.init();
//    }

}

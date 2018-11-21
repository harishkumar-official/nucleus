package com.nucleus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {"com.nucleus"})
public class NucleusStarter extends SpringBootServletInitializer {

  public static void main(String... args) throws Exception {
    SpringApplication.run(NucleusStarter.class, args);
  }

}

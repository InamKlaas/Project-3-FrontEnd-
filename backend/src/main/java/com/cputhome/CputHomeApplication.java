package com.cputhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.cputhome.config.AppProperties;

/* CPUT Home backend POC, stages 0-8. frontend stays untouched React. */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CputHomeApplication {

  public static void main(String[] args) {
    SpringApplication.run(CputHomeApplication.class, args);
  }
}

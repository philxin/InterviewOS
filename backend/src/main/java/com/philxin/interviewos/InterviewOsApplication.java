package com.philxin.interviewos;

import com.philxin.interviewos.config.RagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class InterviewOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewOsApplication.class, args);
    }

}

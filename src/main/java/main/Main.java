package main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"main.controller","main.data","main.services","main.domain"})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }
}
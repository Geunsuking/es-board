package esboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // "이게 스프링 부트 프로젝트다!"라고 선언
public class Main {
    public static void main(String[] args) {
        // 이 한 줄이 웹 서버(Tomcat)를 실행하고 8080 포트를 엽니다.
        SpringApplication.run(Main.class, args);
    }
}
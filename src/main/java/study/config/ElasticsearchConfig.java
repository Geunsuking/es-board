package study.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // "이 파일은 서버 설정 파일이야!"라고 알려주는 역할
public class ElasticsearchConfig {

    @Bean // "이 연결 도구(client)를 스프링이 관리해줘!"라는 뜻
    public RestHighLevelClient client() {
        // 엘라스틱서치와 통신할 주소를 적습니다. (기본 9200번 포트)
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        );
    }
}
package com.realtime.trend.test.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 싱글톤 설정
 * 테스트 클래스 간 컨테이너 재사용으로 테스트 속도 향상
 */
public class TestcontainersConfig {

    // MongoDB Container (싱글톤)
    public static final MongoDBContainer MONGODB;

    // Kafka Container (싱글톤)
    public static final KafkaContainer KAFKA;

    // Elasticsearch Container (싱글톤)
    public static final ElasticsearchContainer ELASTICSEARCH;

    // Redis Container (싱글톤)
    public static final GenericContainer<?> REDIS;

    static {
        MONGODB = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
                .withReuse(true);

        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
                .withReuse(true);

        ELASTICSEARCH = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0"))
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withReuse(true);

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2"))
                .withExposedPorts(6379)
                .withReuse(true);
    }

    private TestcontainersConfig() {
    }

    /**
     * MongoDB만 필요한 테스트용 Initializer
     */
    public static class MongoInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            MONGODB.start();
            TestPropertyValues.of(
                    "spring.data.mongodb.uri=" + MONGODB.getReplicaSetUrl()
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Kafka만 필요한 테스트용 Initializer
     */
    public static class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            KAFKA.start();
            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers()
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Elasticsearch만 필요한 테스트용 Initializer
     */
    public static class ElasticsearchInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            ELASTICSEARCH.start();
            TestPropertyValues.of(
                    "spring.elasticsearch.uris=" + ELASTICSEARCH.getHttpHostAddress()
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Redis만 필요한 테스트용 Initializer
     */
    public static class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            REDIS.start();
            TestPropertyValues.of(
                    "spring.data.redis.host=" + REDIS.getHost(),
                    "spring.data.redis.port=" + REDIS.getMappedPort(6379)
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Elasticsearch + Redis 조합 Initializer (serving-module용)
     */
    public static class ElasticsearchRedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            ELASTICSEARCH.start();
            REDIS.start();

            TestPropertyValues.of(
                    "spring.elasticsearch.uris=" + ELASTICSEARCH.getHttpHostAddress(),
                    "spring.data.redis.host=" + REDIS.getHost(),
                    "spring.data.redis.port=" + REDIS.getMappedPort(6379)
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Kafka + Elasticsearch + Redis 조합 Initializer (indexing-module용)
     */
    public static class KafkaElasticsearchRedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            KAFKA.start();
            ELASTICSEARCH.start();
            REDIS.start();

            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                    "spring.elasticsearch.uris=" + ELASTICSEARCH.getHttpHostAddress(),
                    "spring.data.redis.host=" + REDIS.getHost(),
                    "spring.data.redis.port=" + REDIS.getMappedPort(6379)
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * 모든 컨테이너가 필요한 테스트용 Initializer
     */
    public static class AllContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            MONGODB.start();
            KAFKA.start();
            ELASTICSEARCH.start();
            REDIS.start();

            TestPropertyValues.of(
                    "spring.data.mongodb.uri=" + MONGODB.getReplicaSetUrl(),
                    "spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                    "spring.elasticsearch.uris=" + ELASTICSEARCH.getHttpHostAddress(),
                    "spring.data.redis.host=" + REDIS.getHost(),
                    "spring.data.redis.port=" + REDIS.getMappedPort(6379)
            ).applyTo(context.getEnvironment());
        }
    }
}

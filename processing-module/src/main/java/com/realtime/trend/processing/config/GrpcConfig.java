package com.realtime.trend.processing.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 채널 설정
 * 다중 NER 서버에 대한 로드밸런싱 지원
 */
@Configuration
public class GrpcConfig {

    private static final Logger log = LoggerFactory.getLogger(GrpcConfig.class);

    @Value("${ner.grpc.host:#{null}}")
    private String nerHost;

    @Value("${ner.grpc.port:50051}")
    private int nerPort;

    @Value("${ner.grpc.hosts:#{null}}")
    private String nerHosts;

    private final List<ManagedChannel> channels = new ArrayList<>();

    @Bean
    public List<ManagedChannel> nerChannels() {
        if (nerHosts != null && !nerHosts.isBlank()) {
            // 다중 호스트 모드 (로드밸런싱)
            String[] hostPorts = nerHosts.split(",");
            for (String hostPort : hostPorts) {
                String[] parts = hostPort.trim().split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 50051;

                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(host, port)
                        .usePlaintext()
                        .build();
                channels.add(channel);
                log.info("NER 채널 생성: {}:{}", host, port);
            }
        } else if (nerHost != null) {
            // 단일 호스트 모드 (기존 호환성)
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(nerHost, nerPort)
                    .usePlaintext()
                    .build();
            channels.add(channel);
            log.info("NER 채널 생성 (단일): {}:{}", nerHost, nerPort);
        }

        log.info("총 {} 개의 NER 채널 생성 완료", channels.size());
        return channels;
    }

    @PreDestroy
    public void shutdown() {
        for (ManagedChannel channel : channels) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("모든 NER 채널 종료 완료");
    }
}

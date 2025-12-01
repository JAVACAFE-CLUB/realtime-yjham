package com.realtime.trend.processing.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ner.NERServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcConfig {

    @Value("${ner.grpc.host}")
    private String nerHost;

    @Value("${ner.grpc.port}")
    private int nerPort;

    private ManagedChannel channel;

    @Bean
    public ManagedChannel nerChannel() {
        channel = ManagedChannelBuilder
                .forAddress(nerHost, nerPort)
                .usePlaintext()
                .build();
        return channel;
    }

    @Bean
    public NERServiceGrpc.NERServiceBlockingStub nerServiceStub(ManagedChannel nerChannel) {
        return NERServiceGrpc.newBlockingStub(nerChannel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

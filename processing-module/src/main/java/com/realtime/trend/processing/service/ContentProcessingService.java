package com.realtime.trend.processing.service;

import com.realtime.trend.processing.dto.*;
import com.realtime.trend.processing.grpc.NerGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 처리 서비스
 */
@Service
public class ContentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ContentProcessingService.class);

    private final TextPreprocessor textPreprocessor;
    private final QualityValidator qualityValidator;
    private final NerGrpcClient nerGrpcClient;

    public ContentProcessingService(
            TextPreprocessor textPreprocessor,
            QualityValidator qualityValidator,
            NerGrpcClient nerGrpcClient
    ) {
        this.textPreprocessor = textPreprocessor;
        this.qualityValidator = qualityValidator;
        this.nerGrpcClient = nerGrpcClient;
    }

    public Optional<ProcessedNewsMessage> processNews(RawNewsMessage raw) {
        log.debug("뉴스 처리 시작: {}", raw.id());

        // 텍스트 전처리
        String processedContent = textPreprocessor.preprocess(raw.content());

        // 품질 검증
        QualityValidator.ValidationResult validationResult = qualityValidator.validate(processedContent);
        if (!validationResult.valid()) {
            log.info("뉴스 품질 검증 실패: {} - {}", raw.id(), validationResult.reason());
            return Optional.empty();
        }

        // NER 수행 (제목 + 본문)
        String textForNer = raw.title() + " " + processedContent;
        List<ExtractedEntity> keywords = nerGrpcClient.analyze(textForNer);

        log.debug("뉴스 처리 완료: {} - 키워드 {}개 추출", raw.id(), keywords.size());

        return Optional.of(new ProcessedNewsMessage(
                raw.id(),
                raw.url(),
                raw.title(),
                processedContent,
                raw.publishedAt(),
                raw.publisher(),
                raw.author(),
                raw.category(),
                raw.tags(),
                raw.collectedAt(),
                keywords
        ));
    }

    public Optional<ProcessedYoutubeMessage> processYoutube(RawYoutubeMessage raw) {
        log.debug("YouTube 처리 시작: {}", raw.videoId());

        // 텍스트 전처리
        String processedDescription = textPreprocessor.preprocess(raw.description());

        // 품질 검증 (YouTube는 설명이 짧을 수 있으므로 제목+설명으로 검증)
        String combinedText = raw.title() + " " + processedDescription;
        QualityValidator.ValidationResult validationResult = qualityValidator.validate(combinedText);
        if (!validationResult.valid()) {
            log.info("YouTube 품질 검증 실패: {} - {}", raw.videoId(), validationResult.reason());
            return Optional.empty();
        }

        // NER 수행 (제목 + 설명)
        List<ExtractedEntity> keywords = nerGrpcClient.analyze(combinedText);

        log.debug("YouTube 처리 완료: {} - 키워드 {}개 추출", raw.videoId(), keywords.size());

        return Optional.of(new ProcessedYoutubeMessage(
                raw.id(),
                raw.videoId(),
                raw.title(),
                processedDescription,
                raw.channelTitle(),
                raw.publishedAt(),
                raw.categoryId(),
                raw.tags(),
                raw.viewCount(),
                raw.likeCount(),
                raw.commentCount(),
                raw.collectedAt(),
                keywords
        ));
    }
}

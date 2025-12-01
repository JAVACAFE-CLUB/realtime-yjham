package com.realtime.trend.collection.api;

import com.realtime.trend.collection.core.scheduler.DynamicCollectionScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("JobController 단위 테스트")
@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private DynamicCollectionScheduler scheduler;

    private JobController controller;

    @BeforeEach
    void setUp() {
        controller = new JobController(scheduler);
    }

    @Nested
    @DisplayName("getRegisteredSources() 메서드")
    class GetRegisteredSourcesMethod {

        @Test
        @DisplayName("등록된 데이터 소스 목록을 반환해야 한다")
        void getRegisteredSources_shouldReturnSourcesList() {
            // given
            when(scheduler.getRegisteredSources()).thenReturn(List.of("news", "youtube"));

            // when
            Map<String, Object> response = controller.getRegisteredSources();

            // then
            assertThat(response).containsKey("sources");
            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) response.get("sources");
            assertThat(sources).containsExactly("news", "youtube");
        }

        @Test
        @DisplayName("등록된 소스가 없으면 빈 목록을 반환해야 한다")
        void getRegisteredSources_withNoSources_shouldReturnEmptyList() {
            // given
            when(scheduler.getRegisteredSources()).thenReturn(List.of());

            // when
            Map<String, Object> response = controller.getRegisteredSources();

            // then
            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) response.get("sources");
            assertThat(sources).isEmpty();
        }
    }

    @Nested
    @DisplayName("runNewsCollection() 메서드")
    class RunNewsCollectionMethod {

        @Test
        @DisplayName("뉴스 수집 Job을 실행하고 성공 응답을 반환해야 한다")
        void runNewsCollection_shouldRunJobAndReturnSuccess() {
            // given
            doNothing().when(scheduler).runCollectionJob("news");

            // when
            Map<String, Object> response = controller.runNewsCollection();

            // then
            assertThat(response.get("success")).isEqualTo(true);
            assertThat(response.get("message")).asString().contains("news");
            verify(scheduler).runCollectionJob("news");
        }

        @Test
        @DisplayName("Job 실행 실패 시 실패 응답을 반환해야 한다")
        void runNewsCollection_withException_shouldReturnFailure() {
            // given
            doThrow(new RuntimeException("Job failed")).when(scheduler).runCollectionJob("news");

            // when
            Map<String, Object> response = controller.runNewsCollection();

            // then
            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).asString().contains("실패");
        }
    }

    @Nested
    @DisplayName("runYoutubeCollection() 메서드")
    class RunYoutubeCollectionMethod {

        @Test
        @DisplayName("YouTube 수집 Job을 실행하고 성공 응답을 반환해야 한다")
        void runYoutubeCollection_shouldRunJobAndReturnSuccess() {
            // given
            doNothing().when(scheduler).runCollectionJob("youtube");

            // when
            Map<String, Object> response = controller.runYoutubeCollection();

            // then
            assertThat(response.get("success")).isEqualTo(true);
            verify(scheduler).runCollectionJob("youtube");
        }
    }

    @Nested
    @DisplayName("runCollectionJob() 메서드")
    class RunCollectionJobMethod {

        @Test
        @DisplayName("지정된 소스의 수집 Job을 실행해야 한다")
        void runCollectionJob_shouldRunSpecifiedJob() {
            // given
            String sourceName = "custom-source";
            doNothing().when(scheduler).runCollectionJob(sourceName);

            // when
            Map<String, Object> response = controller.runCollectionJob(sourceName);

            // then
            assertThat(response.get("success")).isEqualTo(true);
            verify(scheduler).runCollectionJob(sourceName);
        }

        @Test
        @DisplayName("존재하지 않는 소스명이면 실패 응답을 반환해야 한다")
        void runCollectionJob_withInvalidSourceName_shouldReturnFailure() {
            // given
            String sourceName = "invalid-source";
            doThrow(new IllegalArgumentException("존재하지 않는 데이터 소스: " + sourceName))
                    .when(scheduler).runCollectionJob(sourceName);

            // when
            Map<String, Object> response = controller.runCollectionJob(sourceName);

            // then
            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).asString().contains("실패");
        }
    }

    @Nested
    @DisplayName("runNewsCompensatingTransaction() 메서드")
    class RunNewsCompensatingTransactionMethod {

        @Test
        @DisplayName("뉴스 보상 트랜잭션 Job을 실행하고 성공 응답을 반환해야 한다")
        void runNewsCompensatingTransaction_shouldRunJobAndReturnSuccess() {
            // given
            doNothing().when(scheduler).runCompensatingJob("news");

            // when
            Map<String, Object> response = controller.runNewsCompensatingTransaction();

            // then
            assertThat(response.get("success")).isEqualTo(true);
            verify(scheduler).runCompensatingJob("news");
        }
    }

    @Nested
    @DisplayName("runYoutubeCompensatingTransaction() 메서드")
    class RunYoutubeCompensatingTransactionMethod {

        @Test
        @DisplayName("YouTube 보상 트랜잭션 Job을 실행하고 성공 응답을 반환해야 한다")
        void runYoutubeCompensatingTransaction_shouldRunJobAndReturnSuccess() {
            // given
            doNothing().when(scheduler).runCompensatingJob("youtube");

            // when
            Map<String, Object> response = controller.runYoutubeCompensatingTransaction();

            // then
            assertThat(response.get("success")).isEqualTo(true);
            verify(scheduler).runCompensatingJob("youtube");
        }
    }

    @Nested
    @DisplayName("runCompensatingJob() 메서드")
    class RunCompensatingJobMethod {

        @Test
        @DisplayName("지정된 소스의 보상 트랜잭션 Job을 실행해야 한다")
        void runCompensatingJob_shouldRunSpecifiedJob() {
            // given
            String sourceName = "custom-source";
            doNothing().when(scheduler).runCompensatingJob(sourceName);

            // when
            Map<String, Object> response = controller.runCompensatingJob(sourceName);

            // then
            assertThat(response.get("success")).isEqualTo(true);
            verify(scheduler).runCompensatingJob(sourceName);
        }

        @Test
        @DisplayName("Job 실행 실패 시 실패 응답을 반환해야 한다")
        void runCompensatingJob_withException_shouldReturnFailure() {
            // given
            String sourceName = "failing-source";
            doThrow(new RuntimeException("Job failed")).when(scheduler).runCompensatingJob(sourceName);

            // when
            Map<String, Object> response = controller.runCompensatingJob(sourceName);

            // then
            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).asString().contains("실패");
        }

        @Test
        @DisplayName("존재하지 않는 소스명이면 실패 응답을 반환해야 한다")
        void runCompensatingJob_withInvalidSourceName_shouldReturnFailure() {
            // given
            String sourceName = "invalid-source";
            doThrow(new IllegalArgumentException("존재하지 않는 데이터 소스: " + sourceName))
                    .when(scheduler).runCompensatingJob(sourceName);

            // when
            Map<String, Object> response = controller.runCompensatingJob(sourceName);

            // then
            assertThat(response.get("success")).isEqualTo(false);
            assertThat(response.get("message")).asString().contains("실패");
        }
    }
}

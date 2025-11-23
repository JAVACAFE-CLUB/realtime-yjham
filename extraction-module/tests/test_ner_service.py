"""
NERAnalyzer 단위 테스트
"""
import pytest
from unittest.mock import Mock, patch, MagicMock

from config import NERConfig
from ner_service import NERAnalyzer


class TestNERAnalyzerValidation:
    """키워드 유효성 검사 테스트"""
    
    @pytest.fixture
    def analyzer_with_mock_model(self, ner_config):
        """모델 로드 없이 analyzer 생성"""
        with patch.object(NERAnalyzer, '__init__', lambda self, config=None: None):
            analyzer = NERAnalyzer()
            analyzer.config = ner_config
            analyzer.model = Mock()
            return analyzer
    
    def test_empty_keyword_is_invalid(self, analyzer_with_mock_model):
        """빈 키워드는 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("", "PERSON") is False
        assert analyzer_with_mock_model._is_valid_keyword("   ", "PERSON") is False
        assert analyzer_with_mock_model._is_valid_keyword(None, "PERSON") is False
    
    def test_short_keyword_is_invalid(self, analyzer_with_mock_model):
        """최소 길이 미만 키워드는 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("김", "PERSON") is False
        assert analyzer_with_mock_model._is_valid_keyword("A", "LOCATION") is False
    
    def test_single_alphabet_is_invalid(self, analyzer_with_mock_model):
        """단일 알파벳은 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("A", "PERSON") is False
        assert analyzer_with_mock_model._is_valid_keyword("Z", "ORGANIZATION") is False
    
    def test_digits_only_is_invalid(self, analyzer_with_mock_model):
        """숫자로만 구성된 키워드는 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("123", "LOCATION") is False
        assert analyzer_with_mock_model._is_valid_keyword("2024", "ORGANIZATION") is False
    
    def test_single_surname_for_person_is_invalid(self, analyzer_with_mock_model):
        """PERSON 타입의 단일 성씨는 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("이", "PERSON") is False
        assert analyzer_with_mock_model._is_valid_keyword("김", "PERSON") is False
        # 다른 타입에서는 유효
        assert analyzer_with_mock_model._is_valid_keyword("이순신", "PERSON") is True
    
    def test_special_chars_only_is_invalid(self, analyzer_with_mock_model):
        """특수문자로만 구성된 키워드는 유효하지 않음"""
        assert analyzer_with_mock_model._is_valid_keyword("!@#$", "LOCATION") is False
        assert analyzer_with_mock_model._is_valid_keyword("...", "ORGANIZATION") is False
    
    def test_valid_keywords(self, analyzer_with_mock_model):
        """유효한 키워드들"""
        assert analyzer_with_mock_model._is_valid_keyword("삼성전자", "ORGANIZATION") is True
        assert analyzer_with_mock_model._is_valid_keyword("서울", "LOCATION") is True
        assert analyzer_with_mock_model._is_valid_keyword("이재용", "PERSON") is True
        assert analyzer_with_mock_model._is_valid_keyword("Google", "ORGANIZATION") is True


class TestNERAnalyzerAnalyze:
    """텍스트 분석 테스트"""
    
    @pytest.fixture
    def analyzer_with_mock_model(self, ner_config):
        """모델 로드 없이 analyzer 생성"""
        with patch.object(NERAnalyzer, '__init__', lambda self, config=None: None):
            analyzer = NERAnalyzer()
            analyzer.config = ner_config
            analyzer.model = Mock()
            return analyzer
    
    def test_analyze_empty_text_returns_empty(self, analyzer_with_mock_model):
        """빈 텍스트 분석 시 빈 리스트 반환"""
        result = analyzer_with_mock_model.analyze("")
        assert result == []
        
        result = analyzer_with_mock_model.analyze("   ")
        assert result == []
    
    def test_analyze_extracts_entities(self, analyzer_with_mock_model):
        """텍스트에서 개체명 추출"""
        # 모델 응답 모킹
        analyzer_with_mock_model.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "서울", "label": "LOCATION"},
            {"text": "이재용", "label": "PERSON"},
        ]
        
        result = analyzer_with_mock_model.analyze("삼성전자가 서울에서 발표했다. 이재용 회장이 참석.")
        
        assert len(result) == 3
        assert {"keyword": "삼성전자", "type": "ORGANIZATION"} in result
        assert {"keyword": "서울", "type": "LOCATION"} in result
        assert {"keyword": "이재용", "type": "PERSON"} in result
    
    def test_analyze_filters_invalid_entities(self, analyzer_with_mock_model):
        """유효하지 않은 개체명은 필터링"""
        analyzer_with_mock_model.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "김", "label": "PERSON"},  # 단일 성씨 - 필터링됨
            {"text": "A", "label": "LOCATION"},  # 단일 알파벳 - 필터링됨
            {"text": "", "label": "PERSON"},  # 빈 문자열 - 필터링됨
        ]
        
        result = analyzer_with_mock_model.analyze("테스트 텍스트")
        
        assert len(result) == 1
        assert result[0]["keyword"] == "삼성전자"
    
    def test_analyze_filters_unknown_labels(self, analyzer_with_mock_model):
        """알 수 없는 레이블은 필터링"""
        analyzer_with_mock_model.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "무언가", "label": "UNKNOWN"},  # 알 수 없는 레이블
        ]
        
        result = analyzer_with_mock_model.analyze("테스트 텍스트")
        
        assert len(result) == 1
        assert result[0]["keyword"] == "삼성전자"
    
    def test_analyze_handles_exception(self, analyzer_with_mock_model):
        """예외 발생 시 빈 리스트 반환"""
        analyzer_with_mock_model.model.predict_entities.side_effect = Exception("모델 오류")
        
        result = analyzer_with_mock_model.analyze("테스트 텍스트")
        
        assert result == []


class TestNERAnalyzerBatch:
    """배치 분석 테스트"""
    
    @pytest.fixture
    def analyzer_with_mock_model(self, ner_config):
        """모델 로드 없이 analyzer 생성"""
        with patch.object(NERAnalyzer, '__init__', lambda self, config=None: None):
            analyzer = NERAnalyzer()
            analyzer.config = ner_config
            analyzer.model = Mock()
            return analyzer
    
    def test_analyze_batch_processes_all_texts(self, analyzer_with_mock_model):
        """배치 분석 시 모든 텍스트 처리"""
        # 각 텍스트에 대해 다른 결과 반환
        analyzer_with_mock_model.model.predict_entities.side_effect = [
            [{"text": "삼성전자", "label": "ORGANIZATION"}],
            [{"text": "손흥민", "label": "PERSON"}],
            [{"text": "네이버", "label": "ORGANIZATION"}],
        ]
        
        texts = ["텍스트1", "텍스트2", "텍스트3"]
        results = analyzer_with_mock_model.analyze_batch(texts)
        
        assert len(results) == 3
        assert results[0][0]["keyword"] == "삼성전자"
        assert results[1][0]["keyword"] == "손흥민"
        assert results[2][0]["keyword"] == "네이버"
    
    def test_analyze_batch_empty_list(self, analyzer_with_mock_model):
        """빈 리스트 배치 분석"""
        results = analyzer_with_mock_model.analyze_batch([])
        assert results == []


class TestNERConfig:
    """설정 테스트"""
    
    def test_default_config_values(self):
        """기본 설정값 확인"""
        config = NERConfig()
        
        assert config.model_name == "taeminlee/gliner_ko"
        assert "PERSON" in config.entity_labels
        assert "LOCATION" in config.entity_labels
        assert "ORGANIZATION" in config.entity_labels
        assert config.min_keyword_length == 2
        assert "김" in config.single_surnames
    
    def test_custom_config_values(self):
        """커스텀 설정값 적용"""
        config = NERConfig(
            model_name="custom/model",
            entity_labels=["PERSON"],
            min_keyword_length=3,
            single_surnames={"테스트"}
        )
        
        assert config.model_name == "custom/model"
        assert config.entity_labels == ["PERSON"]
        assert config.min_keyword_length == 3
        assert config.single_surnames == {"테스트"}


class TestNERAnalyzerIntegration:
    """통합 테스트 (실제 모델 로드 필요 - CI에서는 skip)"""
    
    @pytest.mark.skip(reason="실제 모델 로드 필요 - 로컬 테스트용")
    def test_real_model_analyze(self):
        """실제 모델로 분석 테스트"""
        analyzer = NERAnalyzer()
        
        result = analyzer.analyze("삼성전자가 서울에서 신제품을 발표했다.")
        
        assert len(result) > 0
        keywords = [r["keyword"] for r in result]
        assert "삼성전자" in keywords or "서울" in keywords

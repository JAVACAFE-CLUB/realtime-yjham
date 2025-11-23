"""
NERAnalyzer 단위 테스트
"""
import pytest
from unittest.mock import Mock, MagicMock, patch

from service.validator import KeywordValidator
from domain import Entity, NERResult


class TestNERAnalyzerAnalyze:
    """텍스트 분석 테스트"""
    
    @pytest.fixture
    def mock_model(self):
        """모킹된 GLiNER 모델"""
        return Mock()
    
    @pytest.fixture
    def analyzer(self, ner_config, mock_model):
        """테스트용 analyzer (모델 주입)"""
        # GLiNER import를 우회하여 analyzer 생성
        with patch('service.analyzer._load_gliner_model') as mock_load:
            mock_load.return_value = mock_model
            from service.analyzer import NERAnalyzer
            validator = KeywordValidator(config=ner_config)
            return NERAnalyzer(
                config=ner_config,
                validator=validator,
                model=mock_model
            )
    
    def test_analyze_empty_text_returns_empty_result(self, analyzer):
        """빈 텍스트 분석 시 빈 결과 반환"""
        result = analyzer.analyze("")
        assert isinstance(result, NERResult)
        assert result.entities == []
        
        result = analyzer.analyze("   ")
        assert result.entities == []
    
    def test_analyze_extracts_entities(self, analyzer):
        """텍스트에서 개체명 추출"""
        analyzer.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "서울", "label": "LOCATION"},
            {"text": "이재용", "label": "PERSON"},
        ]
        
        result = analyzer.analyze("삼성전자가 서울에서 발표했다. 이재용 회장이 참석.")
        
        assert isinstance(result, NERResult)
        assert result.entity_count == 3
        
        keywords = [e.keyword for e in result.entities]
        assert "삼성전자" in keywords
        assert "서울" in keywords
        assert "이재용" in keywords
    
    def test_analyze_filters_invalid_entities(self, analyzer):
        """유효하지 않은 개체명은 필터링"""
        analyzer.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "김", "label": "PERSON"},  # 단일 성씨 - 필터링됨
            {"text": "A", "label": "LOCATION"},  # 단일 알파벳 - 필터링됨
            {"text": "", "label": "PERSON"},  # 빈 문자열 - 필터링됨
        ]
        
        result = analyzer.analyze("테스트 텍스트")
        
        assert result.entity_count == 1
        assert result.entities[0].keyword == "삼성전자"
    
    def test_analyze_filters_unknown_labels(self, analyzer):
        """알 수 없는 레이블은 필터링"""
        analyzer.model.predict_entities.return_value = [
            {"text": "삼성전자", "label": "ORGANIZATION"},
            {"text": "무언가", "label": "UNKNOWN"},  # 알 수 없는 레이블
        ]
        
        result = analyzer.analyze("테스트 텍스트")
        
        assert result.entity_count == 1
        assert result.entities[0].keyword == "삼성전자"
    
    def test_analyze_handles_exception(self, analyzer):
        """예외 발생 시 빈 결과 반환"""
        analyzer.model.predict_entities.side_effect = Exception("모델 오류")
        
        result = analyzer.analyze("테스트 텍스트")
        
        assert isinstance(result, NERResult)
        assert result.entities == []


class TestNERAnalyzerBatch:
    """배치 분석 테스트"""
    
    @pytest.fixture
    def mock_model(self):
        """모킹된 GLiNER 모델"""
        return Mock()
    
    @pytest.fixture
    def analyzer(self, ner_config, mock_model):
        """테스트용 analyzer (모델 주입)"""
        with patch('service.analyzer._load_gliner_model') as mock_load:
            mock_load.return_value = mock_model
            from service.analyzer import NERAnalyzer
            validator = KeywordValidator(config=ner_config)
            return NERAnalyzer(
                config=ner_config,
                validator=validator,
                model=mock_model
            )
    
    def test_analyze_batch_processes_all_texts(self, analyzer):
        """배치 분석 시 모든 텍스트 처리"""
        analyzer.model.batch_predict_entities.return_value = [
            [{"text": "삼성전자", "label": "ORGANIZATION"}],
            [{"text": "손흥민", "label": "PERSON"}],
            [{"text": "네이버", "label": "ORGANIZATION"}],
        ]
        
        texts = ["텍스트1", "텍스트2", "텍스트3"]
        results = analyzer.analyze_batch(texts)
        
        assert len(results) == 3
        assert results[0].entities[0].keyword == "삼성전자"
        assert results[1].entities[0].keyword == "손흥민"
        assert results[2].entities[0].keyword == "네이버"
    
    def test_analyze_batch_empty_list(self, analyzer):
        """빈 리스트 배치 분석"""
        results = analyzer.analyze_batch([])
        assert results == []
    
    def test_analyze_batch_handles_empty_texts(self, analyzer):
        """빈 텍스트가 포함된 배치 처리"""
        analyzer.model.batch_predict_entities.return_value = [
            [{"text": "삼성전자", "label": "ORGANIZATION"}],
        ]
        
        texts = ["", "텍스트1", "   "]
        results = analyzer.analyze_batch(texts)
        
        assert len(results) == 3
        assert results[0].entities == []  # 빈 텍스트
        assert results[1].entities[0].keyword == "삼성전자"
        assert results[2].entities == []  # 공백 텍스트
    
    def test_analyze_batch_fallback_on_error(self, analyzer):
        """배치 실패 시 개별 분석으로 폴백"""
        # batch_predict_entities 실패
        analyzer.model.batch_predict_entities.side_effect = Exception("배치 오류")
        # 개별 predict_entities는 성공
        analyzer.model.predict_entities.return_value = [
            {"text": "테스트", "label": "ORGANIZATION"}
        ]
        
        texts = ["텍스트1", "텍스트2"]
        results = analyzer.analyze_batch(texts)
        
        assert len(results) == 2
        # 폴백으로 개별 분석 수행됨
        assert analyzer.model.predict_entities.call_count == 2

"""
gRPC Servicer 테스트
"""
import pytest
from unittest.mock import Mock, MagicMock, patch

from domain import Entity, NERResult


class TestNERServiceServicer:
    """NERServiceServicer 테스트"""
    
    @pytest.fixture
    def mock_analyzer(self):
        """모킹된 NERAnalyzer"""
        return Mock()
    
    @pytest.fixture
    def servicer(self, mock_analyzer):
        """테스트용 servicer"""
        with patch('rpc.servicer.NERAnalyzer') as MockAnalyzer:
            MockAnalyzer.return_value = mock_analyzer
            from rpc.servicer import NERServiceServicer
            return NERServiceServicer(analyzer=mock_analyzer)
    
    @pytest.fixture
    def mock_context(self):
        """모킹된 gRPC context"""
        context = Mock()
        return context
    
    def test_analyze_success(self, servicer, mock_analyzer, mock_context):
        """단일 분석 성공"""
        # Mock 설정
        mock_analyzer.analyze.return_value = NERResult(
            text="테스트",
            entities=[
                Entity(keyword="삼성전자", type="ORGANIZATION"),
                Entity(keyword="서울", type="LOCATION"),
            ]
        )
        
        # Mock request
        request = Mock()
        request.text = "삼성전자가 서울에서 발표했다"
        
        # 실행
        response = servicer.Analyze(request, mock_context)
        
        # 검증
        assert len(response.entities) == 2
        mock_analyzer.analyze.assert_called_once_with("삼성전자가 서울에서 발표했다")
    
    def test_analyze_error(self, servicer, mock_analyzer, mock_context):
        """단일 분석 에러 처리"""
        mock_analyzer.analyze.side_effect = Exception("분석 오류")
        
        request = Mock()
        request.text = "테스트"
        
        response = servicer.Analyze(request, mock_context)
        
        # 에러 상태 설정 확인
        mock_context.set_code.assert_called()
        mock_context.set_details.assert_called()
    
    def test_analyze_batch_success(self, servicer, mock_analyzer, mock_context):
        """배치 분석 성공"""
        mock_analyzer.analyze_batch.return_value = [
            NERResult(text="텍스트1", entities=[Entity(keyword="삼성전자", type="ORGANIZATION")]),
            NERResult(text="텍스트2", entities=[Entity(keyword="네이버", type="ORGANIZATION")]),
        ]
        
        request = Mock()
        request.texts = ["텍스트1", "텍스트2"]
        
        response = servicer.AnalyzeBatch(request, mock_context)
        
        assert len(response.responses) == 2
        mock_analyzer.analyze_batch.assert_called_once()
    
    def test_analyze_batch_error(self, servicer, mock_analyzer, mock_context):
        """배치 분석 에러 처리"""
        mock_analyzer.analyze_batch.side_effect = Exception("배치 오류")
        
        request = Mock()
        request.texts = ["텍스트1"]
        
        response = servicer.AnalyzeBatch(request, mock_context)
        
        mock_context.set_code.assert_called()
        mock_context.set_details.assert_called()

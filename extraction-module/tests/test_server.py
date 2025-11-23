"""
gRPC 서버 테스트
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
import grpc


class TestNERServiceServicer:
    """NERServiceServicer 단위 테스트"""
    
    @pytest.fixture
    def mock_analyzer(self):
        """모킹된 NERAnalyzer"""
        analyzer = Mock()
        analyzer.analyze.return_value = [
            {"keyword": "삼성전자", "type": "ORGANIZATION"},
            {"keyword": "서울", "type": "LOCATION"},
        ]
        analyzer.analyze_batch.return_value = [
            [{"keyword": "삼성전자", "type": "ORGANIZATION"}],
            [{"keyword": "손흥민", "type": "PERSON"}],
        ]
        return analyzer
    
    @pytest.fixture
    def servicer(self, mock_analyzer):
        """테스트용 servicer"""
        with patch('server.NERAnalyzer', return_value=mock_analyzer):
            # server 모듈 동적 임포트 (path 설정 후)
            import server
            servicer = server.NERServiceServicer()
            servicer.analyzer = mock_analyzer
            return servicer
    
    @pytest.fixture
    def mock_context(self):
        """모킹된 gRPC context"""
        context = Mock()
        return context
    
    def test_analyze_returns_entities(self, servicer, mock_context):
        """Analyze 메서드가 개체명 반환"""
        # ner_pb2 모킹
        import ner_pb2
        
        request = ner_pb2.TextRequest(text="삼성전자가 서울에서 발표했다.")
        
        response = servicer.Analyze(request, mock_context)
        
        assert len(response.entities) == 2
        keywords = [e.keyword for e in response.entities]
        assert "삼성전자" in keywords
        assert "서울" in keywords
    
    def test_analyze_handles_empty_text(self, servicer, mock_context, mock_analyzer):
        """빈 텍스트 처리"""
        import ner_pb2
        
        mock_analyzer.analyze.return_value = []
        request = ner_pb2.TextRequest(text="")
        
        response = servicer.Analyze(request, mock_context)
        
        assert len(response.entities) == 0
    
    def test_analyze_handles_exception(self, servicer, mock_context, mock_analyzer):
        """예외 발생 시 에러 처리"""
        import ner_pb2
        
        mock_analyzer.analyze.side_effect = Exception("모델 오류")
        request = ner_pb2.TextRequest(text="테스트")
        
        response = servicer.Analyze(request, mock_context)
        
        mock_context.set_code.assert_called_once_with(grpc.StatusCode.INTERNAL)
        mock_context.set_details.assert_called_once()
    
    def test_analyze_batch_returns_batch_response(self, servicer, mock_context):
        """AnalyzeBatch 메서드가 배치 응답 반환"""
        import ner_pb2
        
        request = ner_pb2.BatchTextRequest(texts=["텍스트1", "텍스트2"])
        
        response = servicer.AnalyzeBatch(request, mock_context)
        
        assert len(response.responses) == 2
        assert response.responses[0].entities[0].keyword == "삼성전자"
        assert response.responses[1].entities[0].keyword == "손흥민"
    
    def test_analyze_batch_handles_exception(self, servicer, mock_context, mock_analyzer):
        """배치 분석 예외 처리"""
        import ner_pb2
        
        mock_analyzer.analyze_batch.side_effect = Exception("배치 오류")
        request = ner_pb2.BatchTextRequest(texts=["텍스트1", "텍스트2"])
        
        response = servicer.AnalyzeBatch(request, mock_context)
        
        mock_context.set_code.assert_called_once_with(grpc.StatusCode.INTERNAL)


class TestServerConfig:
    """서버 설정 테스트"""
    
    def test_default_server_config(self):
        """기본 서버 설정값"""
        from config import ServerConfig
        
        config = ServerConfig()
        
        assert config.port == 50051
        assert config.max_workers == 10
        assert config.log_level == "INFO"
    
    def test_custom_server_config(self):
        """커스텀 서버 설정"""
        from config import ServerConfig
        
        config = ServerConfig(
            port=8080,
            max_workers=20,
            log_level="DEBUG"
        )
        
        assert config.port == 8080
        assert config.max_workers == 20
        assert config.log_level == "DEBUG"

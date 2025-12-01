"""
설정 테스트
"""
import pytest

from config import NERConfig, ServerConfig


class TestNERConfig:
    """NER 설정 테스트"""
    
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
    
    def test_config_immutable(self):
        """설정은 불변"""
        config = NERConfig()
        
        with pytest.raises(AttributeError):
            config.model_name = "new/model"


class TestServerConfig:
    """서버 설정 테스트"""
    
    def test_default_server_config(self):
        """기본 서버 설정값"""
        config = ServerConfig()
        
        assert config.port == 50051
        assert config.max_workers == 10
        assert config.log_level == "INFO"
    
    def test_custom_server_config(self):
        """커스텀 서버 설정값"""
        config = ServerConfig(
            port=50052,
            max_workers=20,
            log_level="DEBUG"
        )
        
        assert config.port == 50052
        assert config.max_workers == 20
        assert config.log_level == "DEBUG"

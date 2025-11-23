"""
도메인 엔티티 테스트
"""
import pytest

from domain import Entity, NERResult


class TestEntity:
    """Entity 테스트"""
    
    def test_entity_creation(self):
        """Entity 생성"""
        entity = Entity(keyword="삼성전자", type="ORGANIZATION")
        
        assert entity.keyword == "삼성전자"
        assert entity.type == "ORGANIZATION"
    
    def test_entity_immutable(self):
        """Entity는 불변"""
        entity = Entity(keyword="삼성전자", type="ORGANIZATION")
        
        with pytest.raises(AttributeError):
            entity.keyword = "네이버"
    
    def test_entity_to_dict(self):
        """Entity를 딕셔너리로 변환"""
        entity = Entity(keyword="삼성전자", type="ORGANIZATION")
        
        result = entity.to_dict()
        
        assert result == {"keyword": "삼성전자", "type": "ORGANIZATION"}


class TestNERResult:
    """NERResult 테스트"""
    
    def test_ner_result_creation(self):
        """NERResult 생성"""
        entities = [
            Entity(keyword="삼성전자", type="ORGANIZATION"),
            Entity(keyword="서울", type="LOCATION"),
        ]
        result = NERResult(text="테스트 텍스트", entities=entities)
        
        assert result.text == "테스트 텍스트"
        assert len(result.entities) == 2
    
    def test_ner_result_entity_count(self):
        """entity_count 프로퍼티"""
        entities = [
            Entity(keyword="삼성전자", type="ORGANIZATION"),
            Entity(keyword="서울", type="LOCATION"),
            Entity(keyword="이재용", type="PERSON"),
        ]
        result = NERResult(text="테스트", entities=entities)
        
        assert result.entity_count == 3
    
    def test_ner_result_empty_entities(self):
        """빈 개체명 리스트"""
        result = NERResult(text="테스트")
        
        assert result.entities == []
        assert result.entity_count == 0
    
    def test_ner_result_to_dict_list(self):
        """개체명 리스트를 딕셔너리 리스트로 변환"""
        entities = [
            Entity(keyword="삼성전자", type="ORGANIZATION"),
            Entity(keyword="서울", type="LOCATION"),
        ]
        result = NERResult(text="테스트", entities=entities)
        
        dict_list = result.to_dict_list()
        
        assert len(dict_list) == 2
        assert {"keyword": "삼성전자", "type": "ORGANIZATION"} in dict_list
        assert {"keyword": "서울", "type": "LOCATION"} in dict_list

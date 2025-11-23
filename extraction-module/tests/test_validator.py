"""
KeywordValidator 단위 테스트
"""
import pytest

from service.validator import KeywordValidator


class TestKeywordValidator:
    """키워드 유효성 검사 테스트"""
    
    @pytest.fixture
    def validator(self, ner_config):
        """테스트용 validator 생성"""
        return KeywordValidator(config=ner_config)
    
    def test_empty_keyword_is_invalid(self, validator):
        """빈 키워드는 유효하지 않음"""
        assert validator.is_valid("", "PERSON") is False
        assert validator.is_valid("   ", "PERSON") is False
        assert validator.is_valid(None, "PERSON") is False
    
    def test_short_keyword_is_invalid(self, validator):
        """최소 길이 미만 키워드는 유효하지 않음"""
        assert validator.is_valid("김", "PERSON") is False
        assert validator.is_valid("A", "LOCATION") is False
    
    def test_single_alphabet_is_invalid(self, validator):
        """단일 알파벳은 유효하지 않음"""
        assert validator.is_valid("A", "PERSON") is False
        assert validator.is_valid("Z", "ORGANIZATION") is False
    
    def test_digits_only_is_invalid(self, validator):
        """숫자로만 구성된 키워드는 유효하지 않음"""
        assert validator.is_valid("123", "LOCATION") is False
        assert validator.is_valid("2024", "ORGANIZATION") is False
    
    def test_single_surname_for_person_is_invalid(self, validator):
        """PERSON 타입의 단일 성씨는 유효하지 않음"""
        assert validator.is_valid("이", "PERSON") is False
        assert validator.is_valid("김", "PERSON") is False
        # 다른 타입에서는 유효
        assert validator.is_valid("이순신", "PERSON") is True
    
    def test_special_chars_only_is_invalid(self, validator):
        """특수문자로만 구성된 키워드는 유효하지 않음"""
        assert validator.is_valid("!@#$", "LOCATION") is False
        assert validator.is_valid("...", "ORGANIZATION") is False
    
    def test_valid_keywords(self, validator):
        """유효한 키워드들"""
        assert validator.is_valid("삼성전자", "ORGANIZATION") is True
        assert validator.is_valid("서울", "LOCATION") is True
        assert validator.is_valid("이재용", "PERSON") is True
        assert validator.is_valid("Google", "ORGANIZATION") is True

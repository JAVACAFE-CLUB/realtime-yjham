"""
pytest 설정 및 fixtures
"""
import sys
import os
import pytest

# src 디렉토리를 path에 추가
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src', 'generated'))

from config import NERConfig, reset_config


@pytest.fixture
def ner_config():
    """테스트용 NER 설정"""
    return NERConfig(
        model_name="taeminlee/gliner_ko",
        entity_labels=["PERSON", "LOCATION", "ORGANIZATION"],
        min_keyword_length=2,
        single_surnames={'이', '김', '박', '최', '정'}
    )


@pytest.fixture(autouse=True)
def reset_config_after_test():
    """각 테스트 후 설정 초기화"""
    yield
    reset_config()


@pytest.fixture
def sample_texts():
    """테스트용 샘플 텍스트"""
    return {
        "news": "삼성전자가 서울에서 신제품을 발표했다. 이재용 회장이 참석했다.",
        "sports": "손흥민 선수가 영국 토트넘에서 활약하고 있다.",
        "tech": "구글이 미국 캘리포니아에서 개발자 컨퍼런스를 개최했다.",
        "empty": "",
        "whitespace": "   ",
        "no_entities": "오늘 날씨가 좋습니다.",
        "special_chars": "!@#$%^&*()",
    }

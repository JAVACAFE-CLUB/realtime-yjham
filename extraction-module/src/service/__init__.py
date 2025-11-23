"""
서비스 모듈 - 비즈니스 로직
"""
from .validator import KeywordValidator

# NERAnalyzer는 gliner 의존성이 있어 lazy import
def get_analyzer_class():
    """NERAnalyzer 클래스 반환 (lazy import)"""
    from .analyzer import NERAnalyzer
    return NERAnalyzer

__all__ = [
    "KeywordValidator",
    "get_analyzer_class",
]

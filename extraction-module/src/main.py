"""
NER 서비스 엔트리포인트
"""
import logging
import sys
import os

# 패키지 경로 설정
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'generated'))

from config import get_server_config
from rpc import serve

# 설정 로드
server_config = get_server_config()

# 로깅 설정
logging.basicConfig(
    level=getattr(logging, server_config.log_level.upper(), logging.INFO),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def main():
    """메인 함수"""
    logger.info("NER 서비스 시작")
    serve()


if __name__ == '__main__':
    main()

"""
gRPC 생성 파일 모듈
"""
try:
    from . import ner_pb2
    from . import ner_pb2_grpc
except ImportError:
    # 생성 파일이 없는 경우 (proto 컴파일 필요)
    pass

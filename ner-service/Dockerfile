FROM python:3.9-slim

WORKDIR /app

# 시스템 패키지 업데이트 및 필요한 라이브러리 설치
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY proto ./proto
COPY src ./src

# gRPC 코드 생성
RUN python -m grpc_tools.protoc \
    -I./proto \
    --python_out=./src/generated \
    --grpc_python_out=./src/generated \
    ./proto/ner.proto

# 포트 노출
EXPOSE 50051

# 서버 실행
CMD ["python", "src/server.py"]

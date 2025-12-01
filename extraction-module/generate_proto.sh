#!/bin/bash

# gRPC Python 코드 생성 스크립트

echo "gRPC Python 코드 생성 시작..."

python -m grpc_tools.protoc \
    -I./proto \
    --python_out=./src/generated \
    --grpc_python_out=./src/generated \
    ./proto/ner.proto

echo "gRPC Python 코드 생성 완료!"
echo "생성된 파일:"
echo "  - src/generated/ner_pb2.py"
echo "  - src/generated/ner_pb2_grpc.py"

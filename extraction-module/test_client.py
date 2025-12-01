"""
Extraction Module 테스트 클라이언트
"""
import sys
sys.path.append('./src/generated')

import grpc
import ner_pb2
import ner_pb2_grpc


def test_analyze():
    """
    단일 텍스트 분석 테스트
    """
    print("=" * 60)
    print("단일 텍스트 분석 테스트")
    print("=" * 60)

    # gRPC 채널 생성
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = ner_pb2_grpc.NERServiceStub(channel)

        # 테스트 텍스트
        test_text = "삼성전자가 서울에서 신제품을 발표했다. 이재용 회장이 참석했다."

        # 요청 생성
        request = ner_pb2.TextRequest(text=test_text)

        try:
            # API 호출
            response = stub.Analyze(request)

            # 결과 출력
            print(f"\n입력 텍스트: {test_text}")
            print(f"\n추출된 개체명 ({len(response.entities)}개):")
            for entity in response.entities:
                print(f"  - {entity.keyword} ({entity.type})")

            return True

        except grpc.RpcError as e:
            print(f"오류 발생: {e.code()} - {e.details()}")
            return False


def test_analyze_batch():
    """
    배치 텍스트 분석 테스트
    """
    print("\n" + "=" * 60)
    print("배치 텍스트 분석 테스트")
    print("=" * 60)

    # gRPC 채널 생성
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = ner_pb2_grpc.NERServiceStub(channel)

        # 테스트 텍스트들
        test_texts = [
            "구글이 미국 캘리포니아에서 개발자 컨퍼런스를 개최했다.",
            "손흥민 선수가 영국 토트넘에서 활약하고 있다.",
            "네이버가 한국에서 새로운 AI 서비스를 출시했다."
        ]

        # 요청 생성
        request = ner_pb2.BatchTextRequest(texts=test_texts)

        try:
            # API 호출
            response = stub.AnalyzeBatch(request)

            # 결과 출력
            print(f"\n입력 텍스트 개수: {len(test_texts)}")
            for idx, (text, result) in enumerate(zip(test_texts, response.responses)):
                print(f"\n[{idx + 1}] {text}")
                print(f"  추출된 개체명 ({len(result.entities)}개):")
                for entity in result.entities:
                    print(f"    - {entity.keyword} ({entity.type})")

            return True

        except grpc.RpcError as e:
            print(f"오류 발생: {e.code()} - {e.details()}")
            return False


def main():
    """
    메인 함수
    """
    print("\n🔍 Extraction Module 테스트 시작\n")

    # 연결 테스트
    try:
        with grpc.insecure_channel('localhost:50051') as channel:
            grpc.channel_ready_future(channel).result(timeout=5)
        print("✅ Extraction Module 연결 성공\n")
    except grpc.FutureTimeoutError:
        print("❌ Extraction Module 연결 실패")
        print("서버가 실행 중인지 확인하세요: docker ps | grep extraction-module")
        return

    # 테스트 실행
    test1_result = test_analyze()
    test2_result = test_analyze_batch()

    # 결과 요약
    print("\n" + "=" * 60)
    print("테스트 결과 요약")
    print("=" * 60)
    print(f"단일 텍스트 분석: {'✅ 성공' if test1_result else '❌ 실패'}")
    print(f"배치 텍스트 분석: {'✅ 성공' if test2_result else '❌ 실패'}")

    if test1_result and test2_result:
        print("\n🎉 모든 테스트 통과!")
    else:
        print("\n⚠️  일부 테스트 실패")


if __name__ == '__main__':
    main()

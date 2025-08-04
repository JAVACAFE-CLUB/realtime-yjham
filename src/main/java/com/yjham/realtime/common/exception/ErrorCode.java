package com.yjham.realtime.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(404, "C003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(400, "C005", "잘못된 타입의 값입니다."),
    HANDLE_ACCESS_DENIED(403, "C006", "접근이 거부되었습니다."),
    
    // 멤버 관련 에러
    MEMBER_NOT_FOUND(404, "M001", "존재하지 않는 회원입니다."),
    DUPLICATE_EMAIL(400, "M002", "이미 존재하는 이메일입니다."),
    INVALID_PASSWORD(400, "M003", "잘못된 비밀번호입니다.");
    
    private final int status;
    private final String code;
    private final String message;
} 
package com.sampoom.factory.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorStatus {

    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.",20001),
    MISSING_EMAIL_VERIFICATION_EXCEPTION(HttpStatus.BAD_REQUEST, "이메일 인증을 진행해주세요.",20002),
    ALREADY_REGISTER_EMAIL_EXCEPETION(HttpStatus.BAD_REQUEST, "이미 가입된 이메일 입니다.",20003),


    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", 20101),

    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.",20301),

    // 404 NOT_FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.",20401),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "자재를 찾을 수 없습니다.", 20402),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.", 20403),



    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, "충돌이 발생했습니다.",20901),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.",20501);

    private final HttpStatus httpStatus;
    private final String message;
    private final int code;


    public int getStatusCode() {
        return this.httpStatus.value();
    }

}

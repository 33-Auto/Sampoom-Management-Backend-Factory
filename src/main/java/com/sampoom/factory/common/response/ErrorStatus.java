package com.sampoom.factory.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum ErrorStatus {

    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.",40001),
    MISSING_EMAIL_VERIFICATION_EXCEPTION(HttpStatus.BAD_REQUEST, "이메일 인증을 진행해주세요.",40002),
    ALREADY_REGISTER_EMAIL_EXCEPETION(HttpStatus.BAD_REQUEST, "이미 가입된 이메일 입니다.",40003),
    FACTORY_MATERIAL_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "해당 공장의 자재 주문이 아닙니다.",40004),
    ORDER_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 주문입니다.",40005),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태입니다.",40006),
    ORDER_NOT_IN_PRODUCTION(HttpStatus.BAD_REQUEST, "생산 중인 주문만 완료 처리할 수 있습니다.", 40007),
    ORDER_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중인 주문만 완료 처리할 수 있습니다.", 40014),
    PART_ORDER_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 부품 주문을 찾을 수 없습니다.", 40008),
    INVALID_FACTORY_FOR_PART_ORDER(HttpStatus.BAD_REQUEST, "해당 공장의 부품 주문이 아닙니다.", 40009),
    CANNOT_CANCEL_PROCESSED_ORDER(HttpStatus.BAD_REQUEST, "이미 처리 중인 주문은 취소할 수 없습니다.", 40010),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "유효하지 않은 수량입니다.",40011),
    INSUFFICIENT_MATERIAL_QUANTITY(HttpStatus.BAD_REQUEST, "자재 수량이 부족합니다.",40012),
    NO_AVAILABLE_FACTORY(HttpStatus.BAD_REQUEST, "사용 가능한 공장이 없습니다.",40013),



    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", 40101),

    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.",40301),

    // 404 NOT_FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.",40401),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "자재를 찾을 수 없습니다.", 40402),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.", 40403),
    FACTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "공장을 찾을 수 없습니다.", 40404),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다.", 40405),
    PART_NOT_FOUND(HttpStatus.NOT_FOUND, "부품을 찾을 수 없습니다.", 40406),
    BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "BOM을 찾을 수 없습니다.", 40407),



    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, "충돌이 발생했습니다.",40901),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.",40501);

    private final HttpStatus httpStatus;
    private final String message;
    private final int code;


    public int getStatusCode() {
        return this.httpStatus.value();
    }

}

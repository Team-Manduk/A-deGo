package com.teammanduk.adego.core.designsystem.component

/**
 * TimeValue Extension Functions
 * TimeValue: Int.MIN_VALUE ~ Int.MAX_VALUE (5분 단위)
 * 표시할 때만 % 288로 정규화 (288 = 24시간 * 12칸)
 */

// 분 (0, 5, 10, ..., 55)
internal val Int.minute: Int
    get() = (((this % 288) + 288) % 12) * 5

// 시간 24시간 형식 (0-23)
internal val Int.hour24: Int
    get() = (((this % 288) + 288) % 288) / 12

// 시간 12시간 형식 (0-11)
internal val Int.hour12: Int
    get() = this.hour24 % 12

// 오후 여부
internal val Int.isAfternoon: Boolean
    get() = this.hour24 >= 12

// 오전/오후 텍스트
internal val Int.periodText: String
    get() = if (this.isAfternoon) "오후" else "오전"

// 분 인덱스 (0-11)
internal val Int.minuteIndex: Int
    get() = (((this % 288) + 288) % 288) % 12

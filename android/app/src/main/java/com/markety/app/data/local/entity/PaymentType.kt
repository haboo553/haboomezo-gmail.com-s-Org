package com.markety.app.data.local.entity

enum class PaymentType(val arabicLabel: String) {
    CASH("كاش"),
    CREDIT("آجل")
}

enum class InvoiceStatus(val arabicLabel: String) {
    COMPLETED("مكتملة"),
    CANCELLED("ملغاة")
}

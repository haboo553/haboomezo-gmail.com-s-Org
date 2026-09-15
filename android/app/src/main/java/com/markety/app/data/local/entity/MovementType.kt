package com.markety.app.data.local.entity

enum class MovementType(val arabicLabel: String) {
    INITIAL("إضافة افتتاحية"),
    PURCHASE("شراء بضاعة"),
    SALE("حركة مبيعات"),
    RETURN("مرتجع"),
    DAMAGE("تالف وهالك"),
    ADJUSTMENT("تعديل جرد"),
    INVENTORY("تسوية جردية")
}

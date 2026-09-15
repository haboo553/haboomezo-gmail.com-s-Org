package com.markety.app.data.local

import androidx.room.TypeConverter
import com.markety.app.data.local.entity.CustomerTransactionType
import com.markety.app.data.local.entity.DailyOperationStatus
import com.markety.app.data.local.entity.InvoiceStatus
import com.markety.app.data.local.entity.MovementType
import com.markety.app.data.local.entity.PaymentType
import com.markety.app.data.local.entity.ReturnType
import com.markety.app.data.local.entity.SupplierTransactionType
import com.markety.app.data.local.entity.UserRole

class Converters {
    @TypeConverter
    fun fromSupplierTransactionType(value: SupplierTransactionType?): String? = value?.name

    @TypeConverter
    fun toSupplierTransactionType(value: String?): SupplierTransactionType? = value?.let {
        try { SupplierTransactionType.valueOf(it) } catch (e: Exception) { SupplierTransactionType.PURCHASE }
    }

    @TypeConverter
    fun fromReturnType(value: ReturnType?): String? = value?.name

    @TypeConverter
    fun toReturnType(value: String?): ReturnType? = value?.let {
        try { ReturnType.valueOf(it) } catch (e: Exception) { ReturnType.SALES_RETURN }
    }

    @TypeConverter
    fun fromUserRole(value: UserRole?): String? = value?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let {
        try { UserRole.valueOf(it) } catch (e: Exception) { UserRole.CASHIER }
    }

    @TypeConverter
    fun fromCustomerTransactionType(value: CustomerTransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCustomerTransactionType(value: String?): CustomerTransactionType? {
        return value?.let {
            try {
                CustomerTransactionType.valueOf(it)
            } catch (e: Exception) {
                CustomerTransactionType.SALE
            }
        }
    }

    @TypeConverter
    fun fromDailyOperationStatus(value: DailyOperationStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDailyOperationStatus(value: String?): DailyOperationStatus? {
        return value?.let {
            try {
                DailyOperationStatus.valueOf(it)
            } catch (e: Exception) {
                DailyOperationStatus.OPEN
            }
        }
    }
    @TypeConverter
    fun fromMovementType(value: MovementType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toMovementType(value: String?): MovementType? {
        return value?.let {
            try {
                MovementType.valueOf(it)
            } catch (e: Exception) {
                MovementType.ADJUSTMENT
            }
        }
    }

    @TypeConverter
    fun fromPaymentType(value: PaymentType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPaymentType(value: String?): PaymentType? {
        return value?.let {
            try {
                PaymentType.valueOf(it)
            } catch (e: Exception) {
                PaymentType.CASH
            }
        }
    }

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toInvoiceStatus(value: String?): InvoiceStatus? {
        return value?.let {
            try {
                InvoiceStatus.valueOf(it)
            } catch (e: Exception) {
                InvoiceStatus.COMPLETED
            }
        }
    }
}

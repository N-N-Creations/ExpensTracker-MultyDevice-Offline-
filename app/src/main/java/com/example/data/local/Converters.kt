package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AccountSourceType
import com.example.data.model.RecurringFrequency
import com.example.data.model.ReservedPaymentStatus
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromAccountSourceType(value: AccountSourceType): String = value.name

    @TypeConverter
    fun toAccountSourceType(value: String): AccountSourceType {
        return try {
            AccountSourceType.valueOf(value)
        } catch (e: Exception) {
            AccountSourceType.CASH
        }
    }

    @TypeConverter
    fun fromReservedPaymentStatus(value: ReservedPaymentStatus): String = value.name

    @TypeConverter
    fun toReservedPaymentStatus(value: String): ReservedPaymentStatus {
        return try {
            ReservedPaymentStatus.valueOf(value)
        } catch (e: Exception) {
            ReservedPaymentStatus.PENDING
        }
    }

    @TypeConverter
    fun fromRecurringFrequency(value: RecurringFrequency): String = value.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency {
        return try {
            RecurringFrequency.valueOf(value)
        } catch (e: Exception) {
            RecurringFrequency.ONCE
        }
    }
}

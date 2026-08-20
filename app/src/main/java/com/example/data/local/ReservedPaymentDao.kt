package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReservedPayment
import com.example.data.model.ReservedPaymentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservedPaymentDao {
    @Query("SELECT * FROM reserved_payments WHERE isDeleted = 0 ORDER BY dueDate ASC")
    fun getAllActiveReservedPayments(): Flow<List<ReservedPayment>>

    @Query("SELECT * FROM reserved_payments WHERE isDeleted = 0 AND status = 'PENDING' ORDER BY dueDate ASC")
    fun getPendingReservedPayments(): Flow<List<ReservedPayment>>

    @Query("SELECT * FROM reserved_payments WHERE isDeleted = 0 AND status = 'PENDING' AND dueDate BETWEEN :startTime AND :endTime")
    fun getPendingReservedPaymentsBetweenDates(startTime: Long, endTime: Long): Flow<List<ReservedPayment>>

    @Query("SELECT * FROM reserved_payments WHERE isDeleted = 0 AND dueDate BETWEEN :startTime AND :endTime ORDER BY dueDate ASC")
    fun getReservedPaymentsForPeriod(startTime: Long, endTime: Long): Flow<List<ReservedPayment>>

    @Query("SELECT * FROM reserved_payments WHERE id = :id LIMIT 1")
    suspend fun getReservedPaymentById(id: String): ReservedPayment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(payment: ReservedPayment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(payments: List<ReservedPayment>)

    @Update
    suspend fun update(payment: ReservedPayment)

    @Query("UPDATE reserved_payments SET status = 'PAID', paidTransactionId = :paidTransactionId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsPaid(id: String, paidTransactionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reserved_payments SET status = 'CANCELLED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun cancelPayment(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reserved_payments SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM reserved_payments")
    suspend fun getAllForSync(): List<ReservedPayment>

    @Query("DELETE FROM reserved_payments")
    suspend fun deleteAll()
}

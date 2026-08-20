package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE isArchived = 0 ORDER BY bankName ASC")
    fun getAllActiveBankAccounts(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts ORDER BY updatedAt DESC")
    fun getAllBankAccountsIncludingArchived(): Flow<List<BankAccount>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id LIMIT 1")
    suspend fun getBankAccountById(id: String): BankAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: BankAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(accounts: List<BankAccount>)

    @Update
    suspend fun update(account: BankAccount)

    @Query("UPDATE bank_accounts SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveBankAccount(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM bank_accounts WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()
}

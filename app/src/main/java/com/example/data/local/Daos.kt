package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUser(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserSync(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
}

@Dao
interface CapsterDao {
    @Query("SELECT * FROM capsters")
    fun getAllCapsters(): Flow<List<CapsterEntity>>

    @Query("SELECT * FROM capsters WHERE id = :id LIMIT 1")
    suspend fun getCapsterById(id: Int): CapsterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapster(capster: CapsterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapsters(capsters: List<CapsterEntity>)

    @Query("UPDATE capsters SET status = :status WHERE id = :id")
    suspend fun updateCapsterStatus(id: Int, status: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)
}

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY createdAt DESC")
    fun getAllReservations(): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getReservationsForUser(email: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE capsterName LIKE :capsterName ORDER BY createdAt DESC")
    fun getReservationsForCapster(capsterName: String): Flow<List<ReservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: ReservationEntity): Long

    @Query("UPDATE reservations SET status = :status WHERE id = :id")
    suspend fun updateReservationStatus(id: Int, status: String)

    @Query("UPDATE reservations SET queueNo = :queueNo WHERE id = :id")
    suspend fun updateReservationQueue(id: Int, queueNo: String)
}

@Dao
interface WaterfallStageDao {
    @Query("SELECT * FROM waterfall_stages ORDER BY stageId ASC")
    fun getStages(): Flow<List<WaterfallStageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<WaterfallStageEntity>)

    @Query("UPDATE waterfall_stages SET status = :status, percentage = :percentage WHERE stageId = :stageId")
    suspend fun updateStageStatus(stageId: Int, status: String, percentage: Int)
}

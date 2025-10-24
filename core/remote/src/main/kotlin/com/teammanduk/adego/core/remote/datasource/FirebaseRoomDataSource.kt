package com.teammanduk.adego.core.remote.datasource

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.teammanduk.adego.core.data_api.datasource.RoomDataSource
import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.data_api.model.RoomDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FirebaseRoomDataSource @Inject constructor() : RoomDataSource {

    private val database: DatabaseReference by lazy {
        Log.d(TAG, "[Firebase] DatabaseReference 초기화 중...")
        try {
            val firebaseDatabase = FirebaseDatabase.getInstance()
            val databaseUrl = firebaseDatabase.reference.toString()
            Log.d(TAG, "[Firebase] Database URL: $databaseUrl")

            val ref = firebaseDatabase.reference
            Log.d(TAG, "[Firebase] DatabaseReference 초기화 완료: ${ref.database.app.name}")
            ref
        } catch (e: Exception) {
            Log.e(TAG, "[Firebase] DatabaseReference 초기화 실패!", e)
            throw e
        }
    }

    override suspend fun createRoom(roomDto: RoomDto): Result<Unit> {
        return try {
            Log.d(TAG, "[Firebase] Room 저장 시작: ${roomDto.roomId}")
            database.child("rooms")
                .child(roomDto.roomId)
                .setValue(roomDto)
                .await()

            Log.d(TAG, "[Firebase] Room 저장 완료: ${roomDto.roomId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[Firebase] Room 저장 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun getRoom(roomId: String): Result<RoomDto?> {
        return try {
            val snapshot = database.child("rooms")
                .child(roomId)
                .get()
                .await()

            val room = snapshot.getValue(RoomDto::class.java)
            Log.d(TAG, "Room fetched: $roomId")
            Result.success(room)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch room", e)
            Result.failure(e)
        }
    }

    override fun observeRoom(roomId: String): Flow<RoomDto?> = callbackFlow {
        Log.d(TAG, "[Firebase] observeRoom() 호출됨 - roomId: $roomId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "[Firebase] observeRoom - onDataChange 콜백 호출됨")
                Log.d(TAG, "[Firebase] observeRoom - snapshot.exists(): ${snapshot.exists()}")
                Log.d(TAG, "[Firebase] observeRoom - snapshot.key: ${snapshot.key}")

                if (snapshot.exists()) {
                    Log.d(TAG, "[Firebase] observeRoom - snapshot value: ${snapshot.value}")
                }

                val room = snapshot.getValue(RoomDto::class.java)
                Log.d(TAG, "[Firebase] observeRoom - parsed room: roomId=${room?.roomId}, roomName=${room?.roomName}, destination=${room?.destination?.name}")
                trySend(room)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "[Firebase] observeRoom - cancelled: ${error.message}", error.toException())
                close(error.toException())
            }
        }

        val ref = database.child("rooms").child(roomId)
        Log.d(TAG, "[Firebase] observeRoom - 리스너 등록 시작: ${ref.path}")
        ref.addValueEventListener(listener)
        Log.d(TAG, "[Firebase] observeRoom - 리스너 등록 완료")

        awaitClose {
            Log.d(TAG, "[Firebase] observeRoom - 리스너 제거됨")
            ref.removeEventListener(listener)
        }
    }

    override suspend fun addParticipant(
        roomId: String,
        participantDto: ParticipantDto
    ): Result<Unit> {
        return try {
            Log.d(TAG, "[Firebase] 참여자 추가 시작: ${participantDto.userId} -> room $roomId")
            Log.d(TAG, "[Firebase] 참여자 정보 - name: ${participantDto.name}, profileColor: ${participantDto.profileColor}")

            val participantRef = database.child("participants")
                .child(roomId)
                .child(participantDto.userId)

            // 기존 참가자 데이터 확인
            val existingData = participantRef.get().await()

            if (existingData.exists()) {
                // 기존 참가자: name과 profileColor만 업데이트 (location, route, movementStatus 유지)
                Log.d(TAG, "[Firebase] 기존 참여자 발견 - name과 profileColor만 업데이트")
                val updates = mapOf<String, Any>(
                    "name" to participantDto.name,
                    "profileColor" to participantDto.profileColor,
                    "userId" to participantDto.userId
                )
                participantRef.updateChildren(updates).await()
            } else {
                // 신규 참가자: 전체 데이터 저장
                Log.d(TAG, "[Firebase] 신규 참여자 - 전체 데이터 저장")
                participantRef.setValue(participantDto).await()
            }

            Log.d(TAG, "[Firebase] 참여자 추가 완료: ${participantDto.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[Firebase] 참여자 추가 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun getParticipants(roomId: String): Result<List<ParticipantDto>> {
        return try {
            Log.d("MapPerformance", "[Firebase] getParticipants 시작 at ${System.currentTimeMillis()}")
            val snapshot = database.child("participants")
                .child(roomId)
                .get()
                .await()

            val participants = mutableListOf<ParticipantDto>()
            snapshot.children.forEach { child ->
                child.getValue(ParticipantDto::class.java)?.let { participant ->
                    val hasLocation = participant.location != null
                    Log.d("MapPerformance", "[Firebase] 초기 참여자 - userId: ${participant.userId}, hasLocation: $hasLocation")
                    participants.add(participant)
                }
            }
            Log.d("MapPerformance", "[Firebase] getParticipants 완료 at ${System.currentTimeMillis()}, count=${participants.size}")
            Result.success(participants)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get participants", e)
            Result.failure(e)
        }
    }

    override fun observeParticipants(roomId: String): Flow<List<ParticipantDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val participants = mutableListOf<ParticipantDto>()
                snapshot.children.forEach { child ->
                    child.getValue(ParticipantDto::class.java)?.let { participant ->
                        val hasLocation = participant.location != null
                        Log.d("MapPerformance", "[Firebase] 참여자 불러옴 - userId: ${participant.userId}, hasLocation: $hasLocation at ${System.currentTimeMillis()}")
                        Log.d(TAG, "[Firebase] 참여자 불러옴 - userId: ${participant.userId}, name: ${participant.name}, profileColor: ${participant.profileColor}")
                        participants.add(participant)
                    }
                }
                Log.d(TAG, "[Firebase] 총 ${participants.size}명의 참여자 불러옴")
                trySend(participants)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Participants observation cancelled", error.toException())
                close(error.toException())
            }
        }

        val ref = database.child("participants").child(roomId)
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override suspend fun updateParticipantLocation(
        roomId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long
    ): Result<Unit> {
        return try {
            val locationMap = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy" to accuracy,
                "updatedAt" to timestamp
            )

            val path = "participants/$roomId/$userId/location"
            Log.d("MapPerformance", "[Firebase] setValue 호출 직전 at ${System.currentTimeMillis()}, path=$path")

            val ref = database.child("participants")
                .child(roomId)
                .child(userId)
                .child("location")

            ref.setValue(locationMap).await()

            Log.d("MapPerformance", "[Firebase] setValue 완료 at ${System.currentTimeMillis()}")

            // 실제로 저장되었는지 즉시 확인
            val verification = ref.get().await()
            val verified = verification.exists()
            Log.d("MapPerformance", "[Firebase] 저장 확인: exists=$verified at ${System.currentTimeMillis()}")

            Log.d(TAG, "Location updated for user $userId in room $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("MapPerformance", "[Firebase] setValue 실패!", e)
            Log.e(TAG, "Failed to update location", e)
            Result.failure(e)
        }
    }

    override suspend fun updateParticipantRoute(
        roomId: String,
        userId: String,
        etaInSeconds: Int,
        distanceInMeters: Int,
        polyline: String,
        timestamp: Long
    ): Result<Unit> {
        return try {
            // polyline이 비어있으면 ETA/거리만 업데이트 (최적화)
            if (polyline.isEmpty()) {
                val updates = mapOf<String, Any>(
                    "etaInSeconds" to etaInSeconds,
                    "distanceInMeters" to distanceInMeters,
                    "updatedAt" to timestamp
                )

                database.child("participants")
                    .child(roomId)
                    .child(userId)
                    .child("route")
                    .updateChildren(updates)
                    .await()

                Log.d(TAG, "Route ETA/distance updated for user $userId in room $roomId (ETA: ${etaInSeconds}s, Distance: ${distanceInMeters}m)")
            } else {
                // polyline이 있으면 전체 저장
                val routeMap = mapOf(
                    "etaInSeconds" to etaInSeconds,
                    "distanceInMeters" to distanceInMeters,
                    "polyline" to polyline,
                    "updatedAt" to timestamp
                )

                database.child("participants")
                    .child(roomId)
                    .child(userId)
                    .child("route")
                    .setValue(routeMap)
                    .await()

                Log.d(TAG, "Route updated (full) for user $userId in room $roomId (ETA: ${etaInSeconds}s, Distance: ${distanceInMeters}m, polyline length: ${polyline.length})")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update route", e)
            Result.failure(e)
        }
    }

    override suspend fun updateParticipantMovementStatus(
        roomId: String,
        userId: String,
        movementStatus: String
    ): Result<Unit> {
        return try {
            database.child("participants")
                .child(roomId)
                .child(userId)
                .child("movementStatus")
                .setValue(movementStatus)
                .await()

            Log.d(TAG, "MovementStatus updated for user $userId in room $roomId: $movementStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update movementStatus", e)
            Result.failure(e)
        }
    }

    /**
     * 위치, ETA/거리, 이동 상태를 한 번에 업데이트
     * UpdateLocationUseCase에서 사용 - 3번의 Firebase 호출을 1번으로 통합
     */
    override suspend fun updateParticipantLocationAndStatus(
        roomId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long,
        movementStatus: String,
        etaInSeconds: Int?,
        distanceInMeters: Int?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "location/latitude" to latitude,
                "location/longitude" to longitude,
                "location/accuracy" to accuracy,
                "location/updatedAt" to timestamp,
                "movementStatus" to movementStatus
            )

            // 경로 정보가 있으면 ETA/거리 추가 (polyline은 제외)
            if (etaInSeconds != null && distanceInMeters != null) {
                updates["route/etaInSeconds"] = etaInSeconds
                updates["route/distanceInMeters"] = distanceInMeters
                updates["route/updatedAt"] = timestamp
            }

            database.child("participants")
                .child(roomId)
                .child(userId)
                .updateChildren(updates)
                .await()

            Log.d(TAG, "Participant updated (unified) for user $userId - location, status${if (etaInSeconds != null) ", ETA" else ""}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update participant (unified)", e)
            Result.failure(e)
        }
    }

    override suspend fun removeParticipant(roomId: String, userId: String): Result<Unit> {
        return try {
            database.child("participants")
                .child(roomId)
                .child(userId)
                .removeValue()
                .await()

            Log.d(TAG, "Participant removed: $userId from room $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove participant", e)
            Result.failure(e)
        }
    }

    override suspend fun generateUniqueRoomId(): String {
        var roomId: String
        var exists: Boolean

        Log.d(TAG, "[Firebase] 고유 roomId 생성 시작")
        try {
            do {
                // 6자리 영숫자 코드 생성 (대문자 + 숫자)
                roomId = generateRandomCode()
                Log.d(TAG, "[Firebase] 랜덤 코드 생성: $roomId")

                // 중복 체크
                Log.d(TAG, "[Firebase] Firebase에서 중복 체크 시작: rooms/$roomId")
                val snapshot = database.child("rooms")
                    .child(roomId)
                    .get()
                    .await()

                exists = snapshot.exists()
                Log.d(TAG, "[Firebase] 중복 체크 결과: exists=$exists")
            } while (exists)

            Log.d(TAG, "[Firebase] 고유 roomId 생성 완료: $roomId")
            return roomId
        } catch (e: Exception) {
            Log.e(TAG, "[Firebase] roomId 생성 중 에러 발생", e)
            throw e
        }
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    companion object {
        private const val TAG = "AdegoRoom"
    }
}

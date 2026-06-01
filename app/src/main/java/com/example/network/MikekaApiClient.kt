package com.example.network

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ─── API MODELS ───

data class BaseResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "api_version") val apiVersion: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "message") val message: String? = null
)

data class LoginResponseData(
    @Json(name = "action") val action: String, // "login" or "need_confirmation"
    @Json(name = "token") val token: String? = null,
    @Json(name = "user") val user: UserProfile? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "display_phone") val displayPhone: String? = null,
    @Json(name = "subscription") val subscription: UserSubscription? = null
)

data class UserProfile(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "display_phone") val displayPhone: String? = null,
    @Json(name = "is_tipster") val isTipster: Int = 0,
    @Json(name = "commission_rate") val commissionRate: Float = 10f,
    @Json(name = "total_earnings") val totalEarnings: Float = 0f,
    @Json(name = "pending_earnings") val pendingEarnings: Float = 0f,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class UserSubscription(
    @Json(name = "id") val id: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "package_type") val packageType: String, // "normal", "vip", "vvip", "leo"
    @Json(name = "package_amount") val packageAmount: Float,
    @Json(name = "order_id") val orderId: String? = null,
    @Json(name = "payment_status") val paymentStatus: String, // "pending", "completed", "failed"
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String? = null,
    @Json(name = "seconds_left") val secondsLeft: Long? = null
)

data class MyProfileResponse(
    @Json(name = "user") val user: UserProfile? = null,
    @Json(name = "subscription") val subscription: UserSubscription? = null
)

data class PublicStats(
    @Json(name = "users") val users: Int = 0,
    @Json(name = "tipsters") val tipsters: Int = 0,
    @Json(name = "slips_today") val slipsToday: Int = 0,
    @Json(name = "active_subs") val activeSubs: Int = 0,
    @Json(name = "wins_today") val winsToday: Int = 0
)

data class TipsterListItem(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "total_slips_created") val totalSlipsCreated: Int? = 0,
    @Json(name = "total_earnings") val totalEarnings: Float? = 0f,
    @Json(name = "rating") val rating: Float? = 0f,
    @Json(name = "follower_count") val followerCount: Int? = 0,
    @Json(name = "slips_today") val slipsToday: Int? = 0,
    @Json(name = "total_wins") val totalWins: Int? = 0,
    @Json(name = "total_losses") val totalLosses: Int? = 0,
    @Json(name = "win_rate") val winRate: Float? = 0f,
    @Json(name = "is_following") val isFollowing: Boolean? = false
)

data class TipsterListResponse(@Json(name = "tipsters") val tipsters: List<TipsterListItem>)

data class TipsterWinLossStats(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "wins") val wins: Int = 0,
    @Json(name = "losses") val losses: Int = 0,
    @Json(name = "voids") val voids: Int = 0,
    @Json(name = "pending") val pending: Int = 0,
    @Json(name = "win_rate") val winRate: Float = 0f,
    @Json(name = "avg_odds") val avgOdds: Float = 0f,
    @Json(name = "streak_type") val streakType: String? = null,
    @Json(name = "streak_count") val streakCount: Int = 0
)

data class TipsterDetailResponse(
    @Json(name = "tipster") val tipster: TipsterListItem,
    @Json(name = "slips") val slips: List<PremiumSlip>? = null,
    @Json(name = "uploaded_slips") val uploadedSlips: List<Map<String, Any>>? = null,
    @Json(name = "stats_all") val statsAll: TipsterWinLossStats? = null,
    @Json(name = "stats_month") val statsMonth: TipsterWinLossStats? = null,
    @Json(name = "stats_today") val statsToday: TipsterWinLossStats? = null
)

data class LeaderboardResponse(@Json(name = "leaderboard") val leaderboard: List<TipsterListItem>)

data class FreePreviewResponse(@Json(name = "slip") val slip: PremiumSlip? = null)

data class PremiumSlip(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "booking_code") val bookingCode: String,
    @Json(name = "match_details") val matchDetails: String,
    @Json(name = "odds") val odds: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "result") val result: String? = null, // "win", "loss", "void"
    @Json(name = "created_date") val createdDate: String,
    @Json(name = "package_type") val packageType: String? = null,
    @Json(name = "tipster_name") val tipsterName: String? = null
)

data class PremiumSlipsResponse(
    @Json(name = "slips") val slips: List<PremiumSlip> = emptyList(),
    @Json(name = "date") val date: String? = null
)

data class AppConfigPackage(
    @Json(name = "type") val type: String, // "normal", "vip", "vvip", "leo"
    @Json(name = "name") val name: String,
    @Json(name = "price") val price: Int,
    @Json(name = "duration") val duration: String,
    @Json(name = "days") val days: Int
)

data class APIConfigResponse(
    @Json(name = "packages") val packages: List<AppConfigPackage> = emptyList(),
    @Json(name = "currency") val currency: String = "TZS",
    @Json(name = "support_phone") val supportPhone: String? = null,
    @Json(name = "support_email") val supportEmail: String? = null
)

data class CreateOrderResponse(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "package_type") val packageType: String,
    @Json(name = "amount") val amount: Int,
    @Json(name = "message") val message: String? = null
)

data class CheckStatusResponse(
    @Json(name = "is_payment_successful") val isPaymentSuccessful: Boolean,
    @Json(name = "transaction_id") val transactionId: String? = null,
    @Json(name = "subscription") val subscription: UserSubscription? = null
)

data class FollowResponse(
    @Json(name = "action") val action: String, // "followed" or "unfollowed"
    @Json(name = "is_following") val isFollowing: Boolean
)

data class ChatMessage(
    @Json(name = "id") val id: Int,
    @Json(name = "sender_id") val senderId: Int,
    @Json(name = "receiver_id") val receiverId: Int,
    @Json(name = "message") val message: String,
    @Json(name = "message_type") val messageType: String = "text",
    @Json(name = "sender_type") val senderType: String, // "user" or "admin"
    @Json(name = "created_at") val createdAt: String
)

data class ChatMessageListResponse(@Json(name = "messages") val messages: List<ChatMessage> = emptyList())

data class SendMessageResponse(
    @Json(name = "message_id") val messageId: Int,
    @Json(name = "sent_at") val sentAt: String
)

data class UnreadCountResponse(@Json(name = "unread_count") val unreadCount: Int = 0)


// ─── LOGIN & REGISTRATION REQUEST BODIES ───

data class LoginRequest(@Json(name = "phone") val phone: String)

data class RegisterRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "name") val name: String,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "device_name") val deviceName: String? = null,
    @Json(name = "platform") val platform: String = "android",
    @Json(name = "fcm_token") val fcmToken: String? = null
)

data class CreateOrderRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "amount") val amount: Int,
    @Json(name = "package_type") val packageType: String
)

data class CheckStatusRequest(@Json(name = "order_id") val orderId: String)

data class FollowRequest(@Json(name = "tipster_id") val tipsterId: Int)

data class SendMessageBody(@Json(name = "message") val message: String)


// ─── RETROFIT SERVICE INTERFACE ───

interface MikekaApiService {
    
    // Auth Routes (Public)
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): BaseResponse<LoginResponseData>

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): BaseResponse<LoginResponseData>

    @POST("auth/logout")
    suspend fun logout(): BaseResponse<Unit>

    // Public Stats
    @GET("stats")
    suspend fun getStats(): BaseResponse<PublicStats>

    // Tipsters
    @GET("tipsters")
    suspend fun getTipsters(): BaseResponse<TipsterListResponse>

    @GET("tipsters/{id}")
    suspend fun getTipsterDetail(@Path("id") id: Int): BaseResponse<TipsterDetailResponse>

    @GET("leaderboard")
    suspend fun getLeaderboard(): BaseResponse<LeaderboardResponse>

    // Preview non-auth slip representation
    @GET("preview-slip")
    suspend fun getFreePreviewSlip(): BaseResponse<FreePreviewResponse>

    // User Logged Context Profiles
    @GET("me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    @GET("config")
    suspend fun getPortalConfig(): BaseResponse<APIConfigResponse>

    // Slips
    @GET("slips")
    suspend fun getTodayPremiumSlips(@Query("date") date: String? = null): BaseResponse<PremiumSlipsResponse>

    // Checkout Billing Integration
    @POST("payments/create-order")
    suspend fun createPaymentOrder(@Body req: CreateOrderRequest): BaseResponse<CreateOrderResponse>

    @POST("payments/check-status")
    suspend fun checkPaymentStatus(@Body req: CheckStatusRequest): BaseResponse<CheckStatusResponse>

    // Follow Updates
    @POST("follow")
    suspend fun toggleFollow(@Body req: FollowRequest): BaseResponse<FollowResponse>

    // Chat
    @GET("chat/messages")
    suspend fun getChatMessages(
        @Query("last_id") lastId: Int? = null,
        @Query("limit") limit: Int? = null
    ): BaseResponse<ChatMessageListResponse>

    @POST("chat/send")
    suspend fun sendChatMessage(@Body body: SendMessageBody): BaseResponse<SendMessageResponse>

    @GET("chat/unread-count")
    suspend fun getChatUnreadCount(): BaseResponse<UnreadCountResponse>
}


// ─── RETROFIT CLIENT MANAGER ───

object MikekaApiClient {
    private const val BASE_URL = "https://mikekaapp.co.tz/api/"
    
    // Simple volatile storage for bearer token to inject dynamically
    @Volatile
    var authToken: String? = null

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = authToken
        
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        // Exclude the path itself from being bloated but append endpoint as query param
        val url = originalRequest.url
        val segments = url.pathSegments
        val relativeSegments = mutableListOf<String>()
        var foundBase = false
        for (segment in segments) {
            if (foundBase) {
                relativeSegments.add(segment)
            } else if (segment == "api" || segment == "api.php") {
                foundBase = true
            }
        }
        
        val relativePath = if (relativeSegments.isNotEmpty()) {
            relativeSegments.joinToString("/")
        } else {
            val idx = segments.indexOf("api")
            if (idx != -1 && idx < segments.size - 1) {
                segments.subList(idx + 1, segments.size).joinToString("/")
            } else {
                segments.lastOrNull() ?: ""
            }
        }
        
        // Rewrite the path directly to "/api.php" and append `endpoint` as a query parameter.
        // This ensures fully-functional direct routing to the API gateway script even if the host's URL-subpath-rewriting is disabled.
        val finalRequest = if (relativePath.isNotBlank()) {
            val newUrl = url.newBuilder()
                .encodedPath("/api.php")
                .setQueryParameter("endpoint", relativePath)
                .build()
            requestBuilder.url(newUrl).build()
        } else {
            val newUrl = url.newBuilder()
                .encodedPath("/api.php")
                .build()
            requestBuilder.url(newUrl).build()
        }
        
        chain.proceed(finalRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("MikekaApiClient", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: MikekaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MikekaApiService::class.java)
    }
}

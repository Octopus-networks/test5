package com.mithaq.app.data.local

import androidx.room.*
import com.mithaq.app.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_users")
data class CachedUserProfile(
    @PrimaryKey val uid: String,
    val name: String,
    val gender: String,
    val age: Int,
    val city: String,
    val country: String,
    val imageUrl: String,
    
    val sect: String,
    val prayerFrequency: String,
    val modestyPreference: String,
    val relocationWillingness: String,
    val polygamyAcceptance: Boolean,
    
    val guardianName: String?,
    val guardianEmail: String?,
    val guardianStatus: String?,

    val photoAccessApprovedUsers: List<String>,
    val photoAccessRequests: List<String>,

    val isWaliAccount: Boolean,
    val wardUid: String?,
    val verificationStatus: String,
    val voiceIntroUrl: String?,
    val fcmToken: String?,
    
    val isAdmin: Boolean,
    val isPremium: Boolean,
    val subscriptionPlan: String,
    val questionnaireAnswers: Map<String, String>,

    // --- Extended Upgrades ---
    val profileCreator: String,
    val regionalCode: String,
    
    val height: Int,
    val weight: Int,
    val bodyType: String,
    val hairColor: String,
    val eyeColor: String,
    val ethnicity: String,
    val appearanceRating: String,

    val maritalStatus: String,
    val haveChildren: String,
    val numberOfChildren: Int,
    val wantMoreChildren: String,
    val livingSituation: String,
    val occupation: String,
    val employmentStatus: String,
    val annualIncome: String,
    val drinkStatus: String,
    val smokeStatus: String,
    val eatingHabit: String,
    val relationshipLookingFor: String,

    val religiousValues: String,
    val attendReligiousService: String,
    val readQuran: String,
    val wearHijab: String,
    val wearNiqab: String,
    val convertStatus: String,
    val familyValue: String,

    val languagesSpoken: List<String>,
    val interestsEntertainments: List<String>,
    val interestsSports: List<String>,
    val interestsFoods: List<String>,
    val interestsMusics: List<String>,

    val aboutYourself: String,
    val partnerPreferences: String,
    val profileHeading: String,
    val idealPartner: String
)

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val roomId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val translatedContent: String?
)

@Entity(tableName = "cached_chat_rooms")
data class CachedChatRoom(
    @PrimaryKey val roomId: String,
    val memberIds: List<String>,
    val isChaperoned: Boolean,
    val waliEmail: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: Long
)

class MithaqConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val arr = org.json.JSONArray()
        value.forEach { arr.put(it) }
        return arr.toString()
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val arr = org.json.JSONArray(value)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        if (value == null) return "{}"
        val obj = org.json.JSONObject()
        value.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val obj = org.json.JSONObject(value)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_users WHERE uid = :uid")
    suspend fun getUser(uid: String): CachedUserProfile?

    @Query("SELECT * FROM cached_users WHERE uid = :uid")
    fun getUserFlow(uid: String): Flow<CachedUserProfile?>

    @Query("SELECT * FROM cached_users")
    fun getAllUsersFlow(): Flow<List<CachedUserProfile>>

    @Query("SELECT * FROM cached_users WHERE verificationStatus = 'PENDING'")
    fun getPendingUsersFlow(): Flow<List<CachedUserProfile>>

    @Query("SELECT * FROM cached_users")
    suspend fun getAllUsers(): List<CachedUserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CachedUserProfile>)

    @Query("DELETE FROM cached_users")
    suspend fun clearUsers()

    @Query("DELETE FROM cached_users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM cached_chat_rooms WHERE roomId = :roomId")
    suspend fun getChatRoom(roomId: String): CachedChatRoom?

    @Query("SELECT * FROM cached_chat_rooms WHERE roomId = :roomId")
    fun getChatRoomFlow(roomId: String): Flow<CachedChatRoom?>

    @Query("SELECT * FROM cached_chat_rooms")
    fun getAllChatRoomsFlow(): Flow<List<CachedChatRoom>>

    @Query("SELECT * FROM cached_chat_rooms")
    suspend fun getAllChatRooms(): List<CachedChatRoom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(room: CachedChatRoom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(rooms: List<CachedChatRoom>)

    @Query("SELECT * FROM cached_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoomFlow(roomId: String): Flow<List<CachedMessage>>

    @Query("SELECT * FROM cached_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    suspend fun getMessagesForRoom(roomId: String): List<CachedMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("DELETE FROM cached_messages WHERE roomId = :roomId")
    suspend fun clearMessagesForRoom(roomId: String)
}

@Database(
    entities = [CachedUserProfile::class, CachedMessage::class, CachedChatRoom::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MithaqConverters::class)
abstract class MithaqDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: MithaqDatabase? = null

        fun getDatabase(context: android.content.Context): MithaqDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MithaqDatabase::class.java,
                    "mithaq_offline_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Extension mappings
fun UserProfile.toCached(): CachedUserProfile = CachedUserProfile(
    uid = uid,
    name = name,
    gender = gender.name,
    age = age,
    city = city,
    country = country,
    imageUrl = imageUrl,
    sect = sect.name,
    prayerFrequency = prayerFrequency.name,
    modestyPreference = modestyPreference.name,
    relocationWillingness = relocationWillingness.name,
    polygamyAcceptance = polygamyAcceptance,
    guardianName = guardianName,
    guardianEmail = guardianEmail,
    guardianStatus = guardianStatus,
    photoAccessApprovedUsers = photoAccessApprovedUsers,
    photoAccessRequests = photoAccessRequests,
    isWaliAccount = isWaliAccount,
    wardUid = wardUid,
    verificationStatus = verificationStatus,
    voiceIntroUrl = voiceIntroUrl,
    fcmToken = fcmToken,
    isAdmin = isAdmin,
    isPremium = isPremium,
    subscriptionPlan = subscriptionPlan,
    questionnaireAnswers = questionnaireAnswers,

    // Extended Upgrades
    profileCreator = profileCreator,
    regionalCode = regionalCode,
    height = height,
    weight = weight,
    bodyType = bodyType,
    hairColor = hairColor,
    eyeColor = eyeColor,
    ethnicity = ethnicity,
    appearanceRating = appearanceRating,
    maritalStatus = maritalStatus,
    haveChildren = haveChildren,
    numberOfChildren = numberOfChildren,
    wantMoreChildren = wantMoreChildren,
    livingSituation = livingSituation,
    occupation = occupation,
    employmentStatus = employmentStatus,
    annualIncome = annualIncome,
    drinkStatus = drinkStatus,
    smokeStatus = smokeStatus,
    eatingHabit = eatingHabit,
    relationshipLookingFor = relationshipLookingFor,
    religiousValues = religiousValues,
    attendReligiousService = attendReligiousService,
    readQuran = readQuran,
    wearHijab = wearHijab,
    wearNiqab = wearNiqab,
    convertStatus = convertStatus,
    familyValue = familyValue,
    languagesSpoken = languagesSpoken,
    interestsEntertainments = interestsEntertainments,
    interestsSports = interestsSports,
    interestsFoods = interestsFoods,
    interestsMusics = interestsMusics,
    aboutYourself = aboutYourself,
    partnerPreferences = partnerPreferences,
    profileHeading = profileHeading,
    idealPartner = idealPartner
)

fun CachedUserProfile.toDomain(): UserProfile = UserProfile(
    uid = uid,
    name = name,
    gender = try { Gender.valueOf(gender) } catch (e: Exception) { Gender.MALE },
    age = age,
    city = city,
    country = country,
    imageUrl = imageUrl,
    sect = try { Sect.valueOf(sect) } catch (e: Exception) { Sect.SUNNI },
    prayerFrequency = try { PrayerFrequency.valueOf(prayerFrequency) } catch (e: Exception) { PrayerFrequency.ALWAYS },
    modestyPreference = try { ModestyPreference.valueOf(modestyPreference) } catch (e: Exception) { ModestyPreference.HIJAB },
    relocationWillingness = try { RelocationWillingness.valueOf(relocationWillingness) } catch (e: Exception) { RelocationWillingness.OPEN },
    polygamyAcceptance = polygamyAcceptance,
    guardianName = guardianName,
    guardianEmail = guardianEmail,
    guardianStatus = guardianStatus,
    photoAccessApprovedUsers = photoAccessApprovedUsers,
    photoAccessRequests = photoAccessRequests,
    isWaliAccount = isWaliAccount,
    wardUid = wardUid,
    verificationStatus = verificationStatus,
    voiceIntroUrl = voiceIntroUrl,
    fcmToken = fcmToken,
    isAdmin = isAdmin,
    isPremium = isPremium,
    subscriptionPlan = subscriptionPlan,
    questionnaireAnswers = questionnaireAnswers,

    // Extended Upgrades
    profileCreator = profileCreator,
    regionalCode = regionalCode,
    height = height,
    weight = weight,
    bodyType = bodyType,
    hairColor = hairColor,
    eyeColor = eyeColor,
    ethnicity = ethnicity,
    appearanceRating = appearanceRating,
    maritalStatus = maritalStatus,
    haveChildren = haveChildren,
    numberOfChildren = numberOfChildren,
    wantMoreChildren = wantMoreChildren,
    livingSituation = livingSituation,
    occupation = occupation,
    employmentStatus = employmentStatus,
    annualIncome = annualIncome,
    drinkStatus = drinkStatus,
    smokeStatus = smokeStatus,
    eatingHabit = eatingHabit,
    relationshipLookingFor = relationshipLookingFor,
    religiousValues = religiousValues,
    attendReligiousService = attendReligiousService,
    readQuran = readQuran,
    wearHijab = wearHijab,
    wearNiqab = wearNiqab,
    convertStatus = convertStatus,
    familyValue = familyValue,
    languagesSpoken = languagesSpoken,
    interestsEntertainments = interestsEntertainments,
    interestsSports = interestsSports,
    interestsFoods = interestsFoods,
    interestsMusics = interestsMusics,
    aboutYourself = aboutYourself,
    partnerPreferences = partnerPreferences,
    profileHeading = profileHeading,
    idealPartner = idealPartner
)

package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.BarberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BarberViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BarberRepository(application)

    // Current logged-in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Floating/Debug role bypass for easy evaluation
    private val _currentActiveRole = MutableStateFlow("CLIENT")
    val currentActiveRole: StateFlow<String> = _currentActiveRole.asStateFlow()

    // Active Capster context (if logged in as Capster)
    private val _activeCapster = MutableStateFlow<CapsterEntity?>(null)
    val activeCapster: StateFlow<CapsterEntity?> = _activeCapster.asStateFlow()

    // Database Flows
    val allCapsters: StateFlow<List<CapsterEntity>> = repository.getAllCapsters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReservations: StateFlow<List<ReservationEntity>> = repository.getAllReservations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterfallStages: StateFlow<List<WaterfallStageEntity>> = repository.getWaterfallStages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered reservations for the current user
    val userReservations: StateFlow<List<ReservationEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getReservationsForUser(user.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered reservations for the logged-in Capster
    val capsterReservations: StateFlow<List<ReservationEntity>> = _activeCapster
        .flatMapLatest { capster ->
            if (capster != null) {
                repository.getReservationsForCapster(capster.name)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback / Complaints list (Simulated local state)
    private val _complaints = MutableStateFlow<List<Pair<String, String>>>(listOf(
        "yudhaactaffian007@gmail.com" to "Layanan home service budi sangat memuaskan, sangat rapi dan ramah!",
        "client@gmail.com" to "Mohon untuk menambah varian pomade water-based di katalog produk."
    ))
    val complaints: StateFlow<List<Pair<String, String>>> = _complaints.asStateFlow()

    // Login validation states
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _unregisteredGmail = MutableStateFlow<String?>(null)
    val unregisteredGmail: StateFlow<String?> = _unregisteredGmail.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed DB
            repository.seedDatabaseIfNeeded()
            // Auto-login with Yudha's email for stellar first impressions, or let them login
            loginUser("yudhaactaffian007@gmail.com")
        }
    }

    fun clearAuthErrors() {
        _authError.value = null
        _unregisteredGmail.value = null
    }

    fun loginUserWithValidation(
        email: String,
        password: String,
        isNewUser: Boolean,
        name: String = "",
        phone: String = ""
    ) {
        _authError.value = null
        _unregisteredGmail.value = null

        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedEmail.isEmpty() || 
            (isNewUser && (trimmedName.isEmpty() || trimmedPhone.isEmpty() || trimmedPassword.isEmpty())) || 
            (!isNewUser && trimmedPassword.isEmpty())
        ) {
            _authError.value = "Gagal: Kolom tidak boleh kosong!"
            return
        }

        viewModelScope.launch {
            val formattedEmail = trimmedEmail.lowercase()
            val userExists = repository.getUserSync(formattedEmail)

            if (isNewUser) {
                // Register - automatically create
                loginUser(formattedEmail, trimmedName, trimmedPhone)
            } else {
                // Login
                if (userExists == null) {
                    _unregisteredGmail.value = formattedEmail
                } else {
                    // Check password: for demo accounts, check length or matching credentials.
                    // Demo accounts: admin@barberteak.com, capster@barberteak.com, agus@barberteak.com, yudhaactaffian007@gmail.com
                    val isDemo = formattedEmail == "admin@barberteak.com" || 
                                 formattedEmail == "capster@barberteak.com" || 
                                 formattedEmail == "agus@barberteak.com" || 
                                 formattedEmail == "yudhaactaffian007@gmail.com"
                    
                    val isCorrectPassword = if (isDemo) {
                        trimmedPassword == "123456"
                    } else {
                        trimmedPassword.length >= 6
                    }

                    if (!isCorrectPassword) {
                        _authError.value = "Gagal: Kata sandi atau email salah saat proses masuk!"
                    } else {
                        loginUser(formattedEmail, trimmedName, trimmedPhone)
                    }
                }
            }
        }
    }

    // Authentication Actions
    fun loginUser(email: String, name: String = "", phone: String = "") {
        viewModelScope.launch {
            val formattedEmail = email.trim().lowercase()
            var user = repository.getUserSync(formattedEmail)
            
            if (user == null) {
                // If it's a special predefined email, create it with roles
                val (role, seedName) = when (formattedEmail) {
                    "admin@barberteak.com" -> "ADMIN" to "Yudha Actaffian (Admin)"
                    "capster@barberteak.com" -> "CAPSTER" to "Budi (Capster Senior)"
                    "agus@barberteak.com" -> "CAPSTER" to "Agus (Capster Junior)"
                    "yudhaactaffian007@gmail.com" -> "CLIENT" to "Yudha Actaffian (VIP)"
                    else -> "CLIENT" to (if (name.isNotEmpty()) name else "Pelanggan Barberteak")
                }
                
                val seedPhone = if (phone.isNotEmpty()) phone else "08129999000"
                val tier = if (formattedEmail == "yudhaactaffian007@gmail.com") "Gold" else "Bronze"
                
                user = UserEntity(formattedEmail, seedName, seedPhone, role, "", tier)
                repository.insertUser(user)
            }
            
            _currentUser.value = user
            _currentActiveRole.value = user.role
            
            // Sync Capster profile if role is CAPSTER
            if (user.role == "CAPSTER") {
                val capsters = repository.getAllCapsters().firstOrNull() ?: emptyList()
                val match = capsters.find { user.name.contains(it.name.split(" ")[0]) } 
                    ?: capsters.firstOrNull()
                _activeCapster.value = match
            } else {
                _activeCapster.value = null
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeCapster.value = null
        _currentActiveRole.value = "CLIENT"
    }

    // Role Switcher bypass for quick demo review
    fun switchActiveRoleDirectly(role: String) {
        _currentActiveRole.value = role
        viewModelScope.launch {
            if (role == "ADMIN") {
                val adminUser = repository.getUserSync("admin@barberteak.com")
                if (adminUser != null) _currentUser.value = adminUser
            } else if (role == "CAPSTER") {
                val capsterUser = repository.getUserSync("capster@barberteak.com")
                if (capsterUser != null) {
                    _currentUser.value = capsterUser
                    val capsters = repository.getAllCapsters().firstOrNull() ?: emptyList()
                    _activeCapster.value = capsters.find { it.id == 1 }
                }
            } else {
                val clientUser = repository.getUserSync("yudhaactaffian007@gmail.com")
                if (clientUser != null) _currentUser.value = clientUser
            }
        }
    }

    // Booking Actions
    fun createReservation(
        capsterId: Int,
        capsterName: String,
        serviceName: String,
        servicePrice: Double,
        serviceType: String,
        homeAddress: String?,
        date: String,
        time: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            
            // Generate typical Mobile JKN style queue numbers
            val prefix = if (serviceType == "HOME") "H-" else "A-"
            val currentCount = (repository.getAllReservations().firstOrNull() ?: emptyList()).size
            val queueNo = String.format("%s%02d", prefix, currentCount + 1)

            val reservation = ReservationEntity(
                userEmail = user.email,
                userName = user.name,
                userPhone = user.phone,
                capsterId = capsterId,
                capsterName = capsterName,
                serviceName = serviceName,
                servicePrice = servicePrice,
                serviceType = serviceType,
                homeAddress = homeAddress,
                date = date,
                time = time,
                status = "PENDING",
                queueNo = queueNo
            )
            repository.insertReservation(reservation)
        }
    }

    fun updateReservationStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updateReservationStatus(id, status)
            
            // If completed and role is capster, also make the capster "Available" again
            if (status == "COMPLETED" || status == "REJECTED") {
                val res = (repository.getAllReservations().firstOrNull() ?: emptyList()).find { it.id == id }
                if (res != null) {
                    repository.updateCapsterStatus(res.capsterId, "Available")
                }
            } else if (status == "IN_PROGRESS") {
                val res = (repository.getAllReservations().firstOrNull() ?: emptyList()).find { it.id == id }
                if (res != null) {
                    repository.updateCapsterStatus(res.capsterId, "Busy")
                }
            }
        }
    }

    // Product purchase simulation
    fun buyProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.stock > 0) {
                // Update stock locally
                repository.insertProduct(product.copy(stock = product.stock - 1))
            }
        }
    }

    // Add Feedback/Complaint
    fun addComplaint(text: String) {
        val userEmail = _currentUser.value?.email ?: "anonymous@barberteak.com"
        _complaints.value = _complaints.value + (userEmail to text)
    }

    // Waterfall Admin Update
    fun updateWaterfallStage(stageId: Int, status: String, percentage: Int) {
        viewModelScope.launch {
            repository.updateWaterfallStage(stageId, status, percentage)
        }
    }

    // Active Capster Status Update
    fun updateActiveCapsterStatus(status: String) {
        viewModelScope.launch {
            val capster = _activeCapster.value ?: return@launch
            repository.updateCapsterStatus(capster.id, status)
            _activeCapster.value = capster.copy(status = status)
        }
    }
}

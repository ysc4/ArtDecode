package com.example.artdecode.presentation.artworkinfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.repository.ArtworkRepository
import com.example.artdecode.utils.Event // Make sure this is correctly pointing to your Event class

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take // <-- This might be new and missing
import kotlinx.coroutines.flow.single // <-- This might be new and missing
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException // <-- This might be new and missing
import kotlinx.coroutines.withTimeout // <-- This might be new and missing
// import kotlinx.coroutines.withTimeoutOrNull // This one is no longer used, so it can be removed
class ArtworkInfoViewModel(
    private val artworkRepository: ArtworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtworkInfoUiState())
    val uiState: StateFlow<ArtworkInfoUiState> = _uiState.asStateFlow()

    private var currentArtworkId: String? = null
    private var capturedImageUri: String? = null
    private var predictedArtStyle: String? = null
    private var predictedConfidence: Float? = null

    private var currentViewModelUserId: String? = null

    // Flag to track if similar artworks have already been loaded for the current session
    private var similarArtworksLoadedForCurrentSession: Boolean = false

    // Timeout duration for loading an existing artwork
    private val ARTWORK_LOAD_TIMEOUT_MS = 5000L // 5 seconds

    fun setCurrentUserId(userId: String) {
        currentViewModelUserId = userId
        artworkRepository.setCurrentUserId(userId)
        Log.d("ArtworkInfoVM", "ViewModel current user ID set to: $userId")
    }

    fun loadArtworkInfo(
        artworkId: String?,
        capturedImageUri: String?,
        artStyle: String?,
        confidenceScore: Float?
    ) {
        // Reset state for a fresh load
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            artwork = null, // Ensure artwork is null initially for a fresh load
            similarArtworks = emptyList() // Clear similar artworks initially
        )
        similarArtworksLoadedForCurrentSession = false // Reset for each new loadArtworkInfo call

        currentArtworkId = artworkId
        this.capturedImageUri = capturedImageUri
        this.predictedArtStyle = artStyle
        this.predictedConfidence = confidenceScore

        viewModelScope.launch {
            if (artworkId != null) {
                // PATH FOR EXISTING ARTWORKS (from HomeFragment)
                Log.d("ArtworkInfoVM", "Attempting to load info for EXISTING artwork with ID: $artworkId")

                try {
                    // Collect the first non-null artwork within a timeout
                    // This uses `withTimeout` which throws TimeoutCancellationException if timeout occurs.
                    val fetchedArtwork = withTimeout(ARTWORK_LOAD_TIMEOUT_MS) {
                        artworkRepository.getArtworkFlowById(artworkId)
                            .filterNotNull() // Only process non-null artwork emissions
                            .onEach {
                                Log.d("ArtworkInfoVM", "--- FETCHED EXISTING ARTWORK FLOW EMISSION (NON-NULL) ---")
                                Log.d("ArtworkInfoVM", "Fetched artwork for ID '$artworkId': $it")
                            }
                            .take(1) // Take only the first non-null item and then cancel the upstream flow
                            .single() // Converts a Flow of 1 item into that item. Will throw if flow is empty.
                    }

                    // If we reach here, fetchedArtwork is guaranteed to be non-null (because of filterNotNull and single)
                    // and loaded within the timeout.
                    _uiState.value = _uiState.value.copy(
                        artwork = fetchedArtwork,
                        isLoading = false,
                        errorMessage = null // Clear any error once artwork is found
                    )

                    // Only fetch similar artworks once for this session of loadArtworkInfo
                    if (!similarArtworksLoadedForCurrentSession) {
                        fetchedArtwork.artStyle?.let { styleFromDb ->
                            Log.d("ArtworkInfoVM", "Fetching similar artworks for existing artwork. Style from DB: '$styleFromDb', Exclude ID: $artworkId")
                            artworkRepository.getSimilarArtworks(styleFromDb, artworkId)
                                .onEach { similarArtworksList ->
                                    Log.d("ArtworkInfoVM", "Received ${similarArtworksList.size} similar artworks for existing artwork (Style: '$styleFromDb').")
                                    _uiState.value = _uiState.value.copy(
                                        similarArtworks = similarArtworksList
                                    )
                                    similarArtworksLoadedForCurrentSession = true // Mark as loaded
                                }
                                .launchIn(viewModelScope) // Launch similar artwork collection
                        } ?: run {
                            Log.w("ArtworkInfoVM", "Existing artwork ID: $artworkId has NULL or empty 'artStyle' field in database. Cannot fetch similar artworks.")
                            _uiState.value = _uiState.value.copy(
                                similarArtworks = emptyList()
                                // errorMessage is already null from initial state, or could be set for "no style"
                            )
                            similarArtworksLoadedForCurrentSession = true
                        }
                    } else {
                        Log.d("ArtworkInfoVM", "Similar artworks already loaded for this session, skipping re-fetch.")
                    }

                } catch (e: TimeoutCancellationException) {
                    // This catch block handles the timeout explicitly
                    Log.e("ArtworkInfoVM", "Artwork with ID: $artworkId not found within ${ARTWORK_LOAD_TIMEOUT_MS}ms (TimeoutCancellationException).", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Artwork not found within timeout.") // Re-added for definitive 'not found'
                    )
                } catch (e: NoSuchElementException) {
                    // This catch block handles if filterNotNull().take(1).single() finds no elements AND no timeout
                    // This is less likely with Room flows that continuously emit, but good for completeness.
                    Log.e("ArtworkInfoVM", "Artwork with ID: $artworkId not found (NoSuchElementException - flow completed without emitting non-null).", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Artwork not found.")
                    )
                }
                catch (e: Exception) {
                    // Catch any other general exceptions during loading an existing artwork
                    Log.e("ArtworkInfoVM", "Error loading artwork with ID $artworkId: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Error loading artwork: ${e.localizedMessage ?: "Unknown error"}")
                    )
                }

            } else {
                // PATH FOR NEWLY SCANNED ARTWORKS (This path is confirmed working as it doesn't wait for DB)
                Log.d("ArtworkInfoVM", "Loading info for NEWLY SCANNED artwork.")
                val tempArtwork = Artwork(
                    id = null, // Will be generated when saved to DB
                    imageUri = capturedImageUri,
                    artStyle = artStyle,
                    confidenceScore = confidenceScore,
                    userId = currentViewModelUserId,
                    // No capturedAt, will be set when saved
                    // No dbId, will be set when saved
                )
                Log.d("ArtworkInfoVM", "Created temp artwork: Style: ${tempArtwork.artStyle}, UserID: ${tempArtwork.userId}")

                _uiState.value = _uiState.value.copy(
                    artwork = tempArtwork, // Set the temporary artwork immediately
                    isLoading = false,
                    errorMessage = null // Clear any error
                )

                artStyle?.let { style ->
                    Log.d("ArtworkInfoVM", "Requesting similar artworks for new scan. Style: '$style', Exclude ID: null")
                    artworkRepository.getSimilarArtworks(style, null)
                        .onEach { similarArtworksList ->
                            Log.d("ArtworkInfoVM", "Received ${similarArtworksList.size} similar artworks for new scan (Style: '$style').")
                            _uiState.value = _uiState.value.copy(
                                similarArtworks = similarArtworksList,
                                errorMessage = null
                            )
                        }
                        .launchIn(viewModelScope)
                } ?: run {
                    Log.w("ArtworkInfoVM", "Newly scanned artwork has NULL or empty 'artStyle'. Cannot fetch similar artworks.")
                    _uiState.value = _uiState.value.copy(
                        similarArtworks = emptyList()
                        // errorMessage is null from initial state
                    )
                }
            }
        }
    }

    fun onScanMoreClick() {
        _uiState.value = _uiState.value.copy(navigateToScan = Event(Unit))
    }

    fun onReportClick() {
        _uiState.value = _uiState.value.copy(navigateToReport = Event(Unit))
    }

    fun onBackClick() {
        _uiState.value = _uiState.value.copy(navigateBack = Event(Unit))
    }

    fun onSimilarArtworkClick(artworkId: String?) {
        // When navigating to a similar artwork, ensure we pass the ID to loadArtworkInfo
        _uiState.value = _uiState.value.copy(navigateToSimilarArtwork = Event(artworkId))
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            navigateBack = null,
            navigateToScan = null,
            navigateToReport = null,
            navigateToSimilarArtwork = null,
            errorMessage = null // Clear any error message after it's handled by UI (e.g., toast shown)
        )
    }
}
package com.example.artdecode.presentation.artworkinfo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artdecode.data.model.Artwork
import com.example.artdecode.data.repository.ArtworkRepository
import com.example.artdecode.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

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

    private var similarArtworksLoadedForCurrentSession: Boolean = false

    private val ARTWORK_LOAD_TIMEOUT_MS = 5000L

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
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            artwork = null,
            similarArtworks = emptyList()
        )
        similarArtworksLoadedForCurrentSession = false

        currentArtworkId = artworkId
        this.capturedImageUri = capturedImageUri
        this.predictedArtStyle = artStyle
        this.predictedConfidence = confidenceScore

        viewModelScope.launch {
            if (artworkId != null) {
                Log.d("ArtworkInfoVM", "Attempting to load info for EXISTING artwork with ID: $artworkId")

                try {
                    val fetchedArtwork = withTimeout(ARTWORK_LOAD_TIMEOUT_MS) {
                        artworkRepository.getArtworkFlowById(artworkId)
                            .filterNotNull()
                            .onEach {
                                Log.d("ArtworkInfoVM", "--- FETCHED EXISTING ARTWORK FLOW EMISSION (NON-NULL) ---")
                                Log.d("ArtworkInfoVM", "Fetched artwork for ID '$artworkId': $it")
                            }
                            .take(1)
                            .single()
                    }

                    _uiState.value = _uiState.value.copy(
                        artwork = fetchedArtwork,
                        isLoading = false,
                        errorMessage = null
                    )

                    if (!similarArtworksLoadedForCurrentSession) {
                        fetchedArtwork.artStyle?.let { styleFromDb ->
                            Log.d("ArtworkInfoVM", "Fetching similar artworks for existing artwork. Style from DB: '$styleFromDb', Exclude ID: $artworkId")
                            artworkRepository.getSimilarArtworks(styleFromDb, artworkId)
                                .onEach { similarArtworksList ->
                                    Log.d("ArtworkInfoVM", "Received ${similarArtworksList.size} similar artworks for existing artwork (Style: '$styleFromDb').")
                                    _uiState.value = _uiState.value.copy(
                                        similarArtworks = similarArtworksList
                                    )
                                    similarArtworksLoadedForCurrentSession = true
                                }
                                .launchIn(viewModelScope)
                        } ?: run {
                            Log.w("ArtworkInfoVM", "Existing artwork ID: $artworkId has NULL or empty 'artStyle' field in database. Cannot fetch similar artworks.")
                            _uiState.value = _uiState.value.copy(
                                similarArtworks = emptyList()
                            )
                            similarArtworksLoadedForCurrentSession = true
                        }
                    } else {
                        Log.d("ArtworkInfoVM", "Similar artworks already loaded for this session, skipping re-fetch.")
                    }

                } catch (e: TimeoutCancellationException) {
                    Log.e("ArtworkInfoVM", "Artwork with ID: $artworkId not found within ${ARTWORK_LOAD_TIMEOUT_MS}ms (TimeoutCancellationException).", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Artwork not found within timeout.")
                    )
                } catch (e: NoSuchElementException) {
                    Log.e("ArtworkInfoVM", "Artwork with ID: $artworkId not found (NoSuchElementException - flow completed without emitting non-null).", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Artwork not found.")
                    )
                }
                catch (e: Exception) {
                    Log.e("ArtworkInfoVM", "Error loading artwork with ID $artworkId: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        artwork = null,
                        similarArtworks = emptyList(),
                        isLoading = false,
                        errorMessage = Event("Error loading artwork: ${e.localizedMessage ?: "Unknown error"}")
                    )
                }

            } else {
                Log.d("ArtworkInfoVM", "Loading info for NEWLY SCANNED artwork.")
                val tempArtwork = Artwork(
                    id = null,
                    imageUri = capturedImageUri,
                    artStyle = artStyle,
                    confidenceScore = confidenceScore,
                    userId = currentViewModelUserId,
                )
                Log.d("ArtworkInfoVM", "Created temp artwork: Style: ${tempArtwork.artStyle}, UserID: ${tempArtwork.userId}")

                _uiState.value = _uiState.value.copy(
                    artwork = tempArtwork,
                    isLoading = false,
                    errorMessage = null
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
        _uiState.value = _uiState.value.copy(navigateToSimilarArtwork = Event(artworkId))
    }

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(
            navigateBack = null,
            navigateToScan = null,
            navigateToReport = null,
            navigateToSimilarArtwork = null,
            errorMessage = null
        )
    }
}
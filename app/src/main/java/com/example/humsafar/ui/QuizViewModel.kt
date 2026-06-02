package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.GamificationRepository
import com.example.humsafar.data.QuizPrepareManager
import com.example.humsafar.models.QuizAbandonRequest
import com.example.humsafar.models.QuizAnswerRequest
import com.example.humsafar.models.QuizCompleteRequest
import com.example.humsafar.models.QuizQuestionPublic
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnswerFeedback(
    val selectedIndex: Int,
    val correct: Boolean,
    val correctIndex: Int,
    val awarded: Int
)

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data class Playing(
        val sessionId: Int,
        val questions: List<QuizQuestionPublic>,
        val index: Int,
        val secondsPerQuestion: Int,
        val runningGems: Int,
        val feedback: AnswerFeedback?,
        val submitting: Boolean
    ) : QuizUiState
    data class AlreadyPlayed(val status: String, val gems: Int) : QuizUiState
    data class Finished(val gemsEarned: Int, val newBalance: Int) : QuizUiState
    data class Error(val message: String) : QuizUiState
}

class QuizViewModel : ViewModel() {

    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var sessionId: Int = 0
    private var completed = false

    fun load(tripId: Int) {
        _state.value = QuizUiState.Loading
        val uid = AuthManager.currentUser.value?.uid
        if (uid.isNullOrBlank()) {
            _state.value = QuizUiState.Error("Sign in to play the quiz.")
            return
        }
        QuizPrepareManager.consumeError(tripId)?.let {
            _state.value = QuizUiState.Error(it)
            return
        }
        QuizPrepareManager.consume(tripId)?.let { body ->
            applyStartBody(body)
            return
        }
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.startQuiz(uid, tripId)
                if (!resp.isSuccessful || resp.body() == null) {
                    _state.value = QuizUiState.Error("Couldn't start the quiz (HTTP ${resp.code()}).")
                    return@launch
                }
                applyStartBody(resp.body()!!)
            }.onFailure {
                _state.value = QuizUiState.Error(it.message ?: "Network error")
            }
        }
    }

    private fun applyStartBody(body: com.example.humsafar.models.QuizStartResponse) {
        sessionId = body.sessionId
        _state.value = when {
            body.alreadyPlayed ->
                QuizUiState.AlreadyPlayed(body.status, body.gemsEarned)
            body.questions.isEmpty() ->
                QuizUiState.Error("No questions available for this trip.")
            else -> QuizUiState.Playing(
                sessionId = body.sessionId,
                questions = body.questions,
                index = 0,
                secondsPerQuestion = body.secondsPerQuestion,
                runningGems = 0,
                feedback = null,
                submitting = false
            )
        }
    }

    /** selectedIndex = -1 means timed out (no selection). */
    fun submitAnswer(questionId: Int, selectedIndex: Int, secondsTaken: Double) {
        val playing = _state.value as? QuizUiState.Playing ?: return
        if (playing.submitting || playing.feedback != null) return
        _state.value = playing.copy(submitting = true)
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.answerQuiz(
                    QuizAnswerRequest(
                        sessionId = playing.sessionId,
                        questionId = questionId,
                        selectedIndex = selectedIndex,
                        secondsTaken = secondsTaken
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val r = resp.body()!!
                    _state.value = playing.copy(
                        runningGems = r.runningTotal,
                        feedback = AnswerFeedback(selectedIndex, r.correct, r.correctIndex, r.gemsAwarded),
                        submitting = false
                    )
                } else {
                    // Soft-fail: show as wrong, no gems, let the user continue.
                    _state.value = playing.copy(
                        feedback = AnswerFeedback(selectedIndex, false, -1, 0),
                        submitting = false
                    )
                }
            }.onFailure {
                _state.value = playing.copy(
                    feedback = AnswerFeedback(selectedIndex, false, -1, 0),
                    submitting = false
                )
            }
        }
    }

    /** Advance to the next question, or finish the quiz when there are none left. */
    fun advance() {
        val playing = _state.value as? QuizUiState.Playing ?: return
        if (playing.index + 1 < playing.questions.size) {
            _state.value = playing.copy(index = playing.index + 1, feedback = null)
        } else {
            complete()
        }
    }

    private fun complete() {
        if (completed) return
        completed = true
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.completeQuiz(QuizCompleteRequest(sessionId))
                if (resp.isSuccessful && resp.body() != null) {
                    val r = resp.body()!!
                    GamificationRepository.setBalance(r.newBalance)
                    _state.value = QuizUiState.Finished(r.gemsEarned, r.newBalance)
                } else {
                    GamificationRepository.refresh()
                    val gems = (_state.value as? QuizUiState.Playing)?.runningGems ?: 0
                    _state.value = QuizUiState.Finished(gems, GamificationRepository.gems.value)
                }
            }.onFailure {
                val gems = (_state.value as? QuizUiState.Playing)?.runningGems ?: 0
                _state.value = QuizUiState.Finished(gems, GamificationRepository.gems.value)
            }
        }
    }

    /** Quitting mid-quiz forfeits everything earned. */
    fun abandon() {
        if (completed || sessionId == 0) return
        completed = true
        viewModelScope.launch {
            runCatching { HumsafarClient.api.abandonQuiz(QuizAbandonRequest(sessionId)) }
        }
    }
}

package com.finance.manager.android.presentation.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Tag
import com.finance.manager.android.domain.usecase.tag.CreateTagUseCase
import com.finance.manager.android.domain.usecase.tag.DeleteTagUseCase
import com.finance.manager.android.domain.usecase.tag.ObserveTagsUseCase
import com.finance.manager.android.domain.usecase.tag.ToggleHiddenTagUseCase
import com.finance.manager.android.domain.usecase.tag.UpdateTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagManagementUiState(
    val isLoading: Boolean = true,
    val tags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val includeHidden: Boolean = false,
    val editing: Tag? = null,
    val creatingNew: Boolean = false,
    val message: String? = null,
) {
    val filtered: List<Tag>
        get() = tags
            .asSequence()
            .filter { includeHidden || !it.isHidden }
            .filter { searchQuery.isBlank() || it.tagName.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.tagName }
            .toList()
}

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val toggleHiddenTagUseCase: ToggleHiddenTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTagsUseCase().collect { list ->
                _uiState.update { it.copy(isLoading = false, tags = list) }
            }
        }
    }

    fun updateSearch(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun toggleIncludeHidden() = _uiState.update { it.copy(includeHidden = !it.includeHidden) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun startCreate() = _uiState.update { it.copy(creatingNew = true, editing = null) }
    fun startEdit(tag: Tag) = _uiState.update { it.copy(editing = tag, creatingNew = false) }
    fun dismissEditor() = _uiState.update { it.copy(editing = null, creatingNew = false) }

    fun save(tag: Tag) {
        viewModelScope.launch {
            val result = if (tag.tagId == 0) createTagUseCase(tag) else updateTagUseCase(tag)
            val msg = when (result) {
                is AppResult.Success<*> -> { dismissEditor(); "已儲存" }
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }

    fun toggleHidden(tag: Tag) {
        viewModelScope.launch {
            toggleHiddenTagUseCase(tag.tagId, !tag.isHidden)
        }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch {
            val result = deleteTagUseCase(tag.tagId)
            val msg = when (result) {
                is AppResult.Success -> "已刪除"
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }
}


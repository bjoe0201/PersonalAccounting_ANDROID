package com.finance.manager.android.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finance.manager.android.domain.common.AppResult
import com.finance.manager.android.domain.model.Category
import com.finance.manager.android.domain.usecase.category.CreateCategoryUseCase
import com.finance.manager.android.domain.usecase.category.DeleteCategoryUseCase
import com.finance.manager.android.domain.usecase.category.ObserveCategoriesUseCase
import com.finance.manager.android.domain.usecase.category.ToggleHiddenCategoryUseCase
import com.finance.manager.android.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CategoryFilter(val displayName: String, val type: String?) {
    ALL("全部", null),
    EXPENSE("個人支出", "E"),
    INCOME("個人收入", "I"),
    INVESTMENT("投資", "T"),
}

data class CategoryManagementUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val filter: CategoryFilter = CategoryFilter.ALL,
    val includeHidden: Boolean = false,
    val editing: Category? = null,
    val creatingNew: Boolean = false,
    val message: String? = null,
) {
    val filtered: List<Category>
        get() = categories
            .asSequence()
            .filter { includeHidden || !it.isHidden }
            .filter { filter.type == null || it.categoryType == filter.type }
            .filter { searchQuery.isBlank() || it.categoryName.contains(searchQuery, ignoreCase = true) || it.displayName.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.categoryName }
            .toList()
}

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val toggleHiddenCategoryUseCase: ToggleHiddenCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCategoriesUseCase().collect { list ->
                _uiState.update { it.copy(isLoading = false, categories = list) }
            }
        }
    }

    fun updateSearch(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun updateFilter(filter: CategoryFilter) = _uiState.update { it.copy(filter = filter) }
    fun toggleIncludeHidden() = _uiState.update { it.copy(includeHidden = !it.includeHidden) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun startCreate() = _uiState.update { it.copy(creatingNew = true, editing = null) }
    fun startEdit(category: Category) = _uiState.update { it.copy(editing = category, creatingNew = false) }
    fun dismissEditor() = _uiState.update { it.copy(editing = null, creatingNew = false) }

    fun save(category: Category) {
        viewModelScope.launch {
            val result = if (category.categoryId == 0) createCategoryUseCase(category)
            else updateCategoryUseCase(category)
            val msg = when (result) {
                is AppResult.Success<*> -> { dismissEditor(); "已儲存" }
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }

    fun toggleHidden(category: Category) {
        viewModelScope.launch {
            toggleHiddenCategoryUseCase(category.categoryId, !category.isHidden)
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase(category.categoryId)
            val msg = when (result) {
                is AppResult.Success -> "已刪除"
                is AppResult.Error -> result.message
            }
            _uiState.update { it.copy(message = msg) }
        }
    }
}


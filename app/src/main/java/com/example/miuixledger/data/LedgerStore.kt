package com.example.miuixledger.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ExpenseRecord(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val time: Long,
    val type: RecordType,
)

@Serializable
enum class RecordType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
}

object Categories {
    val expense = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "其他")
    val income = listOf("工资", "理财", "红包", "其他")
    fun forType(type: RecordType): List<String> = when (type) {
        RecordType.EXPENSE -> expense
        RecordType.INCOME -> income
    }
}

/** JSON 文件持久化（app 私有目录） */
class LedgerStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(context.filesDir, "ledger.json")
    private val mutex = Mutex()

    private val _records = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val records: StateFlow<List<ExpenseRecord>> = _records.asStateFlow()

    init {
        runCatching {
            if (file.exists()) {
                _records.value = json.decodeFromString<List<ExpenseRecord>>(file.readText())
                    .sortedByDescending { it.time }
            }
        }
    }

    suspend fun add(record: ExpenseRecord) = mutex.withLock {
        _records.value = (listOf(record) + _records.value).sortedByDescending { it.time }
        persist()
    }

    suspend fun remove(record: ExpenseRecord) = mutex.withLock {
        _records.value = _records.value - record
        persist()
    }

    suspend fun clearAll() = mutex.withLock {
        _records.value = emptyList()
        persist()
    }

    private fun persist() {
        runCatching { file.writeText(json.encodeToString(_records.value)) }
    }
}

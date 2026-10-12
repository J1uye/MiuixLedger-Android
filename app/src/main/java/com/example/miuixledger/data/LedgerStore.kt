package com.example.miuixledger.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class RecordType(val label: String) {
    EXPENSE("支出"),
    INCOME("收入"),
}

data class ExpenseRecord(
    val id: Long,
    val amount: Double,
    val category: String,
    val note: String,
    val time: Long,
    val type: RecordType,
)

object Categories {
    val expense = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "其他")
    val income = listOf("工资", "理财", "红包", "其他")
    fun forType(type: RecordType): List<String> = when (type) {
        RecordType.EXPENSE -> expense
        RecordType.INCOME -> income
    }
}

/**
 * JSON 文件持久化（app 私有目录）。
 *
 * 用 Android 内置的 org.json，避免引入 kotlinx-serialization 及其编译插件
 * （AGP 9 内置 Kotlin，少一个插件少一处版本冲突）。
 */
class LedgerStore(context: Context) {

    private val file = File(context.filesDir, "ledger.json")
    private val mutex = Mutex()

    private val _records = MutableStateFlow<List<ExpenseRecord>>(emptyList())
    val records: StateFlow<List<ExpenseRecord>> = _records.asStateFlow()

    init {
        runCatching {
            if (file.exists()) {
                _records.value = parse(file.readText()).sortedByDescending { it.time }
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
        runCatching {
            val arr = JSONArray()
            _records.value.forEach { r ->
                arr.put(
                    JSONObject().apply {
                        put("id", r.id)
                        put("amount", r.amount)
                        put("category", r.category)
                        put("note", r.note)
                        put("time", r.time)
                        put("type", r.type.name)
                    }
                )
            }
            file.writeText(arr.toString())
        }
    }

    private fun parse(text: String): List<ExpenseRecord> {
        if (text.isBlank()) return emptyList()
        val arr = JSONArray(text)
        val list = ArrayList<ExpenseRecord>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += ExpenseRecord(
                id = o.optLong("id"),
                amount = o.optDouble("amount", 0.0),
                category = o.optString("category", "其他"),
                note = o.optString("note", ""),
                time = o.optLong("time"),
                type = runCatching { RecordType.valueOf(o.optString("type", "EXPENSE")) }
                    .getOrDefault(RecordType.EXPENSE),
            )
        }
        return list
    }
}

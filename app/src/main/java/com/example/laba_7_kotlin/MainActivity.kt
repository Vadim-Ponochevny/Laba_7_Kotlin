package com.example.laba_7_kotlin


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {
    private lateinit var allContacts: List<Contact>
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Timber.plant(Timber.DebugTree())

        contactParsing()

        val searchButton = findViewById<Button>(R.id.btn_search)
        val editText = findViewById<EditText>(R.id.et_search)

        searchButton.setOnClickListener {
            val query = editText.text.toString().trim().lowercase()

            val filtered = if (query.isEmpty()) {
                allContacts
            } else {
                allContacts.filter {
                    it.name.lowercase().contains(query) ||
                            it.phone.lowercase().contains(query) ||
                            it.type.lowercase().contains(query)
                }
            }

            adapter.submitList(filtered)
        }
    }

    private fun contactParsing() {
        lifecycleScope.launch {
            try {
                allContacts = getContactsFromJson()

                allContacts.forEachIndexed { index, contact ->
                    Timber.d("Контакт #$index: $contact")
                }

                setupRecyclerView(allContacts)

            } catch (e: Exception) {
                Timber.e(e, "Error")
            }
        }
    }
    private fun setupRecyclerView(contacts: List<Contact>) {
        val recyclerView = findViewById<RecyclerView>(R.id.rView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        recyclerView.adapter = adapter
        adapter.submitList(contacts)
    }
}
private suspend fun getContactsFromJson(): List<Contact> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://drive.google.com/u/0/uc?id=1-KO-9GA3NzSgIc1dkAsNm8Dqw0fuPxcR&export=download")
        .build()

    val response = client.newCall(request).execute()

    if (!response.isSuccessful) {
        Timber.e("Ошибка HTTP: ${response.code}")
        return@withContext emptyList()
    }

    val jsonString = response.body?.string() ?: run {
        Timber.e("Пустое тело ответа")
        return@withContext emptyList()
    }

    val type = object : TypeToken<List<Contact>>() {}.type
    return@withContext Gson().fromJson(jsonString, type)
}
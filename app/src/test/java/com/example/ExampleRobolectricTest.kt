package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MemoryRepository
import com.example.security.KeystoreManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AHI", appName)
    }

    @Test
    fun `test memory persistence in Room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MemoryRepository(context)

        val id = repo.saveMemory(
            category = "PROJECT",
            title = "Neural Companion",
            content = "Test companion memory persistence"
        )
        assertTrue(id > 0)

        val memories = repo.getMemoriesSnapshot()
        assertTrue(memories.any { it.title == "Neural Companion" })
    }

    @Test
    fun `test user profile default greeting name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MemoryRepository(context)
        assertEquals("Alex", repo.userProfile.value.userName)
    }

    @Test
    fun `test keystore manager instance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keystore = KeystoreManager(context)
        assertNotNull(keystore)
    }
}

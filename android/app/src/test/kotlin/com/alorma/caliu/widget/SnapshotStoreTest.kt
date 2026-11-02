package com.alorma.caliu.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ThermostatState
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SnapshotStoreTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val scope = CoroutineScope(SupervisorJob())
  private val store by lazy {
    SnapshotStore(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "snapshot.preferences_pb") })
  }

  @After
  fun tearDown() {
    scope.cancel()
  }

  @Test
  fun `sense res desat no hi ha estat`() =
    runTest {
      assertNull(store.get())
    }

  @Test
  fun `desa i recupera tot l'estat`() =
    runTest {
      val state =
        ThermostatState(
          power = true,
          paused = true,
          mode = Mode.AUTO,
          currentTemp = 20.4,
          setpoint = 20.0,
          heating = false,
          override = Override(MinuteOfDay.of(14), 22.0),
          schedule = FakeThermostatRepository.DefaultSchedule,
          battery = null,
          lastUpdated = Instant.parse("2026-11-02T12:47:00Z"),
          minSetpoint = 5.0,
          maxSetpoint = 35.0,
        )

      store.save(state)

      assertEquals(state, store.get())
    }
}

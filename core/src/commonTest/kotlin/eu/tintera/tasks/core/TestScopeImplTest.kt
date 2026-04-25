package eu.tintera.tasks.core

import eu.tintera.tasks.State
import eu.tintera.tasks.TaskInfoQuery
import eu.tintera.tasks.core.data.*
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class TaskScopeImplTest {

    // 1. Vytvoříme si falešné závislosti pro test
    private lateinit var fakeRepository: FakeRepository
    private lateinit var scope: TaskScopeImpl<String, Int> // Input=String, Progress=Int
    private lateinit var testDispatcher: TestDispatcher

    @BeforeTest
    fun setup() {
        fakeRepository = FakeRepository()
        testDispatcher = StandardTestDispatcher()
    }

    private fun createScope(coroutineScope: CoroutineScope) {
        scope = TaskScopeImpl(
            taskId = Uuid.random(),
            data = "TestInput",
            retryCount = 0,
            parents = emptyList(),
            onForegroundInfoProvided = { true },
            scope = coroutineScope, // Vložíme testovací scope
            repository = fakeRepository,
            progressSerializer = FakeIntSerializer(), // Vlastní mock
            tags = emptySet(),
            typedTags = emptySet(),
            saveDispatcher = testDispatcher // Ovládáme časování IO!
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setProgress throttles database saves to 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope) // backgroundScope se sám na konci testu zruší

        // Act: Napálíme tam tři updaty v nulté milisekundě
        scope.setProgress(10)
        scope.setProgress(20)
        scope.setProgress(30)

        // Assert: Zatím se do DB nezapsalo nic (čeká se na 300ms tick)
        assertEquals(0, fakeRepository.saveCount)

        // Act: Posuneme čas o 300 milisekund
        advanceTimeBy(300.milliseconds)
        runCurrent() // Ujistíme se, že se vykonají coroutiny zapsané v dispatcher frontě

        // Assert: Zapsalo se to pouze JEDNOU a s poslední hodnotou (30)
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(30, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `exception in trySave does not crash scope and allows future updates`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        // Nasimulujeme rozbitou databázi
        fakeRepository.shouldThrowException = true

        // Act: První pokus, který spadne
        scope.setProgress(50)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: DB to zkusila, ale spadla. Coroutina však musí dál žít!
        assertEquals(1, fakeRepository.saveCount)

        // Databáze se "opravila"
        fakeRepository.shouldThrowException = false

        // Act: Další progress (musí projít, scope nesmí být mrtvý)
        scope.setProgress(100)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: Druhý zápis prošel úspěšně!
        assertEquals(2, fakeRepository.saveCount)
        assertEquals(100, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `flushProgress saves immediately without waiting for 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        scope.setProgress(99)

        // Assert: Před flushem je uloženo 0 krát
        assertEquals(0, fakeRepository.saveCount)

        // Act: Voláme manuální flush
        scope.flushProgress()
        runCurrent()

        // Assert: Bylo zapsáno okamžitě (čas se neposouval!)
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(99, fakeRepository.lastSavedProgress)
    }

    // --- Pomocné Mock třídy pro izolaci testu ---

    class FakeRepository : Repository { // Tady implementuj jen to nejnutnější
        var saveCount = 0
        var lastSavedProgress: Int? = null
        var shouldThrowException = false
        override fun dispatchableTasks(states: List<State>): Flow<List<DispatchableTask>> {
            TODO("Not yet implemented")
        }

        override fun processableTask(id: Uuid): Flow<ProcessableTask?> {
            TODO("Not yet implemented")
        }

        override suspend fun executableTask(id: Uuid): ExecutableTask? {
            TODO("Not yet implemented")
        }

        override suspend fun parentsDataFor(id: Uuid): List<ParentData> {
            TODO("Not yet implemented")
        }

        override fun parentStatesForTask(id: Uuid): Flow<List<State>> {
            TODO("Not yet implemented")
        }

        override suspend fun updateNextRun(
            id: Uuid,
            processTime: Instant,
            state: State,
            progressData: ByteArray?,
            runAttemptCount: Int?
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun updateRunAttemptCount(id: Uuid, runAttemptsCount: Int) {
            TODO("Not yet implemented")
        }

        override suspend fun updateTerminatingState(
            id: Uuid,
            state: State,
            finishedAt: Instant,
            outputData: ByteArray?
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun task(id: Uuid): Task? {
            TODO("Not yet implemented")
        }

        override suspend fun allByUniqueName(uniqueName: String): List<Task> {
            TODO("Not yet implemented")
        }

        override suspend fun delete(id: Uuid) {
            TODO("Not yet implemented")
        }

        override suspend fun insert(
            task: Task,
            tags: Set<String>,
            parentIds: Set<Uuid>
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun cleanOld(terminalStates: Set<State>) {
            TODO("Not yet implemented")
        }

        override fun taskInfosByTag(name: String): Flow<List<Info>> {
            TODO("Not yet implemented")
        }

        override fun taskInfos(query: TaskInfoQuery): Flow<List<Info>> {
            TODO("Not yet implemented")
        }

        override fun taskInfoById(id: Uuid): Flow<Info?> {
            TODO("Not yet implemented")
        }

        override fun taskInfoByIds(ids: Set<Uuid>): Flow<List<Info>> {
            TODO("Not yet implemented")
        }

        override suspend fun schedulableTasks(states: List<State>): List<SchedulableTask> {
            TODO("Not yet implemented")
        }

        override suspend fun childrenForTask(id: Uuid): List<Uuid> {
            TODO("Not yet implemented")
        }

        override suspend fun taskIdsByTagAndState(
            states: List<State>,
            tag: String
        ): List<Uuid> {
            TODO("Not yet implemented")
        }

        override suspend fun resetState(
            from: State,
            to: State,
            excludedIds: Set<Uuid>
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun updateProgressData(id: Uuid, progressData: ByteArray?) {
            saveCount++
            if (shouldThrowException) throw Exception("Fake DB locked!")
            // V reálu ukládáš ByteArray, tady si pro jednoduchost testu
            // vytáhneme tu původní Int hodnotu (pokud to Serializer umožní nasimulovat)
            lastSavedProgress = progressData?.decodeToString()?.toIntOrNull()
        }

        override suspend fun updateState(
            id: Uuid,
            state: State,
            allowedSourceStates: Set<State>,
            resetProcessTime: Boolean,
            runAttemptCount: Int?
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun updateTerminatingStateWithDescendants(
            id: Uuid,
            state: State,
            allowedSourceStates: Set<State>,
            finishedAt: Instant
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun upgradeData(
            id: Uuid,
            input: ByteArray?,
            output: ByteArray?,
            progress: ByteArray?,
            version: Int
        ) {
            TODO("Not yet implemented")
        }

        override suspend fun <T> withTransaction(action: suspend Repository.() -> T): T {
            TODO("Not yet implemented")
        }

        // Zbytek metod Repository může vrátit TODO() nebo empty hodnoty
    }

    class FakeIntSerializer : Serializer<Int> {
        override fun encodeToBytes(value: Int): ByteArray = value.toString().encodeToByteArray()
        override fun decodeFromBytes(bytes: ByteArray): Int = bytes.decodeToString().toInt()
    }
}
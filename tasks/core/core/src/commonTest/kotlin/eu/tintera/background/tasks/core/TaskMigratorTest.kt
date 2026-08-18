package eu.tintera.background.tasks.core

/*
class TaskMigratorTest {

    data class Data1(val text: String)
    data class Data2(val text: String, val length: Int)

    val data1Serializer = FakeStringSerializer<Data1>(
        encodeLogic = { it.text },
        decodeLogic = { Data1(text = it) }
    )

    val data2Serializer = FakeStringSerializer<Data2>(
        encodeLogic = { "${it.text}|${it.length}" },
        decodeLogic = { str ->
            val parts = str.split("|")
            Data2(parts[0], parts[1].toInt())
        }
    )


    private fun <Input : Any, Output : Any, Progress : Any> taskHandlerRegistration(
        currentVersion: Int,
        migrations: List<Migration>,
        inputSerializer: TaskDataSerializer<Input>,
        outputSerializer: TaskDataSerializer<Output>,
        progressSerializer: TaskDataSerializer<Progress>,
    ) = TaskRegistry.TaskRegistration(
        currentVersion = currentVersion,
        factory = { error("not implemented") },
        inputSerializer = inputSerializer,
        outputSerializer = outputSerializer,
        progressSerializer = progressSerializer,
        migrations = migrations
    )

    @Test
    fun `migrate should successfully transform data through multiple versions`() {
        // 1. ARRANGE
        // A simple migration chain V1 -> V2 -> V3.
        val registration = taskHandlerRegistration(
            currentVersion = 3,
            inputSerializer = data1Serializer,
            outputSerializer = data1Serializer,
            progressSerializer = data1Serializer,
            migrations = listOf(
                migration(1, 2) {
                    migrateInput<Data1, Data2>(data1Serializer, data2Serializer) { old ->
                        Data2(text = old.text + "to2", length = old.text.length)
                    }
                },
                migration(2, 3) {
                    migrateInput<Data2, Data1>(data2Serializer, data1Serializer) { old ->
                        Data1(text = old.text + "to3")
                    }
                }
            )
        )

        // Legacy data from the DB, still at version 1.
        val oldInputBytes = data1Serializer.encodeToBytes(Data1(""))
        val dummyTask = createTask(
            identifier = "test_task",
            version = 1,
            inputData = oldInputBytes,
        )

        val migrator = TaskMigrator()

        // 2. AKCE (Act)
        val result = migrator.migrate(dummyTask, registration)

        // 3. ASSERT
        assertNotNull(result, "Migration result must not be null")
        assertEquals(3, result.version, "Version must be 3") // moved all the way to the target

        // Both steps ran: (10 * 2) + 5 = 25.
        val finalInput = result.input as Data1
        assertEquals("to2to3", finalInput.text)
    }

    @Test
    fun `migrate should retain previously migrated fields if next step ignores them`() {
        val registration = taskHandlerRegistration(
            currentVersion = 3,
            inputSerializer = data1Serializer,
            outputSerializer = data1Serializer,
            progressSerializer = data1Serializer,
            migrations = listOf(
                migration(1, 2) {
                    migrateInput<Data1, Data1>(data1Serializer, data1Serializer) { old ->
                        Data1(text = "V2")
                    }
                },
                migration(2, 3) {
                    migrateOutput<Data1, Data1>(data1Serializer, data1Serializer) { old ->
                        Data1(text = "V3")
                    }
                }
            )
        )

        val dummyTask = createTask(
            identifier = "test_task",
            version = 1,
            inputData = data1Serializer.encodeToBytes(Data1("V1")),
            outputData = data1Serializer.encodeToBytes(Data1("V1")),
        )
        val result = TaskMigrator().migrate(dummyTask, registration)

        assertNotNull(result)

        val finalInput = result.input as Data1
        assertEquals("V2", finalInput.text)

        val finalOutput = result.output as Data1
        assertEquals("V3", finalOutput.text)
    }

    @Test
    fun `migrate should return null if task version matches current registration version`() {
        val registration = taskHandlerRegistration(
            currentVersion = 3,
            inputSerializer = data1Serializer,
            outputSerializer = data1Serializer,
            progressSerializer = data1Serializer,
            migrations = listOf(
                migration(1, 2) {
                    migrateInput<Data1, Data1>(data1Serializer, data1Serializer) { old ->
                        Data1(text = "V2")
                    }
                },
                migration(2, 3) {
                    migrateOutput<Data1, Data1>(data1Serializer, data1Serializer) { old ->
                        Data1(text = "V3")
                    }
                }
            )
        )

        // The task in the DB is already at version 2.
        val dummyTask = createTask(
            version = 3,
            identifier = "test_task",
            inputData = data1Serializer.encodeToBytes(Data1("V1"))
        )

        val result = TaskMigrator().migrate(dummyTask, registration)

        // Expect null: the evaluator knows there is nothing to write and parses directly.
        assertNull(result)
    }
}*/
package eu.tintera.background.tasks

import eu.tintera.background.tasks.compat.plus
import eu.tintera.background.tasks.compat.sum
import eu.tintera.background.tasks.compat.taskDataOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskDataTest {
    @Test
    fun `Data merging prioritizes task input over new parents and new parents over old parents`() {
        // Arrange
        val originalInput = taskDataOf("path" to "original", "retries" to 0)

        val oldParentData = taskDataOf("path" to "old_parent", "old_flag" to true)
        val newParentData = taskDataOf("path" to "new_parent", "new_flag" to true)

        // Mirrors the order TaskProcessor produces.
        val sortedParentsData = listOf(oldParentData, newParentData)

        // Act
        val mergedParents = sortedParentsData.sum()
        val finalData = mergedParents + originalInput

        // Assert
        assertEquals("original", finalData.getString("path")) // the task input always wins
        assertEquals(true, finalData.getBoolean("new_flag"))  // data from the newer parent survived
        assertEquals(true, finalData.getBoolean("old_flag"))  // non-conflicting data from the older parent survived
        assertEquals(0, finalData.getInt("retries"))
    }
}
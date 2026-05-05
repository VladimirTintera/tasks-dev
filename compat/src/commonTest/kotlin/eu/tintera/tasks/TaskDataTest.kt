package eu.tintera.tasks

import eu.tintera.tasks.compat.plus
import eu.tintera.tasks.compat.sum
import eu.tintera.tasks.compat.taskDataOf
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskDataTest {
    @Test
    fun `Data merging prioritizes task input over new parents, and new parents over old parents`() {
        // Arrange
        val originalInput = taskDataOf("path" to "original", "retries" to 0)

        val oldParentData = taskDataOf("path" to "old_parent", "old_flag" to true)
        val newParentData = taskDataOf("path" to "new_parent", "new_flag" to true)

        // Simulace toho, jak to seřadí TaskProcessor
        val sortedParentsData = listOf(oldParentData, newParentData)

        // Act
        val mergedParents = sortedParentsData.sum()
        val finalData = mergedParents + originalInput

        // Assert
        assertEquals("original", finalData.getString("path")) // Input tasku má absolutní přednost
        assertEquals(true, finalData.getBoolean("new_flag"))  // Data z nového rodiče přežila
        assertEquals(true, finalData.getBoolean("old_flag"))  // Data ze starého, co nebyla v konfliktu, přežila
        assertEquals(0, finalData.getInt("retries"))
    }
}
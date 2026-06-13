package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.TaskInfoQuery

fun TaskInfoQuery.isEmpty() = ids.isEmpty()
        && uniqueNames.isEmpty()
        && tags.isEmpty()
        && states.isEmpty()
package eu.tintera.tasks.core

import eu.tintera.tasks.TaskInfoQuery

fun TaskInfoQuery.isEmpty() = ids.isEmpty()
        && uniqueNames.isEmpty()
        && tags.isEmpty()
        && states.isEmpty()
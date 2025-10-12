package com.github.jkjamies.newfeaturemodule.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Simple [Service] registered at the project level.
 *
 * Constructing the service logs a message using [thisLogger].
 */
@Service(Service.Level.PROJECT)
class MyProjectService {

    init {
        thisLogger().info("Project service initialized")
    }
}

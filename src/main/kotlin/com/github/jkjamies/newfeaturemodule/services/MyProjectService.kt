package com.github.jkjamies.newfeaturemodule.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.PROJECT)
class MyProjectService {

    init {
        thisLogger().info("Project service initialized")
    }

    fun getRandomNumber() = (1..100).random()
}

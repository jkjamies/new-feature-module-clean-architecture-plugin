package com.github.jkjamies.newfeaturemodule.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger

@Service(Service.Level.APP)
class MyApplicationService {

    init {
        thisLogger().info("Application service initialized")
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()
}

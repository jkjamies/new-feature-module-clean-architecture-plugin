package com.github.jkjamies.cammp.feature.presentationgenerator.ui

import com.github.jkjamies.cammp.feature.cleanarchitecture.util.GradlePathUtil
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/**
 * Dialog to gather inputs for generating a Presentation Screen.
 */
class GenerateScreenDialog(private val project: Project) : DialogWrapper(project) {
    private val dirField = TextFieldWithBrowseButton(JBTextField())
    private val nameField = JBTextField()

    private val cbFlow = JBCheckBox("Use Flow StateHolder")
    private val cbScreen = JBCheckBox("Use Screen StateHolder")
    private val cbNavigation = JBCheckBox("Add Navigation Destination (Navigation Compose)")

    // UI Architecture pattern selection (placed before DI section)
    private val rbMvvm = JBRadioButton("MVVM", false)
    private val rbMvi = JBRadioButton("MVI", true)
    private val patternGroup = ButtonGroup().apply {
        add(rbMvvm)
        add(rbMvi)
    }

    private val cbEnableDi = JBCheckBox("Enable Dependency Injection", true)
    private val rbHilt = JBRadioButton("Hilt", true)
    private val rbKoin = JBRadioButton("Koin")
    private val cbKoinAnnotations = JBCheckBox("Koin Annotations")

    // Domain UseCases scanning UI (across entire project)
    private val useCasesPanel = JPanel().apply { layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS) }
    private val useCasesScroll = JBScrollPane(useCasesPanel).apply {
        preferredSize = JBUI.size(600, 160)
        minimumSize = JBUI.size(400, 120)
        border = JBUI.Borders.empty()
    }
    private val useCaseCheckboxes = mutableListOf<Pair<JBCheckBox, UseCaseItem>>()

    // Track whether the user has interacted to control when to show validation errors
    private var userInteracted: Boolean = false

    init {
        title = "Generate Presentation Screen"
        isResizable = true
        init()

        // Default the directory field to the current project so chooser opens there
        project.basePath?.let { dirField.text = it }

        // Configure directory chooser scoped to project base
        run {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            val basePath = project.basePath
            if (basePath != null) {
                val baseVf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
                if (baseVf != null) descriptor.withRoots(baseVf)
            }
            dirField.addActionListener {
                val currentText = dirField.text
                val toSelectPath = currentText.ifBlank { project.basePath }
                val toSelect = if (toSelectPath != null) VfsUtil.findFile(Paths.get(toSelectPath).normalize(), true) else null
                val file = FileChooser.chooseFile(descriptor, project, toSelect)
                if (file != null) {
                    dirField.text = file.path
                    userInteracted = true
                    refreshUseCases()
                    updateOkAndError()
                }
            }
        }

        // Live validation on directory field changes
        dirField.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                refreshUseCases()
                updateOkAndError()
            }
        })

        // Live validation on screen name changes
        nameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                userInteracted = true
                updateOkAndError()
            }
        })

        val group = ButtonGroup().apply {
            add(rbHilt)
            add(rbKoin)
        }
        // Toggle DI options visibility/enabled states
        cbEnableDi.addItemListener { updateDiControls() }
        rbKoin.addItemListener { updateDiControls() }
        rbHilt.addItemListener { updateDiControls() }
        // Initialize DI controls state
        updateDiControls()

        // Initial scanning of usecases across the project
        refreshUseCases()

        // Initialize validation state immediately (OK disabled if invalid; no error text until user interacts)
        updateOkAndError()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        // Make the dialog wider (approximately 2–3x)
        panel.preferredSize = JBUI.size(900, 540)
        val form = JPanel(GridBagLayout())
        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            gridy = 0
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
        }

        fun row(label: String, comp: JComponent) {
            c.gridx = 0; c.weightx = 0.0
            form.add(JBLabel(label), c)
            c.gridx = 1; c.weightx = 1.0
            form.add(comp, c)
            c.gridy += 1
        }

        row("Directory (desired presentation module):", dirField)
        row("Screen name (e.g., HomeScreen):", nameField)

        // Options
        c.gridx = 0; c.gridwidth = 2
        form.add(cbFlow, c); c.gridy += 1
        form.add(cbScreen, c); c.gridy += 1
        form.add(cbNavigation, c); c.gridy += 1

        // UI Architecture Pattern title + options below (full width), order: MVI then MVVM
        c.gridx = 0; c.gridwidth = 2
        form.add(JBLabel("UI Architecture Pattern:"), c); c.gridy += 1
        val patternOptions = JBPanel<JBPanel<*>>()
        patternOptions.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        patternOptions.add(rbMvi)
        patternOptions.add(rbMvvm)
        c.gridx = 0; c.gridwidth = 2
        form.add(patternOptions, c); c.gridy += 1

        // Spacer before UseCase list section
        val spacerUseCases = JBPanel<JBPanel<*>>()
        spacerUseCases.preferredSize = JBUI.size(1, JBUI.scale(12))
        c.gridx = 0; c.gridwidth = 2
        form.add(spacerUseCases, c); c.gridy += 1

        // Domain UseCases selection (across project)
        c.gridx = 0; c.gridwidth = 2
        form.add(JBLabel("Domain UseCases (across project):"), c); c.gridy += 1
        c.gridx = 0; c.gridwidth = 2
        form.add(useCasesScroll, c); c.gridy += 1

        // Dependency Injection title + options below (full width)
        c.gridx = 0; c.gridwidth = 2
        form.add(JBLabel("Dependency Injection:"), c); c.gridy += 1
        val diEnablePanel = JBPanel<JBPanel<*>>()
        diEnablePanel.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        diEnablePanel.add(cbEnableDi)
        c.gridx = 0; c.gridwidth = 2
        form.add(diEnablePanel, c); c.gridy += 1
        val diRow = JBPanel<JBPanel<*>>()
        diRow.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(8), 0)
        diRow.add(rbHilt)
        diRow.add(rbKoin)
        diRow.add(cbKoinAnnotations)
        c.gridx = 0; c.gridwidth = 2
        form.add(diRow, c); c.gridy += 1

        panel.add(form, BorderLayout.CENTER)
        return panel
    }

    private fun updateDiControls() {
        val enabled = cbEnableDi.isSelected
        rbHilt.isEnabled = enabled
        rbKoin.isEnabled = enabled
        val showKoinAnn = enabled && rbKoin.isSelected
        cbKoinAnnotations.isEnabled = showKoinAnn
        cbKoinAnnotations.isVisible = showKoinAnn
        cbKoinAnnotations.revalidate()
        cbKoinAnnotations.repaint()
    }

    // Live validation helper: disable OK when invalid; show error only after interaction
    private fun updateOkAndError() {
        val vi = doValidate()
        isOKActionEnabled = vi == null
        // Only show error text after the user interacts (types or chooses)
        if (userInteracted) {
            setErrorText(vi?.message, vi?.component)
        } else {
            setErrorText(null)
        }
    }

    override fun doValidate(): ValidationInfo? {
        val dir = dirField.text.trim()
        if (dir.isEmpty()) return ValidationInfo("Please choose a directory", dirField)
        // Require selecting a presentation module directory
        run {
            val last = try {
                Paths.get(dir).fileName?.toString()
            } catch (t: Throwable) {
                null
            }
            if (!"presentation".equals(last, ignoreCase = true)) {
                return ValidationInfo("You must select a presentation module directory", dirField)
            }
        }
        val name = nameField.text.trim()
        if (name.isEmpty()) return ValidationInfo("Please enter a screen name", nameField)
        if (!name.first().isLetter()) return ValidationInfo("Screen name should start with a letter", nameField)
        return null
    }

    fun getTargetDir(): String = dirField.text.trim()
    fun getScreenName(): String = nameField.text.trim()

    fun isNavigationSelected(): Boolean = cbNavigation.isSelected
    fun isFlowStateHolderSelected(): Boolean = cbFlow.isSelected
    fun isScreenStateHolderSelected(): Boolean = cbScreen.isSelected

    enum class DiChoice { HILT, KOIN }
    fun getDiChoice(): DiChoice = if (rbKoin.isSelected) DiChoice.KOIN else DiChoice.HILT
    fun isKoinAnnotationsSelected(): Boolean = cbKoinAnnotations.isSelected

    enum class PatternChoice { MVI, MVVM }
    fun getPatternChoice(): PatternChoice = if (rbMvvm.isSelected) PatternChoice.MVVM else PatternChoice.MVI

    private data class UseCaseItem(
        val moduleGradlePath: String,
        val fqn: String
    )

    private fun refreshUseCases() {
        useCaseCheckboxes.clear()
        useCasesPanel.removeAll()

        val basePath = project.basePath
        if (basePath == null) {
            useCasesPanel.add(JBLabel("Project base path not found"))
            useCasesPanel.revalidate(); useCasesPanel.repaint()
            return
        }
        val baseVf = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath)
        if (baseVf == null || !baseVf.isDirectory) {
            useCasesPanel.add(JBLabel("Project base directory not found"))
            useCasesPanel.revalidate(); useCasesPanel.repaint()
            return
        }

        // Scan entire project for *UseCase.kt files and group by Gradle module directory
        val itemsByModule = linkedMapOf<String, MutableList<UseCaseItem>>()
        val ignoreDirs = setOf(".git", ".gradle", ".idea", "build", "out", "gradle")

        fun findModuleDir(vf: VirtualFile): VirtualFile? {
            var cur: VirtualFile? = vf
            while (cur != null && cur != baseVf) {
                if (cur.findChild("build.gradle.kts") != null || cur.findChild("build.gradle") != null) return cur
                cur = cur.parent
            }
            // Also check root
            return if (baseVf.findChild("build.gradle.kts") != null || baseVf.findChild("build.gradle") != null) baseVf else null
        }

        fun toGradlePath(moduleDir: VirtualFile): String {
            val base = Paths.get(baseVf.path).normalize()
            val mod = Paths.get(moduleDir.path).normalize()
            val rel: Path = try { base.relativize(mod) } catch (t: Throwable) { return ":" }
            val segs = rel.toString().replace('\\', '/').split('/').filter { it.isNotBlank() }
            return ":" + segs.joinToString(":")
        }

        fun findKotlinRoot(file: VirtualFile): VirtualFile? {
            var p = file.parent
            while (p != null && p != baseVf) {
                val path = p.path.replace('\\', '/')
                if (path.endsWith("/src/main/kotlin") || path.endsWith("/src/commonMain/kotlin")) return p
                p = p.parent
            }
            return null
        }

        fun deriveFqn(file: VirtualFile): String {
            val kotlinRoot = findKotlinRoot(file)
            val simple = file.name.removeSuffix(".kt")
            if (kotlinRoot != null) {
                val rootPath = kotlinRoot.path.trimEnd('/') + "/"
                val rel = file.path.substringAfter(rootPath)
                val pkgPath = rel.substringBeforeLast('/', "").removeSuffix("/")
                val pkg = pkgPath.replace('/', '.')
                return if (pkg.isBlank()) simple else "$pkg.$simple"
            }
            return simple
        }

        fun traverse(dir: VirtualFile) {
            if (!dir.isDirectory) return
            if (dir.name in ignoreDirs || dir.name.startsWith('.')) return
            dir.children.forEach { child ->
                if (child.isDirectory) {
                    traverse(child)
                } else if (child.name.endsWith("UseCase.kt")) {
                    val moduleDir = findModuleDir(child) ?: baseVf
                    val gradlePath = toGradlePath(moduleDir)
                    val fqn = deriveFqn(child)
                    itemsByModule.computeIfAbsent(gradlePath) { mutableListOf() }
                        .add(UseCaseItem(moduleGradlePath = gradlePath, fqn = fqn))
                }
            }
        }

        traverse(baseVf)

        if (itemsByModule.isEmpty()) {
            useCasesPanel.add(JBLabel("No domain UseCases found across project"))
        } else {
            itemsByModule.toSortedMap().forEach { (modulePath, items) ->
                val header = JBLabel("$modulePath:")
                header.border = JBUI.Borders.empty(6, 0, 2, 0)
                useCasesPanel.add(header)
                items.sortedBy { it.fqn }.forEach { item ->
                    val cb = JBCheckBox(item.fqn)
                    useCaseCheckboxes += cb to item
                    useCasesPanel.add(cb)
                }
            }
        }

        useCasesPanel.revalidate(); useCasesPanel.repaint()
    }

    /**
     * Returns FQNs of selected UseCases across the project.
     */
    fun getSelectedUseCaseFqns(): List<String> = useCaseCheckboxes
        .filter { it.first.isSelected }
        .map { it.second.fqn }

    /**
     * Returns the distinct Gradle module paths (e.g., :features:orders:domain) for selected usecases.
     */
    fun getSelectedUseCaseGradlePaths(): Set<String> = useCaseCheckboxes
        .filter { it.first.isSelected }
        .map { it.second.moduleGradlePath }
        .toSet()
}

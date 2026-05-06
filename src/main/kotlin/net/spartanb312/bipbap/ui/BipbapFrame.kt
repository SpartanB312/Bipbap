package net.spartanb312.bipbap.ui

import net.spartanb312.bipbap.SUBTITLE
import net.spartanb312.bipbap.VERSION
import net.spartanb312.bipbap.cli.Preset
import net.spartanb312.bipbap.cli.applyPreset
import net.spartanb312.bipbap.config.BooleanValue
import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.config.FloatValue
import net.spartanb312.bipbap.config.IntValue
import net.spartanb312.bipbap.config.ListValue
import net.spartanb312.bipbap.config.StringValue
import net.spartanb312.bipbap.process.Transformers
import net.spartanb312.bipbap.process.impls.CodeOptimizer
import net.spartanb312.bipbap.process.impls.ConstantEncryptor
import net.spartanb312.bipbap.process.impls.HWIDAuthenticator
import net.spartanb312.bipbap.process.impls.InvokeDynamics
import net.spartanb312.bipbap.process.impls.MembersRenamer
import net.spartanb312.bipbap.process.impls.Miscellaneous
import net.spartanb312.bipbap.runInstance
import net.spartanb312.bipbap.utils.logging.Logger
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.UIManager
import javax.swing.WindowConstants
import javax.swing.DefaultListModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal fun launchUi() {
    SwingUtilities.invokeLater {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        BipbapFrame().isVisible = true
    }
}

private class BipbapFrame : JFrame("Bipbap $VERSION") {

    private val tabs = JTabbedPane()
    private val inputField = JTextField(Configs.Settings.input)
    private val outputField = JTextField(Configs.Settings.output)
    private val librariesModel = DefaultListModel<String>()
    private val exclusionsArea = JTextArea(Configs.Settings.exclusions.joinToString(System.lineSeparator()))
    private val threadsSpinner = JSpinner(SpinnerNumberModel(Configs.Settings.threads, 1, 256, 1))
    private val allProcessorsCheckBox = JCheckBox("Use all processors")

    private val lowPreset = JRadioButton("Low", true)
    private val midPreset = JRadioButton("Medium")
    private val highPreset = JRadioButton("High")
    private val codeOptimizerBox = JCheckBox("Code Optimizer")
    private val constantEncryptorBox = JCheckBox("Constant Encryptor")
    private val invokeDynamicsBox = JCheckBox("Invoke Dynamics")
    private val membersRenamerBox = JCheckBox("Members Renamer")
    private val hideCodeBox = JCheckBox("Hide Code")
    private val watermarkBox = JCheckBox("Watermark")
    private val crasherBox = JCheckBox("Crasher")
    private val localVarBox = JCheckBox("Local")
    private val fieldBox = JCheckBox("Field")
    private val methodBox = JCheckBox("Method")
    private lateinit var presetPanel: JPanel

    private val authEnabledBox = JCheckBox("Enable HWID Auth")
    private val authUrlField = JTextField(HWIDAuthenticator.onlineURL)
    private val authKeyField = JTextField(HWIDAuthenticator.encryptKey)
    private val authPoolsSpinner = JSpinner(SpinnerNumberModel(HWIDAuthenticator.pools, 1, 256, 1))
    private val showHwidBox = JCheckBox("Show HWID", HWIDAuthenticator.showHWIDWhenFailed)

    private val consoleArea = JTextArea()
    private val statusLabel = JLabel("Ready")
    private val progressBar = JProgressBar()
    private val autoScrollBox = JCheckBox("Auto scroll", true)
    private val obfuscateButton = JButton("Obfuscate")
    private val stopButton = JButton("Stop")
    private val consoleBuffer = StringBuilder()
    private val advancedEnabledBox = JCheckBox("Enable Advanced Configuration")
    private lateinit var advancedControlsPanel: JPanel
    private val advancedBindings = mutableListOf<AdvancedBinding>()
    private var worker: SwingWorker<Unit, String>? = null

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        minimumSize = Dimension(920, 620)
        setSize(980, 680)
        setLocationRelativeTo(null)

        contentPane.layout = BorderLayout()
        contentPane.add(createHeader(), BorderLayout.NORTH)
        contentPane.add(tabs, BorderLayout.CENTER)

        tabs.addTab("Configuration", createConfigurationPage())
        tabs.addTab("Advanced", createAdvancedPage())
        tabs.addTab("Console", createConsolePage())
        applyPresetToUi(Preset.LOW)
        runCatching {
            applyUiToRuntime(applyAdvanced = false)
            syncAdvancedFromRuntime()
        }
    }

    private fun createHeader(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            )
            add(JPanel(GridBagLayout()).apply {
                isOpaque = false
                add(JLabel("Bipbap").apply {
                    font = font.deriveFont(java.awt.Font.BOLD, 18f)
                }, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 0
                    anchor = GridBagConstraints.WEST
                })
                add(JLabel("$VERSION  $SUBTITLE").apply {
                    foreground = UIManager.getColor("Label.disabledForeground")
                }, GridBagConstraints().apply {
                    gridx = 0
                    gridy = 1
                    anchor = GridBagConstraints.WEST
                    insets = Insets(2, 0, 0, 0)
                })
            }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
                isOpaque = false
                add(button("Load") { loadConfig() })
                add(button("Save") { saveConfig() })
                add(button("Generate") { generateConfig() })
            }, BorderLayout.EAST)
        }
    }

    private fun createConfigurationPage(): JPanel {
        val page = JPanel(GridBagLayout())
        page.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        page.add(createProjectPanel(), constraints(0, 0, 0.55, 0.45))
        presetPanel = createPresetPanel()
        page.add(presetPanel, constraints(1, 0, 0.45, 0.45))
        page.add(createRuntimePanel(), constraints(0, 1, 0.55, 0.55))
        page.add(createAuthPanel(), constraints(1, 1, 0.45, 0.55))
        return page
    }

    private fun createProjectPanel(): JPanel = titledPanel("Project") {
        addRow("Input Jar", fileInput(inputField, false))
        addRow("Output Jar", fileInput(outputField, true))
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(button("+ Add") { chooseFiles("Add Libraries", true) { librariesModel.addElement(it.absolutePath) } })
            add(button("- Remove") { librariesList.selectedValuesList.forEach { librariesModel.removeElement(it) } })
        }
        addRow("Libraries", buttons)
        addFull(JScrollPane(librariesList).apply { preferredSize = Dimension(320, 96) })
    }

    private val librariesList = JList(librariesModel).apply {
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }

    private fun createPresetPanel(): JPanel = titledPanel("Preset") {
        ButtonGroup().apply {
            add(lowPreset)
            add(midPreset)
            add(highPreset)
        }
        val presetRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(lowPreset)
            add(midPreset)
            add(highPreset)
        }
        addFull(presetRow)
        lowPreset.addActionListener { selectPreset(Preset.LOW) }
        midPreset.addActionListener { selectPreset(Preset.MID) }
        highPreset.addActionListener { selectPreset(Preset.HIGH) }
        addFull(codeOptimizerBox)
        addFull(constantEncryptorBox)
        addFull(invokeDynamicsBox)
        addFull(membersRenamerBox)
        addFull(JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JLabel("Renamer:"))
            add(localVarBox)
            add(fieldBox)
            add(methodBox)
        })
        addFull(JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JLabel("Misc:"))
            add(hideCodeBox)
            add(watermarkBox)
            add(crasherBox)
        })
    }

    private fun createRuntimePanel(): JPanel = titledPanel("Runtime") {
        allProcessorsCheckBox.addActionListener {
            if (allProcessorsCheckBox.isSelected) {
                threadsSpinner.value = Runtime.getRuntime().availableProcessors()
                threadsSpinner.isEnabled = false
            } else {
                threadsSpinner.isEnabled = true
            }
        }
        addRow("Threads", JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(threadsSpinner)
            add(allProcessorsCheckBox)
        })
        exclusionsArea.rows = 8
        addRow("Exclusions", JScrollPane(exclusionsArea).apply { preferredSize = Dimension(320, 150) })
    }

    private fun createAuthPanel(): JPanel = titledPanel("Authentication") {
        addFull(authEnabledBox)
        addRow("Online URL", authUrlField)
        addRow("Encrypt Key", authKeyField)
        addRow("Pools", authPoolsSpinner)
        addFull(showHwidBox)
    }

    private fun createConsolePage(): JPanel {
        consoleArea.isEditable = false
        consoleArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 13)
        progressBar.isStringPainted = true
        progressBar.string = "Idle"
        stopButton.isEnabled = false

        val toolbar = JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                add(obfuscateButton)
                add(stopButton)
            }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply {
                add(button("Copy") { copyConsole() })
                add(button("Export") { exportConsole() })
            }, BorderLayout.EAST)
        }

        obfuscateButton.addActionListener { startObfuscation() }
        stopButton.addActionListener {
            worker?.cancel(true)
            statusLabel.text = "Stopping..."
        }

        return JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            add(toolbar, BorderLayout.NORTH)
            add(JScrollPane(consoleArea), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                add(statusLabel, BorderLayout.WEST)
                add(progressBar, BorderLayout.CENTER)
                add(autoScrollBox, BorderLayout.EAST)
            }, BorderLayout.SOUTH)
        }
    }

    private fun createAdvancedPage(): JPanel {
        advancedControlsPanel = JPanel()
        advancedControlsPanel.layout = BoxLayout(advancedControlsPanel, BoxLayout.Y_AXIS)
        advancedControlsPanel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        Transformers.forEach { transformer ->
            advancedControlsPanel.add(titledPanel(transformer.name) {
                transformer.getValues().forEach { value ->
                    when (value) {
                        is BooleanValue -> addAdvancedRow(value.name, BooleanAdvancedBinding(value))
                        is StringValue -> addAdvancedRow(value.name, StringAdvancedBinding(value))
                        is IntValue -> addAdvancedRow(value.name, IntAdvancedBinding(value))
                        is FloatValue -> addAdvancedRow(value.name, FloatAdvancedBinding(value))
                        is ListValue -> addAdvancedRow(value.name, ListAdvancedBinding(value))
                    }
                }
            })
            advancedControlsPanel.add(Box.createVerticalStrut(8))
        }
        advancedEnabledBox.addActionListener { switchAdvancedMode(advancedEnabledBox.isSelected) }
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            add(advancedEnabledBox, BorderLayout.NORTH)
            add(JScrollPane(advancedControlsPanel), BorderLayout.CENTER)
            updateAdvancedMode(false)
        }
    }

    private fun FormPanel.addAdvancedRow(name: String, binding: AdvancedBinding) {
        advancedBindings.add(binding)
        addRow(name, binding.component)
    }

    private fun applyPresetToUi(preset: Preset) {
        when (preset) {
            Preset.HIGH -> {
                codeOptimizerBox.isSelected = true
                constantEncryptorBox.isSelected = true
                invokeDynamicsBox.isSelected = true
                membersRenamerBox.isSelected = true
                localVarBox.isSelected = true
                fieldBox.isSelected = true
                methodBox.isSelected = true
                hideCodeBox.isSelected = true
                watermarkBox.isSelected = true
                crasherBox.isSelected = true
            }

            Preset.MID -> {
                codeOptimizerBox.isSelected = true
                constantEncryptorBox.isSelected = true
                invokeDynamicsBox.isSelected = true
                membersRenamerBox.isSelected = true
                localVarBox.isSelected = true
                fieldBox.isSelected = true
                methodBox.isSelected = true
                hideCodeBox.isSelected = true
                watermarkBox.isSelected = true
                crasherBox.isSelected = false
            }

            Preset.LOW -> {
                codeOptimizerBox.isSelected = true
                constantEncryptorBox.isSelected = true
                invokeDynamicsBox.isSelected = false
                membersRenamerBox.isSelected = true
                localVarBox.isSelected = true
                fieldBox.isSelected = false
                methodBox.isSelected = false
                hideCodeBox.isSelected = true
                watermarkBox.isSelected = true
                crasherBox.isSelected = false
            }
        }
    }

    private fun selectPreset(preset: Preset) {
        applyPresetToUi(preset)
        runCatching {
            applyUiToRuntime(applyAdvanced = false)
            syncAdvancedFromRuntime()
        }
    }

    private fun selectedPreset(): Preset = when {
        highPreset.isSelected -> Preset.HIGH
        midPreset.isSelected -> Preset.MID
        else -> Preset.LOW
    }

    private fun startObfuscation() {
        tabs.selectedIndex = tabs.indexOfTab("Console")
        clearConsoleForNextRun()
        try {
            applyUiToRuntime()
        } catch (exception: IllegalArgumentException) {
            appendConsole("ERROR: ${exception.message}${System.lineSeparator()}")
            return
        }

        setRunning(true)
        worker = object : SwingWorker<Unit, String>() {
            private var oldOut: PrintStream? = null
            private var oldErr: PrintStream? = null

            override fun doInBackground() {
                oldOut = System.out
                oldErr = System.err
                val stream = PrintStream(ConsoleOutputStream({ publish(it) }, oldOut), true)
                System.setOut(stream)
                System.setErr(stream)
                Thread.currentThread().name = "Obfuscate"
                runInstance()
            }

            override fun process(chunks: List<String>) {
                chunks.forEach { appendConsole(it) }
            }

            override fun done() {
                oldOut?.let { System.setOut(it) }
                oldErr?.let { System.setErr(it) }
                if (!isCancelled) {
                    runCatching { get() }.onFailure {
                        appendConsole("ERROR: ${it.message ?: it.javaClass.simpleName}${System.lineSeparator()}")
                        appendConsole(it.stackTraceText())
                    }
                }
                setRunning(false)
                statusLabel.text = if (isCancelled) "Cancelled" else "Ready"
            }
        }.also { it.execute() }
    }

    private fun applyUiToRuntime(applyAdvanced: Boolean = true) {
        val input = inputField.text.trim()
        if (input.isEmpty()) throw IllegalArgumentException("Input jar is required.")
        Configs.resetConfig()
        Configs.Settings.input = input
        Configs.Settings.output = outputField.text.trim().ifEmpty {
            input.dropLast(4) + "-obf.jar"
        }
        Configs.Settings.threads = (threadsSpinner.value as Number).toInt().coerceAtLeast(1)
        Configs.Settings.libraries = (0 until librariesModel.size()).map { librariesModel.getElementAt(it) }
        Configs.Settings.exclusions = exclusionsArea.text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (advancedEnabledBox.isSelected && applyAdvanced) {
            applyAdvancedValues(force = true)
        } else {
            applyPreset(selectedPreset())
            CodeOptimizer.enabled = codeOptimizerBox.isSelected
            ConstantEncryptor.enabled = constantEncryptorBox.isSelected
            InvokeDynamics.enabled = invokeDynamicsBox.isSelected
            MembersRenamer.enabled = membersRenamerBox.isSelected
            MembersRenamer.localVar = localVarBox.isSelected
            MembersRenamer.field = fieldBox.isSelected
            MembersRenamer.method = methodBox.isSelected
            Miscellaneous.enabled = hideCodeBox.isSelected || watermarkBox.isSelected || crasherBox.isSelected
            Miscellaneous.hideCode = hideCodeBox.isSelected
            Miscellaneous.watermark = watermarkBox.isSelected
            Miscellaneous.crasher = crasherBox.isSelected
            HWIDAuthenticator.enabled = authEnabledBox.isSelected
            HWIDAuthenticator.onlineMode = true
            HWIDAuthenticator.onlineURL = authUrlField.text.trim()
            HWIDAuthenticator.encryptKey = authKeyField.text.trim()
            HWIDAuthenticator.pools = (authPoolsSpinner.value as Number).toInt().coerceAtLeast(1)
            HWIDAuthenticator.showHWIDWhenFailed = showHwidBox.isSelected
        }
    }

    private fun setRunning(running: Boolean) {
        obfuscateButton.isEnabled = !running
        stopButton.isEnabled = running
        setChildrenEnabled(tabs.getComponentAt(tabs.indexOfTab("Configuration")), !running)
        setChildrenEnabled(tabs.getComponentAt(tabs.indexOfTab("Advanced")), !running)
        if (!running) updateAdvancedMode(advancedEnabledBox.isSelected)
        if (!running && allProcessorsCheckBox.isSelected) threadsSpinner.isEnabled = false
        progressBar.isIndeterminate = running
        progressBar.string = if (running) "Running" else "Idle"
        statusLabel.text = if (running) "Running..." else "Ready"
    }

    private fun clearConsoleForNextRun() {
        consoleBuffer.setLength(0)
        consoleArea.text = ""
    }

    private fun appendConsole(text: String) {
        consoleBuffer.append(text)
        consoleArea.append(text)
        if (autoScrollBox.isSelected) {
            consoleArea.caretPosition = consoleArea.document.length
        }
    }

    private fun copyConsole() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(consoleBuffer.toString()), null)
    }

    private fun exportConsole() {
        chooseSaveFile("Export Console", "bipbap-console.log") { file ->
            file.writeText(consoleBuffer.toString())
        }
    }

    private fun loadConfig() {
        chooseOpenFile("Load Config") { file ->
            Configs.resetConfig()
            Configs.loadConfig(file.absolutePath)
            advancedEnabledBox.isSelected = true
            syncUiFromRuntime()
            updateAdvancedMode(true)
            Logger.info("Loaded config: ${file.absolutePath}")
        }
    }

    private fun saveConfig() {
        try {
            applyUiToRuntime()
        } catch (exception: IllegalArgumentException) {
            JOptionPane.showMessageDialog(this, exception.message, "Invalid Configuration", JOptionPane.ERROR_MESSAGE)
            return
        }
        chooseSaveFile("Save Config", "config.json") { file ->
            Configs.saveConfig(file.absolutePath)
        }
    }

    private fun generateConfig() {
        chooseSaveFile("Generate Config", "config.json") { file ->
            Configs.resetConfig()
            Configs.saveConfig(file.absolutePath)
            syncUiFromRuntime()
        }
    }

    private fun syncUiFromRuntime(markAdvancedDirty: Boolean = false) {
        inputField.text = Configs.Settings.input
        outputField.text = Configs.Settings.output
        threadsSpinner.value = Configs.Settings.threads.coerceAtLeast(1)
        librariesModel.clear()
        Configs.Settings.libraries.forEach { librariesModel.addElement(it) }
        exclusionsArea.text = Configs.Settings.exclusions.joinToString(System.lineSeparator())
        codeOptimizerBox.isSelected = CodeOptimizer.enabled
        constantEncryptorBox.isSelected = ConstantEncryptor.enabled
        invokeDynamicsBox.isSelected = InvokeDynamics.enabled
        membersRenamerBox.isSelected = MembersRenamer.enabled
        hideCodeBox.isSelected = Miscellaneous.enabled && Miscellaneous.hideCode
        watermarkBox.isSelected = Miscellaneous.enabled && Miscellaneous.watermark
        crasherBox.isSelected = Miscellaneous.enabled && Miscellaneous.crasher
        localVarBox.isSelected = MembersRenamer.localVar
        fieldBox.isSelected = MembersRenamer.field
        methodBox.isSelected = MembersRenamer.method
        authEnabledBox.isSelected = HWIDAuthenticator.enabled
        authUrlField.text = HWIDAuthenticator.onlineURL
        authKeyField.text = HWIDAuthenticator.encryptKey
        authPoolsSpinner.value = HWIDAuthenticator.pools.coerceAtLeast(1)
        showHwidBox.isSelected = HWIDAuthenticator.showHWIDWhenFailed
        syncAdvancedFromRuntime(markAdvancedDirty)
    }

    private fun syncAdvancedFromRuntime(markDirty: Boolean = false) {
        advancedBindings.forEach { binding ->
            binding.refresh()
            if (markDirty) binding.markDirty()
        }
    }

    private fun applyAdvancedValues(force: Boolean) {
        advancedBindings.forEach { it.applyValue(force) }
    }

    private fun switchAdvancedMode(enabled: Boolean) {
        advancedEnabledBox.isSelected = enabled
        updateAdvancedMode(enabled)
        if (enabled) {
            runCatching {
                applyUiToRuntime(applyAdvanced = false)
                syncAdvancedFromRuntime()
            }
        } else {
            selectPreset(selectedPreset())
        }
    }

    private fun updateAdvancedMode(enabled: Boolean) {
        setChildrenEnabled(presetPanel, !enabled)
        advancedEnabledBox.isEnabled = true
        setChildrenEnabled(advancedControlsPanel, enabled)
    }

    private fun fileInput(field: JTextField, save: Boolean): JPanel {
        return JPanel(BorderLayout(6, 0)).apply {
            add(field, BorderLayout.CENTER)
            add(button("...") {
                if (save) chooseSaveFile("Select Output", field.text.ifEmpty { "output.jar" }) { field.text = it.absolutePath }
                else chooseOpenFile("Select Input") { file ->
                    field.text = file.absolutePath
                    if (outputField.text.isBlank() || outputField.text == Configs.Settings.output) {
                        outputField.text = file.absolutePath.dropLast(4) + "-obf.jar"
                    }
                }
            }, BorderLayout.EAST)
        }
    }

    private fun chooseFiles(title: String, multi: Boolean, onFile: (File) -> Unit) {
        JFileChooser().apply {
            dialogTitle = title
            isMultiSelectionEnabled = multi
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            if (showOpenDialog(this@BipbapFrame) == JFileChooser.APPROVE_OPTION) {
                if (multi) selectedFiles.forEach(onFile) else onFile(selectedFile)
            }
        }
    }

    private fun chooseOpenFile(title: String, onFile: (File) -> Unit) {
        JFileChooser().apply {
            dialogTitle = title
            if (showOpenDialog(this@BipbapFrame) == JFileChooser.APPROVE_OPTION) onFile(selectedFile)
        }
    }

    private fun chooseSaveFile(title: String, defaultName: String, onFile: (File) -> Unit) {
        JFileChooser().apply {
            dialogTitle = title
            selectedFile = File(defaultName)
            if (showSaveDialog(this@BipbapFrame) == JFileChooser.APPROVE_OPTION) onFile(selectedFile)
        }
    }

    private fun button(text: String, action: () -> Unit): JButton = JButton(text).apply {
        addActionListener { action() }
    }

    private fun titledPanel(title: String, block: FormPanel.() -> Unit): JPanel {
        return FormPanel().apply {
            border = BorderFactory.createTitledBorder(title)
            block()
        }
    }

    private fun constraints(x: Int, y: Int, wx: Double, wy: Double): GridBagConstraints {
        return GridBagConstraints().apply {
            gridx = x
            gridy = y
            weightx = wx
            weighty = wy
            fill = GridBagConstraints.BOTH
            insets = Insets(4, 4, 4, 4)
        }
    }
}

private fun setChildrenEnabled(component: Component, enabled: Boolean) {
    component.isEnabled = enabled
    if (component is java.awt.Container) {
        component.components.forEach { setChildrenEnabled(it, enabled) }
    }
}

private fun Throwable.stackTraceText(): String {
    val writer = StringWriter()
    printStackTrace(PrintWriter(writer))
    return writer.toString()
}

private interface AdvancedBinding {
    val component: Component
    fun refresh()
    fun markDirty()
    fun applyValue(force: Boolean)
}

private class BooleanAdvancedBinding(private val value: BooleanValue) : AdvancedBinding {
    private var dirty = false
    private val box = JCheckBox().apply {
        addActionListener { dirty = true }
    }

    override val component: Component = box

    override fun refresh() {
        box.isSelected = value.value
        dirty = false
    }

    override fun markDirty() {
        dirty = true
    }

    override fun applyValue(force: Boolean) {
        if (force || dirty) value.value = box.isSelected
    }
}

private class StringAdvancedBinding(private val value: StringValue) : AdvancedBinding {
    private var dirty = false
    private val field = JTextField().apply {
        document.addDocumentListener(dirtyListener { dirty = true })
    }

    override val component: Component = field

    override fun refresh() {
        field.text = value.value
        dirty = false
    }

    override fun markDirty() {
        dirty = true
    }

    override fun applyValue(force: Boolean) {
        if (force || dirty) value.value = field.text
    }
}

private class IntAdvancedBinding(private val value: IntValue) : AdvancedBinding {
    private var dirty = false
    private val spinner = JSpinner(SpinnerNumberModel(value.value, Int.MIN_VALUE, Int.MAX_VALUE, 1)).apply {
        addChangeListener { dirty = true }
    }

    override val component: Component = spinner

    override fun refresh() {
        spinner.value = value.value
        dirty = false
    }

    override fun markDirty() {
        dirty = true
    }

    override fun applyValue(force: Boolean) {
        if (force || dirty) value.value = (spinner.value as Number).toInt()
    }
}

private class FloatAdvancedBinding(private val value: FloatValue) : AdvancedBinding {
    private var dirty = false
    private val spinner = JSpinner(
        SpinnerNumberModel(value.value.toDouble(), -Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble(), 0.1)
    ).apply {
        addChangeListener { dirty = true }
    }

    override val component: Component = spinner

    override fun refresh() {
        spinner.value = value.value.toDouble()
        dirty = false
    }

    override fun markDirty() {
        dirty = true
    }

    override fun applyValue(force: Boolean) {
        if (force || dirty) value.value = (spinner.value as Number).toFloat()
    }
}

private class ListAdvancedBinding(private val value: ListValue) : AdvancedBinding {
    private var dirty = false
    private val area = JTextArea(4, 24).apply {
        document.addDocumentListener(dirtyListener { dirty = true })
    }
    override val component: Component = JScrollPane(area)

    override fun refresh() {
        area.text = value.value.joinToString(System.lineSeparator())
        dirty = false
    }

    override fun markDirty() {
        dirty = true
    }

    override fun applyValue(force: Boolean) {
        if (force || dirty) {
            value.value = area.text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }
    }
}

private fun dirtyListener(onDirty: () -> Unit): DocumentListener {
    return object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = onDirty()
        override fun removeUpdate(e: DocumentEvent) = onDirty()
        override fun changedUpdate(e: DocumentEvent) = onDirty()
    }
}

private class FormPanel : JPanel(GridBagLayout()) {
    private var row = 0

    fun addRow(label: String, component: Component) {
        add(JLabel(label), GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = Insets(4, 6, 4, 6)
        })
        add(component, GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 6, 4, 6)
        })
        row++
    }

    fun addFull(component: Component) {
        add(component, GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 6, 4, 6)
        })
        row++
    }
}

private class ConsoleOutputStream(
    private val onText: (String) -> Unit,
    private val mirror: PrintStream?
) : OutputStream() {

    override fun write(b: Int) {
        val text = b.toChar().toString()
        mirror?.print(text)
        onText(text)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        val text = String(b, off, len)
        mirror?.print(text)
        onText(text)
    }
}

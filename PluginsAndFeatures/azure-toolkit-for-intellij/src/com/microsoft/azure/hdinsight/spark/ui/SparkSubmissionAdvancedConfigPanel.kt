/**
 * Copyright (c) Microsoft Corporation
 *
 *
 * All rights reserved.
 *
 *
 * MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.icons.AllIcons.Actions.Help
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.InplaceButton
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.*
import com.intellij.uiDesigner.core.GridLayoutManager
import com.microsoft.azure.hdinsight.common.Docs
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UseKeyFile
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import org.apache.commons.lang3.StringUtils
import rx.subjects.PublishSubject
import java.awt.Component
import java.awt.event.ItemEvent.DESELECTED
import java.awt.event.ItemEvent.SELECTED
import java.io.File
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent

// No auto format for the following lines
data class Place(val component: Component, val gridConstraints: GridConstraints)

// View and Control combined class
class SparkSubmissionAdvancedConfigPanel: JPanel(), SettableControl<SparkSubmitAdvancedConfigModel> {

    private val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)

    // FIXME!!! Since the Intellij has no locale setting, just set en-us here.
    private val helpUrl: String = Docs(Locale.US).getDocUrlByTopic(Docs.TOPIC_CONNECT_HADOOP_LINUX_USING_SSH)

    private val sshUserNameTip = "Secure shell (SSH) user name used in Spark remote debugging, by default using `sshuser`"
    private val enableRemoteDebugTip = "Enable Spark remote debug, use with caution since this might override data previously generated"
    private val authTypeTip = "Secure shell (SSH) authentication type used in Spark remote debugging, by default using the password"
    private val usePasswordTip = "For secure shell (SSH) password, use the password specified here"
    private val useKeyFileToolTip = "For secure shell (SSH) password, use the key file specified here"

    private val enableRemoteDebugCheckBox = JCheckBox("Enable Spark remote debug", true).apply {
        toolTipText = enableRemoteDebugTip
        isSelected = false
    }

    private val sshUserNameLabel = JLabel("Secure Shell (SSH) User Name:").apply {
        toolTipText = sshUserNameTip
    }
    private val sshUserNameTextField = JTextField("sshuser").apply {
        toolTipText = sshUserNameTip
    }

    private val sshAuthTypeLabel = JLabel("Secure Shell (SSH) Auth Type").apply {
        toolTipText = authTypeTip
    }

    // Password inputs
    private val sshUsePasswordRadioButton = JRadioButton("Use SSH password:", false).apply {
        toolTipText = usePasswordTip
        isSelected = true
    }
    private val sshPasswordField = JPasswordField().apply {
        toolTipText = usePasswordTip
    }

    // Key file inputs
    private val sshUseKeyFileRadioButton = JRadioButton("Use private key file:", false).apply {
        toolTipText = useKeyFileToolTip
        isSelected = false
    }
    private val sshKeyFileTextField = TextFieldWithBrowseButton().apply {
        toolTipText = useKeyFileToolTip
    }

    // Group password and key file inputs
    private val sshAuthGroup = ButtonGroup().apply {
        add(sshUsePasswordRadioButton)
        add(sshUseKeyFileRadioButton)
    }

    internal val checkSshCertIndicator = BackgroundTaskIndicator("Verify SSH Authentication...")

    private val helpButton = InplaceButton(IconButton("Help about connection to HDInsight using SSH", Help)) {
        BrowserUtil.browse(helpUrl)
    }

    var isRemoteDebugEnabled: Boolean
        get() = enableRemoteDebugCheckBox.isSelected
        set(isEnabled) {
            enableRemoteDebugCheckBox.isSelected = isEnabled
        }

    private fun baseConstraints() = GridConstraints().apply { anchor = ANCHOR_WEST }

    private val colTemplate= listOf(
            // Column 0                           | Column 1                              | Column 2
            baseConstraints().apply { column = 0 }, baseConstraints().apply { column = 1 }, baseConstraints().apply { column = 2; hSizePolicy = SIZEPOLICY_WANT_GROW; fill = FILL_HORIZONTAL })

    private fun buildConstraints(colTemplateOffset: Int): GridConstraints = colTemplate[colTemplateOffset].clone() as GridConstraints

    private val layoutPlan = listOf(
            Place(enableRemoteDebugCheckBox, buildConstraints(0).apply { indent = 0;         row = 0 }), Place(helpButton, buildConstraints(1).apply { row = 0 }), Place(checkSshCertIndicator, buildConstraints(2).apply { row = 0 }),
                Place(sshUserNameLabel,          buildConstraints(0).apply { indent = 1;     row = 1 }), /* Empty Column 1, */ Place(sshUserNameTextField,  buildConstraints(2).apply { row = 1 }),
                Place(sshAuthTypeLabel,          buildConstraints(0).apply { indent = 1;     row = 2 }),
                    Place(sshUsePasswordRadioButton, buildConstraints(0).apply { indent = 2; row = 3 }), /* Empty Column 1, */ Place(sshPasswordField,      buildConstraints(2).apply { row = 3 }),
                    Place(sshUseKeyFileRadioButton,  buildConstraints(0).apply { indent = 2; row = 4 }), /* Empty Column 1, */ Place(sshKeyFileTextField,   buildConstraints(2).apply { row = 4 }))

    val sshCheckSubject: PublishSubject<String> = PublishSubject.create()

    init {
        setSshAuthenticationUIEnabled(false)

        // Add all components according to the layout plan
        layout = GridLayoutManager(layoutPlan.last().gridConstraints.row + 1, colTemplate.size)
        layoutPlan.forEach { (component, gridConstrains) -> add(component, gridConstrains) }

        // To enable/disable all options
        enableRemoteDebugCheckBox.addItemListener { setSshAuthenticationUIEnabled(it.stateChange == SELECTED) }

        // To trigger SSH authentication background check
        val inputListener = object : DocumentAdapter() {
            override fun textChanged(ev: DocumentEvent) = sshCheckSubject.onNext(ev.document.toString())
        }

        sshUserNameTextField.document.addDocumentListener(inputListener)
        sshPasswordField.document.addDocumentListener(inputListener)
        sshKeyFileTextField.textField.document.addDocumentListener(inputListener)

        sshUsePasswordRadioButton.addActionListener { sshCheckSubject.onNext(it.toString()) }
        sshUseKeyFileRadioButton.addActionListener { sshCheckSubject.onNext(it.toString()) }

        // To change SSH authentication type
        sshUsePasswordRadioButton.addItemListener { setSshPasswordInputEnabled(it.stateChange == SELECTED) }
        sshUseKeyFileRadioButton.addItemListener { setSshPasswordInputEnabled(it.stateChange == DESELECTED) }

        // To popup the key file chooser dialog
        sshKeyFileTextField.button.addActionListener { showSshKeyFileChooser() }
    }

    private fun setSshPasswordInputEnabled(isEnabled: Boolean) {
        sshPasswordField.isEnabled = isEnabled
        sshKeyFileTextField.isEnabled = !isEnabled
    }

    private fun setSshAuthenticationUIEnabled(isEnabled: Boolean) {
        sshUserNameTextField.isEnabled = isEnabled
        sshUserNameLabel.isEnabled = isEnabled
        sshAuthTypeLabel.isEnabled = isEnabled

        sshUsePasswordRadioButton.isEnabled = isEnabled
        sshUseKeyFileRadioButton.isEnabled = isEnabled

        sshPasswordField.isEnabled = isEnabled
        sshKeyFileTextField.isEnabled = isEnabled

        if (isEnabled) {
            val currentSelection = sshAuthGroup.selection
            sshAuthGroup.clearSelection()
            currentSelection.isSelected = true
        } else {
            checkSshCertIndicator.setTextAndStatus("", false)
            sshPasswordField.text = ""
            sshKeyFileTextField.text = ""
        }
    }

    val model: SparkSubmitAdvancedConfigModel
            get() = SparkSubmitAdvancedConfigModel().apply { getData(this) }

    private fun showSshKeyFileChooser() {
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .apply { title = "Select SSH Key File" }

        FileChooser.chooseFile(fileChooserDescriptor, null, null)?.run { sshKeyFileTextField.text = path }
    }

    override fun removeNotify() {
        super.removeNotify()

        // Stop the SSH authentication check
        sshCheckSubject.onCompleted()
    }

    override fun setData(data: SparkSubmitAdvancedConfigModel) {
        // Data -> Component
        val applyData: () -> Unit = {
            val current = model

            val password = (
                    if (StringUtils.isEmpty(current.sshPassword)) {
                        // Load password for no password input
                        try {
                            secureStore?.loadPassword(data.credentialStoreAccount, data.sshUserName)
                        } catch (ignored: Exception) {
                            null
                        }
                    } else {
                        null
                    }) ?: data.sshPassword

            if (current.sshPassword != password) {
                sshPasswordField.text = password ?: ""
            }

            if (data.sshKeyFile != null && data.sshKeyFile.exists()) {
                if ((current.sshKeyFile?.absolutePath ?: "") != data.sshKeyFile.absolutePath) {
                    sshKeyFileTextField.text = data.sshKeyFile.absolutePath
                }
            }

            if (current.sshAuthType != data.sshAuthType) {
                when (data.sshAuthType) {
                    UsePassword -> {
                        sshUsePasswordRadioButton.isSelected = true
                        sshUseKeyFileRadioButton.isSelected = false
                    }
                    else -> {
                        sshUsePasswordRadioButton.isSelected = false
                        sshUseKeyFileRadioButton.isSelected = true
                    }
                }
            }

            if (current.sshUserName != data.sshUserName && data.sshUserName != null && data.sshUserName.isNotEmpty()) {
                sshUserNameTextField.text = data.sshUserName
            }

            if (current.checkingMessage != data.checkingMessage || current.isChecking != data.isChecking) {
                checkSshCertIndicator.setTextAndStatus(data.checkingMessage, data.isChecking)
            }

            // Keep this line at the ending
            if (current.enableRemoteDebug != data.enableRemoteDebug) {
                enableRemoteDebugCheckBox.isSelected = data.enableRemoteDebug
            }
        }

        ApplicationManager.getApplication().invokeLater(applyData, ModalityState.any())
    }

    override fun getData(data: SparkSubmitAdvancedConfigModel) {
        // Component -> Data
        if (StringUtils.isNotBlank(String(sshPasswordField.password?: CharArray(0)))) {
            data.sshPassword = String(sshPasswordField.password)
        }

        if (StringUtils.isNotBlank(sshKeyFileTextField.text)) {
            val keyFile = File(sshKeyFileTextField.text)
            if (keyFile.exists() && !keyFile.isDirectory) {
                data.sshKeyFile = keyFile
            }
        }

        if (sshUsePasswordRadioButton.isSelected) {
            data.sshAuthType = UsePassword
        } else if (sshUseKeyFileRadioButton.isSelected) {
            data.sshAuthType = UseKeyFile
        }

        if (StringUtils.isNotBlank(sshUserNameTextField.text)) {
            data.sshUserName = sshUserNameTextField.text
        }

        // Keep this line at the ending
        data.enableRemoteDebug = enableRemoteDebugCheckBox.isSelected
    }
}

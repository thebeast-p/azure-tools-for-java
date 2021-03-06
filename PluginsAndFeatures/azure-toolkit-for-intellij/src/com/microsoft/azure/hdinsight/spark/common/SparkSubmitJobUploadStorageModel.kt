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

package com.microsoft.azure.hdinsight.spark.common

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStoragePanel
import javax.swing.DefaultComboBoxModel

@Tag("job_upload_storage")
class SparkSubmitJobUploadStorageModel {
    @Attribute("storage_account")
    var storageAccount: String? = null

    @Attribute("storage_key")
    var storageKey: String? = null

    @Transient var containersModel: DefaultComboBoxModel<String> = DefaultComboBoxModel()

    @Attribute("upload_path")
    var uploadPath: String? = null

    // selectedContainer is saved to reconstruct a containersModel when we reopen the project
    @Attribute("selected_container")
    var selectedContainer: String? = null

    @Attribute("storage_account_type")
    var storageAccountType: SparkSubmitStorageType = SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT

    @Transient var errorMsg: String? = SparkSubmissionJobUploadStoragePanel().notFinishCheckMessage
}
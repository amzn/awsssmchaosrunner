package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.CloudWatchOutputConfig
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentResult
import com.amazonaws.services.simplesystemsmanagement.model.DeleteDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.DocumentFormat
import com.amazonaws.services.simplesystemsmanagement.model.DocumentType
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest
import com.amazonaws.services.simplesystemsmanagement.model.Target

interface SSMAttack {
    val ssm: AWSSimpleSystemsManagement
    val documentContent: String

    fun documentName(): String? {
        return this::class.simpleName
    }

    fun start(timeoutSeconds: Int, targets: List<Target>, cloudWatchLogGroupName: String) {
        createCommandDocument(ssm, this.documentName(), this.documentContent)

        val request = SendCommandRequest()
                .withDocumentName(this.documentName())
                .withTargets(targets)
                .withCloudWatchOutputConfig(CloudWatchOutputConfig().withCloudWatchLogGroupName(cloudWatchLogGroupName))
                .withMaxConcurrency(fullConcurrency)
                .withTimeoutSeconds(timeoutSeconds)

        ssm.sendCommand(request)
    }

    fun stop() {
        val deleteDocumentRequest = DeleteDocumentRequest()
                .withName(this.documentName())
        ssm.deleteDocument(deleteDocumentRequest)
    }

    companion object {
        const val fullConcurrency = "100"
        private const val EC2TargetType = "/AWS::EC2::Instance"

        fun createCommandDocument(ssm: AWSSimpleSystemsManagement, documentName: String?, documentContent: String):
                CreateDocumentResult? {
            val request = CreateDocumentRequest()
                    .withDocumentFormat(DocumentFormat.YAML)
                    .withContent(documentContent)
                    .withDocumentType(DocumentType.Command)
                    .withTargetType(EC2TargetType)
                    .withName(documentName)
            return ssm.createDocument(request)
        }
    }
}

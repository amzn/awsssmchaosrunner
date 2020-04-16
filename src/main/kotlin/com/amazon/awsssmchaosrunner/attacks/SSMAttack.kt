package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.CancelCommandRequest
import com.amazonaws.services.simplesystemsmanagement.model.CloudWatchOutputConfig
import com.amazonaws.services.simplesystemsmanagement.model.Command
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentResult
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.DeleteDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.DocumentFormat
import com.amazonaws.services.simplesystemsmanagement.model.DocumentType
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest
import com.amazonaws.services.simplesystemsmanagement.model.Target

interface SSMAttack {
    val ssm: AWSSimpleSystemsManagement
    val configuration: AttackConfiguration
    val documentContent: String

    fun documentName(): String? {
        return this::class.simpleName
    }

    fun start(): Command {
        createCommandDocument(ssm, this.documentName(), this.documentContent)

        val request = SendCommandRequest()
                .withDocumentName(this.documentName())
                .withTargets(configuration.targets)
                .withCloudWatchOutputConfig(CloudWatchOutputConfig()
                        .withCloudWatchLogGroupName(configuration.cloudWatchLogGroupName)
                        .withCloudWatchOutputEnabled(true))
                .withMaxConcurrency("${configuration.concurrencyPercentage}%")
                .withTimeoutSeconds(configuration.timeoutSeconds)
        val sendCommandResult = ssm.sendCommand(request)
        return sendCommandResult.command
    }

    fun stop(command: Command) {
        val cancelCommandRequest = CancelCommandRequest().withCommandId(command.commandId)
        ssm.cancelCommand(cancelCommandRequest)
        val deleteDocumentRequest = DeleteDocumentRequest()
                .withName(this.documentName())
        ssm.deleteDocument(deleteDocumentRequest)
    }

    companion object {
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

        fun getAttack(ssm: AWSSimpleSystemsManagement, configuration: AttackConfiguration): SSMAttack = when (configuration.name) {
            "NetworkInterfaceLatencyAttack" -> NetworkInterfaceLatencyAttack(ssm, configuration)
            "DependencyLatencyAttack" -> DependencyLatencyAttack(ssm, configuration)
            "MemoryHogAttack" -> MemoryHogAttack(ssm, configuration)
            "CPUHogAttack" -> CPUHogAttack(ssm, configuration)
            "DiskHogAttack" -> DiskHogAttack(ssm, configuration)
            else -> throw NotImplementedError("${configuration.name} is not a valid SSMAttack")
        }

        data class AttackConfiguration(
            val name: String,
            val duration: String,
            val timeoutSeconds: Int,
            val cloudWatchLogGroupName: String,
            val targets: List<Target>,
            val concurrencyPercentage: Int,
            val otherParameters: Map<String, String>
        )
    }
}

package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.CancelCommandRequest
import com.amazonaws.services.simplesystemsmanagement.model.CloudWatchOutputConfig
import com.amazonaws.services.simplesystemsmanagement.model.Command
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.CreateDocumentResult
import com.amazonaws.services.simplesystemsmanagement.model.DeleteDocumentRequest
import com.amazonaws.services.simplesystemsmanagement.model.DocumentFormat
import com.amazonaws.services.simplesystemsmanagement.model.DocumentType
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest
import com.amazonaws.services.simplesystemsmanagement.model.Target

interface SSMAttack {
    val ssm: AWSSimpleSystemsManagement
    val configuration: AttackConfiguration
    val documentContent: String

    val timeUnitInMicroseconds: String
        get() = "us"
    val timeUnitInMilliseconds: String
        get() = "ms"
    val defaultJitterValue: String
        get() = "10"

    /**
     * SSM createDocument() needs unique document names for creation. Delete the existing SSM document from the SSM AWS UI.
     *
     * Use the AttackConfiguration 'documentNameSuffix' parameter if you need to create multiple SSM documents of the same type without
     * deleting the existing documents. For example: You need to run chaos test A for fleet X and at the same you need to run chaos test
     * B for fleet Y.
     * This is an uncommon use-case with potentially dangerous consequences, proceed with caution.
     */
    fun documentName(): String? {
        return this::class.simpleName + this.configuration.otherParameters.getOrDefault("documentNameSuffix", "")
    }

    fun start(): Command {
        createCommandDocument(ssm, this.documentName(), this.documentContent)

        val request = SendCommandRequest()
            .withDocumentName(this.documentName())
            .withTargets(configuration.targets)
            .withCloudWatchOutputConfig(
                CloudWatchOutputConfig()
                    .withCloudWatchLogGroupName(configuration.cloudWatchLogGroupName)
                    .withCloudWatchOutputEnabled(true)
            )
            .withMaxConcurrency("${configuration.concurrencyPercentage}%")
            .withTimeoutSeconds(configuration.timeoutSeconds)
        val sendCommandResult = ssm.sendCommand(request)
        return sendCommandResult.command
    }

    fun stop(command: Command) {
        val cancelCommandRequest = CancelCommandRequest().withCommandId(command.commandId)
        ssm.cancelCommand(cancelCommandRequest)
        val deleteDocumentRequest = DeleteDocumentRequest().withName(this.documentName())
        ssm.deleteDocument(deleteDocumentRequest)
    }

    fun getNetworkInterfaceLatency(): String {
        return if (configuration.otherParameters.containsKey("networkInterfaceLatencyUs"))
            configuration.otherParameters.get("networkInterfaceLatencyUs") + timeUnitInMicroseconds
        else configuration.otherParameters.get("networkInterfaceLatencyMs") + timeUnitInMilliseconds
    }

    fun getJitter(): String {
        return configuration.otherParameters.getOrDefault("networkInterfaceLatencyJitter", getDefaultJitter())
    }

    fun getDefaultJitter(): String {
        return if (configuration.otherParameters.containsKey("networkInterfaceLatencyUs"))
            defaultJitterValue + timeUnitInMicroseconds
        else defaultJitterValue + timeUnitInMilliseconds
    }

    companion object {
        private const val EC2TargetType = "/AWS::EC2::Instance"

        fun createCommandDocument(ssm: AWSSimpleSystemsManagement, documentName: String?, documentContent: String): CreateDocumentResult? {
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
            "DependencyPacketLossAttack" -> DependencyPacketLossAttack(ssm, configuration)
            "MultiIPAddressLatencyAttack" -> MultiIPAddressLatencyAttack(ssm, configuration)
            "MultiIPAddressPacketLossAttack" -> MultiIPAddressPacketLossAttack(ssm, configuration)
            "MemoryHogAttack" -> MemoryHogAttack(ssm, configuration)
            "CPUHogAttack" -> CPUHogAttack(ssm, configuration)
            "DiskHogAttack" -> DiskHogAttack(ssm, configuration)
            "AWSServiceLatencyAttack" -> AWSServiceLatencyAttack(ssm, configuration)
            "AWSServicePacketLossAttack" -> AWSServicePacketLossAttack(ssm, configuration)
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

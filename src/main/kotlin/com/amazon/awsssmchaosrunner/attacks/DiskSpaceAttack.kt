package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

private val log = KotlinLogging.logger { }

class DiskSpaceAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    override val requiredOtherParameters = arrayOf("diskPercent")

    private val directoryToOverload = configuration.otherParameters.getOrDefault("directory", "/tmp")
    private val diskPercentage = configuration.otherParameters.getOrDefault("diskPercent", "50")

    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Hog disk on the instance\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${this.documentName()}\n" +
                "  inputs:\n" +
                "    workingDirectory: $directoryToOverload\n" +
                "    runCommand:\n"
            val chaos = "    - df -P . | tail -1 | awk '{print \"((\"\$2\"*$diskPercentage)/100)-\"\$3 }' | bc | sed 's/\\..*//' | xargs -I{} sudo fallocate -l {}K disk_hog_file.tmp\n"
            val scheduledChaosRollback = "    - echo \"sudo rm disk_hog_file.tmp\" | " +
                "at now + ${Duration.parse(configuration.duration).toMinutes() + 1} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
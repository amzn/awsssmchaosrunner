package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

private val log = KotlinLogging.logger { }

class MemoryHogAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    private val vmWorkers = 8

    override val requiredOtherParameters = arrayOf("virtualMemoryPercent")

    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Hog virtual memory on the instance\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${this.documentName()}\n" +
                "  inputs:\n" +
                "    runCommand:\n" +
                "    - sudo yum -y install at\n" +
                "    - sudo systemctl start atd\n"
            // stress-ng package is available through amazon-linux-extras in Amazon Linux 2
            // first shell command is set to retun true always, as amazon-linux-extras is only available for AL2
            val chaos = "    - sudo amazon-linux-extras install testing || true\n" +
                "    - sudo yum -y install stress-ng\n" +
                "    - stress-ng --vm $vmWorkers --vm-bytes ${configuration.otherParameters["virtualMemoryPercent"]}% -t ${Duration.parse(configuration.duration).seconds}s\n"
            val scheduledChaosRollback = "    - echo \"sudo yum -y remove stress-ng\" | " +
                "at now + ${Duration.parse(configuration.duration).toMinutes() + 1} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
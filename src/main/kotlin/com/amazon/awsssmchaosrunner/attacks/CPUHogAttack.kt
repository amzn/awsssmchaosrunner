package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

class CPUHogAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    private val log = KotlinLogging.logger { }
    private val CPU_STRESSORS = 0 // Stress all available cores
    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                    "schemaVersion: '2.2'\n" +
                    "description: Hog CPU on the instance\n" +
                    "mainSteps:\n" +
                    "- action: aws:runShellScript\n" +
                    "  name: ${this.documentName()}\n" +
                    "  inputs:\n" +
                    "    runCommand:\n"
            val chaos = "    - sudo yum -y install stress-ng\n" +
                    "    - stress-ng --cpu $CPU_STRESSORS --cpu-load ${configuration.otherParameters["cpuLoadPercent"]}% " +
                    "--cpu-method matrixprod -t ${Duration.parse(configuration.duration).seconds}s\n"
            val scheduledChaosRollback = "    - echo \"sudo yum -y remove stress-ng\" | " +
                    "at now + ${Duration.parse(configuration.duration).toMinutes() + 1} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
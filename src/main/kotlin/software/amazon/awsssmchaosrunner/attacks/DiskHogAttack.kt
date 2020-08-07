// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

private val log = KotlinLogging.logger { }

class DiskHogAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {

    private val diskStressors = 0 // Stress all available cores
    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Hog disk on the instance\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${this.documentName()}\n" +
                "  inputs:\n" +
                "    runCommand:\n"
            val chaos = "    - sudo yum -y install stress-ng\n" +
                "    - stress-ng --iomix $diskStressors --iomix-bytes ${configuration.otherParameters["diskPercent"]}%" +
                " -t ${Duration.parse(configuration.duration).seconds}s\n"
            val scheduledChaosRollback = "    - echo \"sudo yum -y remove stress-ng\" | " +
                "at now + ${Duration.parse(configuration.duration).toMinutes() + 1} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

class MemoryHogAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    private val log = KotlinLogging.logger { }
    private val VM_WORKERS = 8
    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                    "schemaVersion: '2.2'\n" +
                    "description: Hog virtual memory on the instance\n" +
                    "mainSteps:\n" +
                    "- action: aws:runShellScript\n" +
                    "  name: ${this.documentName()}\n" +
                    "  inputs:\n" +
                    "    runCommand:\n"
            val chaos = "    - sudo yum -y install stress-ng\n" +
                    "    - stress-ng --vm $VM_WORKERS --vm-bytes ${configuration.otherParameters["virtualMemoryPercent"]}% -t ${Duration.parse(configuration
                    .duration).seconds}s\n"
            val scheduledChaosRollback = "    - echo \"sudo yum -y remove stress-ng\" | " +
                    "at now + ${Duration.parse(configuration.duration).toMinutes() + 1} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
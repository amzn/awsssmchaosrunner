// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

class DependencyPacketLossAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    private val log = KotlinLogging.logger { }
    override val documentContent: String
        get() {
            val documentHeader = "---\n" +
                    "schemaVersion: '2.2'\n" +
                    "description: Add Latency to Network Interface eth0 on EC2 instances\n" +
                    "mainSteps:\n" +
                    "- action: aws:runShellScript\n" +
                    "  name: ${this.documentName()}\n" +
                    "  inputs:\n" +
                    "    runCommand:\n"
            val chaos = "    - \"sudo tc qdisc add dev eth0 root handle 1: prio priomap 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2\"\n" +
                    "    - \"sudo tc qdisc add dev eth0 parent 1:1 handle 10: netem loss ${configuration.otherParameters["packetLossPercentage"]}%\"\n" +
                    "    - \"for k in \$(dig +short ${configuration.otherParameters["dependencyEndpoint"]});" +
                    " do echo \$k && sudo tc filter add dev eth0 protocol ip parent 1:0 prio 1 u32 match ip dst \$k/32 match ip dport " +
                    "${configuration.otherParameters["dependencyPort"]} 0xffff flowid 1:1; done\"\n" +
                    "    - \"sudo tc qdisc show\"\n"
            val scheduledChaosRollback = "    - \"echo \'sudo tc filter del dev eth0 prio 1 && " +
                    "sudo tc qdisc del dev eth0 parent 1:1 handle 10: && " +
                    "sudo tc qdisc del dev eth0 root handle 1: prio \' | " +
                    "at now + ${Duration.parse(configuration.duration).toMinutes()} minutes\"\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
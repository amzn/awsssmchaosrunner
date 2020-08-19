package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

private val log = KotlinLogging.logger { }

class NetworkInterfaceLatencyAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    override val documentContent: String
        // From https://github.com/adhorn/chaos-ssm-documents/blob/master/latency-stress.yml
        get() {

            val documentHeader = "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Add Latency to Network Interface eth0 on EC2 instances\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${this.documentName()}\n" +
                "  inputs:\n" +
                "    runCommand:\n"
            val chaos = "    - sudo tc qdisc add dev eth0 root netem delay " +
                "${getNetworkInterfaceLatency()} && tc qdisc show\n"
            val scheduledChaosRollback = "    - echo \"sudo tc qdisc del dev eth0 root netem delay " +
                "${getNetworkInterfaceLatency()} && tc qdisc show\" | " +
                "at now + ${Duration.parse(configuration.duration).toMinutes()} minutes\n"
            val documentContent = "$documentHeader$scheduledChaosRollback$chaos"
            log.info("Chaos Document Content:\n$documentContent")

            return documentContent
        }
}
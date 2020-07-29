package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import java.time.Duration

/**
 * This abstract class implements SSMAttack and provides common functionality to attacks
 * against AWS services. Importantly, this class defines the serviceCidrRangeQuery
 * which parses the service's CIDR range from https://ip-ranges.amazonaws.com/ip-ranges.json.
 * All AWS Service attacks require the following CLI tools to be installed on the EC2 host under attack:
 * <ul>
 *     <li>curl</li>
 *     <li>jq</li>
 *     <li>at</li>
 *     <li>tc</li>
 *     <li>echo</li>
 * </ul>
 * <b>It's important to note that jq is not installed by default on AL1 and AL2 hosts. You can install it with
 * sudo yum install jq.</b>
 */
abstract class AbstractAWSServiceAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    abstract val chaosContent: String

    override val documentContent: String
        get() {
            val rollbackTime = " | at now + ${Duration.parse(configuration.duration).toMinutes()} minutes\""
            val scheduledChaosRollback1 = "    - \"echo \'sudo tc filter del dev eth0 prio 1\'$rollbackTime"
            val scheduledChaosRollback2 = "    - \"echo \'sudo tc qdisc del dev eth0 parent 1:1 handle 10:\'$rollbackTime"
            val scheduledChaosRollback3 = "    - \"echo \'sudo tc qdisc del dev eth0 root handle 1: prio\'$rollbackTime"
            val scheduledChaosRollback = "$scheduledChaosRollback1\n$scheduledChaosRollback2\n$scheduledChaosRollback3\n"
            return documentHeader + scheduledChaosRollback + chaosContent
        }

    val serviceCidrRangeQuery: String
        get() {
            return "curl -s https://ip-ranges.amazonaws.com/ip-ranges.json | " +
                "jq -r '.prefixes[] | " +
                "select(.region==\\\"${configuration.otherParameters["region"]}\\\") | " +
                "select(.service==\\\"${configuration.otherParameters["service"]}\\\") | " +
                ".ip_prefix'"
        }

    private val documentHeader: String
        get() {
            return "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Attack an AWS service\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${documentName()}\n" +
                "  inputs:\n" +
                "    runCommand:\n"
        }
}

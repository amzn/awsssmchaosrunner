// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import software.amazon.awsssmchaosrunner.attacks.AbstractDependencyAttack

/**
 * This class extends AbstractDependencyPacketLossAttack and used for
 * network packet loss attack on multiple dependencies in one go.
 * It uses multiple ip addresses provided in command line argument.
 */
class MultiIPAddressPacketLossAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : AbstractDependencyAttack(ssm, configuration) {

    override val chaosContent: String
        get() {
            return "    - \"sudo tc qdisc add dev eth0 root handle 1: prio priomap 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2\"\n" +
                    "    - \"sudo tc qdisc add dev eth0 parent 1:1 handle 10: netem loss ${configuration.otherParameters["packetLossPercentage"]}%\"\n" +
                    "    - \"for k in ${configuration.otherParameters["dependencyIpAddresses"]};" +
                    " do echo \$k && sudo tc filter add dev eth0 protocol ip parent 1:0 prio 1 u32 match ip dst \$k/32 match ip dport " +
                    "${configuration.otherParameters["dependencyPort"]} 0xffff flowid 1:1; done\"\n" +
                    "    - \"sudo tc qdisc show\"\n"
        }

    override val documentDescription: String
        get() {
            return "description: Add packet loss to a dependency IP addresses through Network interface eth0.\n"
        }
}
package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement

/**
 * This attack drops packets to an AWS service using the CIDR ranges returned from
 * https://ip-ranges.amazonaws.com/ip-ranges.json. This is useful for services like S3
 * and DynamoDB which have a wide range of IP addresses. Otherwise it behaves similar
 * to the DependencyPacketLossAttack.
 */
class AWSServicePacketLossAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : AbstractAWSServiceAttack(ssm, configuration) {
    override val chaosContent: String
        get() {
            return "    - \"sudo tc qdisc add dev eth0 root handle 1: prio priomap 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2\"\n" +
                "    - \"sudo tc qdisc add dev eth0 parent 1:1 handle 10: netem loss ${configuration.otherParameters["packetLossPercentage"]}%\"\n" +
                "    - \"for k in $($serviceCidrRangeQuery);" +
                " do echo \$k && sudo tc filter add dev eth0 protocol ip parent 1:0 prio 1 u32 match ip dst \$k match ip dport " +
                "${configuration.otherParameters["dependencyPort"]} 0xffff flowid 1:1; done\"\n" +
                "    - \"sudo tc qdisc show\"\n"
        }
}

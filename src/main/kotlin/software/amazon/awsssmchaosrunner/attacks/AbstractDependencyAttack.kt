package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import mu.KotlinLogging
import java.time.Duration

/**
 * This abstract class implements SSMAttack and provides common functionality for
 * SSM attack on dependency.
 */
abstract class AbstractDependencyAttack constructor(
    override val ssm: AWSSimpleSystemsManagement,
    override val configuration: SSMAttack.Companion.AttackConfiguration
) : SSMAttack {
    private val log = KotlinLogging.logger { }

    abstract val chaosContent: String
    abstract val documentDescription: String

    override val documentContent: String
        get() {
            val documentContent = "$documentHeader$scheduledChaosRollback$chaosContent"
            log.info("Chaos Document Content:\n$documentContent")
            return documentContent
        }

    private val documentHeader: String
        get() {
            return "---\n" +
                    "schemaVersion: '2.2'\n" +
                    documentDescription +
                    "mainSteps:\n" +
                    "- action: aws:runShellScript\n" +
                    "  name: ${this.documentName()}\n" +
                    "  inputs:\n" +
                    "    runCommand:\n" +
                    "    - sudo yum -y install tc at || true\n" +
                    "    - sudo systemctl start atd\n"
        }

    private val scheduledChaosRollback: String
        get() {
            return "    - \"echo \'sudo tc filter del dev eth0 prio 1 && " +
                "sudo tc qdisc del dev eth0 parent 1:1 handle 10: && " +
                "sudo tc qdisc del dev eth0 root handle 1: prio \' | " +
                "at now + ${Duration.parse(configuration.duration).toMinutes()} minutes\"\n"
        }
}
package com.amazon.awsssmchaosrunner.attacks

import com.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections

class SSMAttackTest {
    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when getAttack called with NetworkInterfaceLatencyAttack return NetworkInterfaceLatencyAttack`() {
        val attack = getAttack(
            ssm,
            SSMAttack.Companion.AttackConfiguration(
                name = "NetworkInterfaceLatencyAttack",
                duration = "",
                timeoutSeconds = 0,
                cloudWatchLogGroupName = "",
                targets = Collections.emptyList(),
                concurrencyPercentage = 100,
                otherParameters = Collections.emptyMap()
            )
        )
        assertThat(attack).isInstanceOf(NetworkInterfaceLatencyAttack::class.java)
    }

    @Test
    fun `when getAttack called with test throws`() {
        assertThrows(NotImplementedError::class.java) {
            getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                    name = "test",
                    duration = "",
                    timeoutSeconds = 0,
                    cloudWatchLogGroupName = "",
                    targets = Collections.emptyList(),
                    concurrencyPercentage = 100,
                    otherParameters = Collections.emptyMap()
                )
            )
        }
    }

    @Test
    fun `test when documentName() is called the correct name is returned`() {
        val attack = getAttack(
            ssm,
            SSMAttack.Companion.AttackConfiguration(
                name = "NetworkInterfaceLatencyAttack",
                duration = "",
                timeoutSeconds = 0,
                cloudWatchLogGroupName = "",
                targets = Collections.emptyList(),
                concurrencyPercentage = 100,
                otherParameters = mutableMapOf("documentNameSuffix" to "NetwotkInterfaceLatencyAttack-1")
            )
        )
        assertThat(attack.documentName() == "NetwotkInterfaceLatencyAttack-1")
    }

    @Test
    fun `test when additional document name param does not exist`() {
        val attack = getAttack(
            ssm,
            SSMAttack.Companion.AttackConfiguration(
                name = "NetworkInterfaceLatencyAttack",
                duration = "",
                timeoutSeconds = 0,
                cloudWatchLogGroupName = "",
                targets = Collections.emptyList(),
                concurrencyPercentage = 100,
                otherParameters = emptyMap<String, String>()
            )
        )
        assertThat(attack.documentName() == "NetworkInterfaceLatencyAttack")
    }

    @Test
    fun `test when additional document name param is empty`() {
        val attack = getAttack(
            ssm,
            SSMAttack.Companion.AttackConfiguration(
                name = "NetworkInterfaceLatencyAttack",
                duration = "",
                timeoutSeconds = 0,
                cloudWatchLogGroupName = "",
                targets = Collections.emptyList(),
                concurrencyPercentage = 100,
                otherParameters = mutableMapOf("documentNameSuffix" to "")
            )
        )
        assertThat(attack.documentName() == "NetworkInterfaceLatencyAttack")
    }

    @Test
    fun `when getAttack called with networkInterfaceLatency in milliseconds with jitter`() {
        val attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "DependencyLatencyAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = mutableMapOf("dependencyEndpoint" to "test-endpoint",
                                "networkInterfaceLatencyMs" to "5", "networkInterfaceLatencyJitter" to "1ms",
                                "dependencyPort" to "1234")
                )
        )
        Assertions.assertTrue(attack.documentContent.contains("test-endpoint"))
        Assertions.assertTrue(attack.documentContent.contains("5ms"))
        Assertions.assertTrue(attack.documentContent.contains("1ms"))
        Assertions.assertTrue(attack.documentContent.contains("1234"))
    }

    @Test
    fun `when getAttack called with networkInterfaceLatency in milliseconds without jitter`() {
        val attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "DependencyLatencyAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = mutableMapOf("dependencyEndpoint" to "test-endpoint",
                                "networkInterfaceLatencyMs" to "5", "dependencyPort" to "1234")
                )
        )
        Assertions.assertTrue(attack.documentContent.contains("test-endpoint"))
        Assertions.assertTrue(attack.documentContent.contains("5ms"))
        Assertions.assertTrue(attack.documentContent.contains("10ms"))
        Assertions.assertTrue(attack.documentContent.contains("1234"))
    }

    @Test
    fun `when getAttack called with networkInterfaceLatency in microseconds with jitter`() {
        val attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "DependencyLatencyAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = mutableMapOf("dependencyEndpoint" to "test-endpoint",
                                "networkInterfaceLatencyUs" to "5", "networkInterfaceLatencyJitter" to "1us",
                                "dependencyPort" to "1234")
                )
        )
        Assertions.assertTrue(attack.documentContent.contains("test-endpoint"))
        Assertions.assertTrue(attack.documentContent.contains("5us"))
        Assertions.assertTrue(attack.documentContent.contains("1us"))
        Assertions.assertTrue(attack.documentContent.contains("1234"))
    }

    @Test
    fun `when getAttack called with networkInterfaceLatency in microseconds without jitter`() {
        val attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "DependencyLatencyAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = mutableMapOf("dependencyEndpoint" to "test-endpoint",
                                "networkInterfaceLatencyUs" to "5", "dependencyPort" to "1234")
                )
        )
        Assertions.assertTrue(attack.documentContent.contains("test-endpoint"))
        Assertions.assertTrue(attack.documentContent.contains("5us"))
        Assertions.assertTrue(attack.documentContent.contains("10us"))
        Assertions.assertTrue(attack.documentContent.contains("1234"))
    }
}
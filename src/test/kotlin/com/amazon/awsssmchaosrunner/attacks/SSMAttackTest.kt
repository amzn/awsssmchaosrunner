package com.amazon.awsssmchaosrunner.attacks

import com.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.assertj.core.api.Assertions.assertThat
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
}
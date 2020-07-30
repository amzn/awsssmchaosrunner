package com.amazon.awsssmchaosrunner.attacks

import com.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.Command
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.util.Collections

class AbstractDependencyAttackTest {
    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    lateinit var attack: SSMAttack

    @BeforeEach
    fun prepTest() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "MultiIPAddressLatencyAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = Collections.emptyMap()
                )
        )
    }

    @Test
    fun `when start() then ssm createDocument and sendCommand is called`() {
        attack.start()
        verify(exactly = 1) { ssm.createDocument(any()) }
        verify(exactly = 1) { ssm.sendCommand(any()) }
    }

    @Test
    fun `when stop() then ssm cancelCommand and deleteDocument is called`() {
        attack.stop(Command())
        verify(exactly = 1) { ssm.cancelCommand(any()) }
        verify(exactly = 1) { ssm.deleteDocument(any()) }
    }

    @Test
    fun `when getAttack() called return MultiIPAddressLatencyAttack`() {
        assertThat(attack).isInstanceOf(MultiIPAddressLatencyAttack::class.java)
    }
}
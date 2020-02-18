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
    fun `when getAttack called with BlackholeDNSAttackTest return BlackholeDNSAttackTest`() {
        val attack = getAttack(ssm,
                SSMAttack.Companion.AttackConfiguration("NetworkInterfaceLatencyAttack",
                        "",
                        0,
                        "",
                        Collections.emptyList(),
                        100,
                        Collections.emptyMap()))
        assertThat(attack).isInstanceOf(NetworkInterfaceLatencyAttack::class.java)
    }

    @Test
    fun `when getAttack called with test throws`() {
        assertThrows(NotImplementedError::class.java) {
            getAttack(ssm, SSMAttack.Companion.AttackConfiguration("test",
                    "",
                    0,
                    "",
                    Collections.emptyList(),
                    100,
                    Collections.emptyMap()))
        }
    }
}
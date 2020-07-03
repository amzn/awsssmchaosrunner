// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks

import software.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
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

    @Test
    fun `test when documentName() is called the correct name is returned`() {
        val attack = getAttack(ssm,
                SSMAttack.Companion.AttackConfiguration("NetworkInterfaceLatencyAttack",
                        "",
                        0,
                        "",
                        Collections.emptyList(),
                        100,
                        mutableMapOf("documentNameSuffix" to "NetwotkInterfaceLatencyAttack-1")))
        assertThat(attack.documentName() == "NetwotkInterfaceLatencyAttack-1")
    }
    @Test
    fun `test when additional document name param does not exist`() {
        val attack = getAttack(ssm,
                SSMAttack.Companion.AttackConfiguration("NetworkInterfaceLatencyAttack",
                        "",
                        0,
                        "",
                        Collections.emptyList(),
                        100,
                        emptyMap<String, String>()))
        assertThat(attack.documentName() == "NetworkInterfaceLatencyAttack")
    }

    @Test
    fun `test when additional document name param is empty`() {
        val attack = getAttack(ssm,
                SSMAttack.Companion.AttackConfiguration("NetworkInterfaceLatencyAttack",
                        "",
                        0,
                        "",
                        Collections.emptyList(),
                        100,
                        mutableMapOf("documentNameSuffix" to "")))
        assertThat(attack.documentName() == "NetworkInterfaceLatencyAttack")
    }
}
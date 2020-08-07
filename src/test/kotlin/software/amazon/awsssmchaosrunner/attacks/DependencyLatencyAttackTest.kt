// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections

class DependencyLatencyAttackTest {

    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    lateinit var attack: SSMAttack

    @BeforeEach
    fun prepTest() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        attack = SSMAttack.getAttack(
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
    }

    @Test
    fun `when getAttack called documentContent contains required parameters`() {
        assertTrue(attack.documentContent.contains("test-endpoint"))
        assertTrue(attack.documentContent.contains("5"))
        assertTrue(attack.documentContent.contains("1234"))
    }
}
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import java.util.Collections

class MultiIPAddressPacketLossAttackTest {

    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    lateinit var attack: SSMAttack

    @BeforeEach
    fun prepTest() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        attack = getAttack(
                ssm,
                SSMAttack.Companion.AttackConfiguration(
                        name = "MultiIPAddressPacketLossAttack",
                        duration = "PT10M",
                        timeoutSeconds = 120,
                        cloudWatchLogGroupName = "",
                        targets = Collections.emptyList(),
                        concurrencyPercentage = 100,
                        otherParameters = mutableMapOf("dependencyIpAddresses" to "1.2.3.4 4.5.6.7",
                                "packetLossPercentage" to "100", "dependencyPort" to "1234")
                )
        )
    }

    @Test
    fun `when getAttack called documentContent contains required parameters`() {
        assertTrue(attack.documentContent.contains("1.2.3.4 4.5.6.7"))
        assertTrue(attack.documentContent.contains("100"))
        assertTrue(attack.documentContent.contains("1234"))
    }
}
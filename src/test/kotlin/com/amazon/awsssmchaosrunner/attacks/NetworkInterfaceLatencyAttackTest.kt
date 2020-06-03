// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.awsssmchaosrunner.attacks

import com.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.Command
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections

class NetworkInterfaceLatencyAttackTest {
    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    lateinit var attack: SSMAttack

    @BeforeEach
    fun prepTest() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        attack = getAttack(ssm,
                SSMAttack.Companion.AttackConfiguration("NetworkInterfaceLatencyAttack",
                        "PT1M",
                        0,
                        "",
                        Collections.emptyList(),
                        100,
                        Collections.emptyMap()))
    }

    @Test
    fun `when start() then ssm createDocument and sendCommand is called`() {
        attack.start()
        verify(exactly = 1) { ssm.createDocument(any()) }
        verify(exactly = 1) { ssm.sendCommand(any()) }
    }

    @Test
    fun `when stop() then ssm deleteDocument is called`() {
        attack.stop(Command())
        verify(exactly = 1) { ssm.deleteDocument(any()) }
    }
}
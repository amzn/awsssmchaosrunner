// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.amazon.awsssmchaosrunner.attacks;

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementAsyncClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.Target
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import software.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import java.time.Duration

@Disabled("Only for demonstration")
class Demo {
    private val awsProfile = "reinventDemo"

    private fun getSSMClient(): AWSSimpleSystemsManagement {
        return AWSSimpleSystemsManagementAsyncClientBuilder.standard()
                .withCredentials(ProfileCredentialsProvider(awsProfile))
                .withRegion(Regions.US_EAST_1)
                .build()
    }

    @Test
    fun `Hog CPU on a host`() {
        val ssmClient = getSSMClient()

        val attack = getAttack(ssmClient,
                SSMAttack.Companion.AttackConfiguration(
                        name = "CPUHogAttack",
                        targets = listOf(Target().withKey("tag:Name").withValues("ChaosRunnerDemo")),
                        otherParameters = mutableMapOf("cpuLoadPercent" to "80"),
                        duration = "PT2M",
                        cloudWatchLogGroupName = "test",

                        //The maximum number of instances that are allowed to run the command at the same time.
                        concurrencyPercentage = 100,

                        //If this time is reached and the command has not already started running, it will not run.
                        timeoutSeconds = 30
                ))

        val command = attack.start()

        testSomething()

        attack.stop(command)
    }

    private fun testSomething() {
        // Generate load for a duration
        Thread.sleep(Duration.parse("PT3M").toMillis())
    }
}

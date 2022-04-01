package software.amazon.awsssmchaosrunner.attacks.fis

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.fis.FisClient
import software.amazon.awssdk.services.fis.model.StartExperimentResponse
import software.amazon.awssdk.services.fis.model.StopExperimentRequest
import software.amazon.awssdk.services.fis.model.StopExperimentResponse
import software.amazon.awsssmchaosrunner.attacks.fis.FISSendCommandAttack.Companion.getAttack

class FISAttackTest {
    @RelaxedMockK
    lateinit var fis: FisClient

    private val documentArns = mapOf(
            "CPUStress" to "AWSFIS-Run-CPU-Stress",
            "IOStress" to "AWSFIS-Run-IO-Stress",
            "KillProcess" to "AWSFIS-Run-Kill-Process",
            "MemoryStress" to "AWSFIS-Run-Memory-Stress",
            "NetworkBlackholePort" to "AWSFIS-Run-Network-Blackhole-Port",
            "NetworkLatency" to "AWSFIS-Run-Network-Latency",
            "NetworkLatencySources" to "AWSFIS-Run-Network-Latency-Sources",
            "NetworkPacketLoss" to "AWSFIS-Run-Network-Packet-Loss",
            "NetworkPacketLossSources" to "AWSFIS-Run-Network-Packet-Loss-Sources"
    )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `when getAttack is called an FisAttack is returned`() {
        val attack = getAttack(fis,
                FISAttack.Companion.AttackConfiguration(
                        targets = emptyMap(),
                        targetsSelectionMode = "ALL",
                        cloudWatchLogGroupArn = "",
                        stopConditionCloudWatchAlarmArn = "",
                        roleArn = ""),
                FISSendCommandAttack.Companion.ActionConfiguration(
                        name = "IOStress",
                        duration = "PT2M",
                        awsRegion = Region.US_WEST_2.toString(),
                        otherParameters = emptyMap())
        )
        assertThat(attack).isInstanceOf(FISAttack::class.java)
    }

    @Test
    fun `when getAttack is called with a unknown name it throws`() {
        assertThrows(NotImplementedError::class.java) {
            getAttack(fis,
                    FISAttack.Companion.AttackConfiguration(
                            targets = emptyMap(),
                            targetsSelectionMode = "ALL",
                            cloudWatchLogGroupArn = "",
                            stopConditionCloudWatchAlarmArn = "",
                            roleArn = ""),
                    FISSendCommandAttack.Companion.ActionConfiguration(
                            name = "test",
                            duration = "PT2M",
                            awsRegion = Region.US_WEST_2.toString(),
                            otherParameters = emptyMap())
            )
        }
    }

    @Test
    fun `when getAttack is called the correct FisAttack is returned`() {
        for (attackName in documentArns.keys) {
            val attack = getAttack(fis,
                    FISAttack.Companion.AttackConfiguration(
                            targets = emptyMap(),
                            targetsSelectionMode = "ALL",
                            cloudWatchLogGroupArn = "",
                            stopConditionCloudWatchAlarmArn = "",
                            roleArn = ""),
                    FISSendCommandAttack.Companion.ActionConfiguration(
                            name = attackName,
                            duration = "PT2M",
                            awsRegion = Region.US_WEST_2.toString(),
                            otherParameters = emptyMap())
            )
            assertThat(attack.actionInputList.containsKey(documentArns[attackName])).isEqualTo(true)
        }
    }

    @Test
    fun `when getAttack is called for KillProcess the correct FisAttack params are set`() {
        val attack = getAttack(fis,
                FISAttack.Companion.AttackConfiguration(
                        targets = emptyMap(),
                        targetsSelectionMode = "ALL",
                        cloudWatchLogGroupArn = "",
                        stopConditionCloudWatchAlarmArn = "",
                        roleArn = ""),
                FISSendCommandAttack.Companion.ActionConfiguration(
                        name = "KillProcess",
                        duration = "PT2M",
                        awsRegion = Region.US_WEST_2.toString(),
                        otherParameters = mapOf("ProcessName" to "MyProcess"))
        )
        assertThat(attack.params.get("ProcessName")).isEqualTo("MyProcess")
        assertThat("Duration" !in attack.params.keySet())
    }

    @Test
    fun `when getAttack is called for CPUStress the correct FisAttack params are set`() {
        val attack = getATestAttack(fis)
        assertThat(attack.params.get("CPU")).isEqualTo("0")
        assertThat(attack.params.get("DurationSeconds")).isEqualTo("120")
    }

    @Test
    fun `when stopAttack called it returns null from non-stoppable states`() {
        val attack = getATestAttack(fis)

        val experimentResponse = mockk<StartExperimentResponse>()
        for (state in listOf("stopped", "failed", "completed", "stopping"))
        {
            every { experimentResponse.experiment().state().status().name } returns state
            assertThat(attack.stop(experimentResponse, false)).isEqualTo(null)
        }
    }

    @Test
    fun `when stopAttack called it returns response from stoppable states`() {
        val experimentResponse = mockk<StartExperimentResponse>()
        val stopExperimentResponse = mockk<StopExperimentResponse>()
        val fis = mockk<FisClient>()
        val attack = getATestAttack(fis)

        every { experimentResponse.experiment().state().status().name } returns "running"
        every { experimentResponse.experiment().id() } returns "test"
        every { fis.stopExperiment(StopExperimentRequest.builder().id("test").build()) } returns stopExperimentResponse
        assertThat(attack.stop(experimentResponse, false)).isEqualTo(stopExperimentResponse)
    }

    private fun getATestAttack(fis: FisClient) = getAttack(fis,
            FISAttack.Companion.AttackConfiguration(
                    targets = emptyMap(),
                    targetsSelectionMode = "ALL",
                    cloudWatchLogGroupArn = "",
                    stopConditionCloudWatchAlarmArn = "",
                    roleArn = ""),
            FISSendCommandAttack.Companion.ActionConfiguration(
                    name = "CPUStress",
                    duration = "PT2M",
                    awsRegion = Region.US_WEST_2.toString(),
                    otherParameters = mapOf("CPU" to "0"))
    )
}
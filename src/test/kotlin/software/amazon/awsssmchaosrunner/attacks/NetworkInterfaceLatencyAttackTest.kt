package software.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.Command
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
import java.util.Collections

class NetworkInterfaceLatencyAttackTest {
    @RelaxedMockK
    lateinit var ssm: AWSSimpleSystemsManagement

    lateinit var attack: SSMAttack

    @BeforeEach
    fun prepTest() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        attack = getAttack(
            ssm,
            SSMAttack.Companion.AttackConfiguration(
                name = "NetworkInterfaceLatencyAttack",
                duration = "PT1M",
                timeoutSeconds = 0,
                cloudWatchLogGroupName = "",
                targets = Collections.emptyList(),
                concurrencyPercentage = 100,
                otherParameters = mutableMapOf("networkInterfaceLatencyMs" to "0")
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
    fun `when stop() then ssm deleteDocument is called`() {
        attack.stop(Command())
        verify(exactly = 1) { ssm.deleteDocument(any()) }
    }
}
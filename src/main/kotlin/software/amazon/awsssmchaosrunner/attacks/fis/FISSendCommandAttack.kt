package software.amazon.awsssmchaosrunner.attacks.fis

import org.json.JSONObject
import software.amazon.awssdk.services.fis.FisClient
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateActionInput
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateTargetInput
import java.time.Duration

private const val ACTION_ID = "aws:ssm:send-command"
private const val EC2_RESOURCE_TYPE = "aws:ec2:instance"

class FISSendCommandAttack(
    override val fis: FisClient,
    override val attackConfiguration: FISAttack.Companion.AttackConfiguration,
    private val actionConfiguration: ActionConfiguration,
    private val documentArn: String,
    public val params: JSONObject
) : FISAttack {
    override val actionInputList: Map<String, CreateExperimentTemplateActionInput>
        get() {
            return mutableMapOf(this.documentArn.split("/")[1] to
                    getActionInput(actionConfiguration,
                            this.documentArn,
                            this.params))
        }

    // From https://docs.aws.amazon.com/fis/latest/APIReference/API_CreateExperimentTemplateTargetInput.html
    override val targetInput: CreateExperimentTemplateTargetInput = createTargetInput(attackConfiguration)

    companion object {
        private fun getActionInput(
            configuration: ActionConfiguration,
            documentArn: String,
            documentParams: JSONObject
        ): CreateExperimentTemplateActionInput {
            return CreateExperimentTemplateActionInput.builder()
                    .actionId(ACTION_ID)
                    .parameters(
                            mutableMapOf("duration" to configuration.duration,
                                    "documentArn" to documentArn,
                                    "documentParameters" to documentParams.toString()))
                    .targets(mutableMapOf("Instances" to TARGET_INSTANCE_KEY)).build()
        }

        private fun createTargetInput(configuration: FISAttack.Companion.AttackConfiguration):
                CreateExperimentTemplateTargetInput {
            return CreateExperimentTemplateTargetInput.builder()
                    .resourceType(EC2_RESOURCE_TYPE)
                    .resourceTags(configuration.targets)
                    .selectionMode(configuration.targetsSelectionMode)
                    .build()
        }

        private fun documentParams(configuration: ActionConfiguration): JSONObject {
            val params = mutableMapOf<String, String>()
            params.putAll(configuration.otherParameters)
            if (configuration.name == "KillProcess") {
                return JSONObject(params)
            }
            params["DurationSeconds"] = Duration.parse(configuration.duration).seconds.toString()
            return JSONObject(params)
        }

        // From https://docs.aws.amazon.com/fis/latest/userguide/actions-ssm-agent.html
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

        private fun getAttackDocumentArn(attackName: String, region: String): String {
            return "arn:aws:ssm:$region::document/${documentArns[attackName]}"
        }

        data class ActionConfiguration(
            val name: String,
            val duration: String,
            val awsRegion: String,
            val otherParameters: Map<String, String>
        )

        fun getAttack(
            fis: FisClient,
            attackConfiguration: FISAttack.Companion.AttackConfiguration,
            actionConfiguration: ActionConfiguration
        ): FISSendCommandAttack {
            if (actionConfiguration.name !in documentArns.keys) {
                throw NotImplementedError("${actionConfiguration.name} is not a valid FISAttack")
            }
            return FISSendCommandAttack(fis,
                    attackConfiguration,
                    actionConfiguration,
                    getAttackDocumentArn(actionConfiguration.name, actionConfiguration.awsRegion),
                    documentParams(actionConfiguration))
        }
    }
}
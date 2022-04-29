package software.amazon.awsssmchaosrunner.attacks.fis

import software.amazon.awssdk.services.fis.FisClient
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateActionInput
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateLogConfigurationInput
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateRequest
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateStopConditionInput
import software.amazon.awssdk.services.fis.model.CreateExperimentTemplateTargetInput
import software.amazon.awssdk.services.fis.model.DeleteExperimentTemplateRequest
import software.amazon.awssdk.services.fis.model.ExperimentTemplate
import software.amazon.awssdk.services.fis.model.ExperimentTemplateCloudWatchLogsLogConfigurationInput
import software.amazon.awssdk.services.fis.model.StartExperimentRequest
import software.amazon.awssdk.services.fis.model.StartExperimentResponse
import software.amazon.awssdk.services.fis.model.StopExperimentRequest
import software.amazon.awssdk.services.fis.model.StopExperimentResponse

const val TARGET_INSTANCE_KEY = "testInstances"
const val EXPERIMENT_DESCRIPTION = "FIS Experiment Template"

interface FISAttack {
    val fis: FisClient
    val attackConfiguration: AttackConfiguration
    val actionInputList: Map<String, CreateExperimentTemplateActionInput>
    val targetInput: CreateExperimentTemplateTargetInput

    fun start(): StartExperimentResponse {
        createExperimentTemplate()
        val startExperimentRequest = StartExperimentRequest.builder()
                .experimentTemplateId(createExperimentTemplate().id())
                .build()
        return fis.startExperiment(startExperimentRequest)
    }

    private fun createExperimentTemplate(): ExperimentTemplate {
        var experimentTemplateRequest = CreateExperimentTemplateRequest.builder()
                .actions(this.actionInputList)
                .targets(mutableMapOf(TARGET_INSTANCE_KEY to this.targetInput))
                .description(EXPERIMENT_DESCRIPTION)
                .stopConditions(createStopCondition())
                .roleArn(attackConfiguration.roleArn)

        if (attackConfiguration.cloudWatchLogGroupArn.isNotEmpty()) {
            experimentTemplateRequest = experimentTemplateRequest.logConfiguration(createLogConfiguration())
        }

        return fis.createExperimentTemplate(experimentTemplateRequest.build()).experimentTemplate()
    }

    private fun createLogConfiguration(): CreateExperimentTemplateLogConfigurationInput {
        return CreateExperimentTemplateLogConfigurationInput.builder()
                .cloudWatchLogsConfiguration(ExperimentTemplateCloudWatchLogsLogConfigurationInput.builder()
                        .logGroupArn(attackConfiguration.cloudWatchLogGroupArn)
                        .build())
                .build()
    }

    private fun createStopCondition(): CreateExperimentTemplateStopConditionInput {
        return CreateExperimentTemplateStopConditionInput.builder()
                .source(getStopConditionCloudWatchAlarmArn(attackConfiguration)).build()
    }

    fun stop(experiment: StartExperimentResponse, deleteExperimentTemplate: Boolean): StopExperimentResponse? {
        val stopExperimentResponse: StopExperimentResponse?
        if (experiment.experiment().state().status().name in listOf("stopped", "failed", "completed", "stopping")) {
            stopExperimentResponse = null
        } else {
            stopExperimentResponse = fis.stopExperiment(StopExperimentRequest.builder()
                    .id(experiment.experiment().id()).build())
        }
        if (deleteExperimentTemplate) {
            fis.deleteExperimentTemplate(DeleteExperimentTemplateRequest
                    .builder().id(experiment.experiment().experimentTemplateId())
                    .build())
        }
        return stopExperimentResponse
    }

    companion object {
        data class AttackConfiguration(
            val targets: Map<String, String>,
            val targetsSelectionMode: String,
            val cloudWatchLogGroupArn: String,
            val stopConditionCloudWatchAlarmArn: String,
            val roleArn: String
        )

        private fun getStopConditionCloudWatchAlarmArn(configuration: AttackConfiguration): String {
            if (configuration.stopConditionCloudWatchAlarmArn.isEmpty()) return "none"
            return configuration.stopConditionCloudWatchAlarmArn
        }
    }
}
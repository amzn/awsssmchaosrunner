[![Build Status](https://travis-ci.org/amzn/awsssmchaosrunner.svg)](https://travis-ci.org/amzn/awsssmchaosrunner)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.amazon.awsssmchaosrunner/awsssmchaosrunner/badge.svg)](https://maven-badges.herokuapp.com/maven-central/software.amazon.awsssmchaosrunner/awsssmchaosrunner)
[![Javadoc](https://javadoc-badge.appspot.com/software.amazon.awsssmchaosrunner/awsssmchaosrunner.svg?label=javadoc)](http://www.javadoc.io/doc/software.amazon.awsssmchaosrunner/awsssmchaosrunner)

## AWSSSMChaosRunner
AWSSSMChaosRunner is a library which simplifies failure injection testing and chaos engineering for [EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/concepts.html) and [ECS (with EC2 launch type)](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/getting-started-ecs-ec2.html). 
It offers the following options for failure injection - 
  * [AWS Systems Manager SendCommand](https://docs.aws.amazon.com/systems-manager/latest/APIReference/API_SendCommand.html)
  * [AWS Fault Injection Simulator](https://aws.amazon.com/fis/)

![](./AWSSSMChaosRunner.png)

**An in-depth introduction to this library and how Prime Video uses it can be found here** - https://aws.amazon.com/blogs/opensource/building-resilient-services-at-prime-video-with-chaos-engineering/   

### Usage with AWS Systems Manager SendCommand
1. **Setup permissions for calling SSM from tests package**

    This can be done in many different ways. The approach described here generates temporary credentials for AWS SSM on each run of the tests. To enable this the following are needed
    
    * An IAM role with the following permissions.
        (JSON snippet)  
        ```json
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": [
                        "sts:AssumeRole",
                        "ssm:CancelCommand",
                        "ssm:CreateDocument",
                        "ssm:DeleteDocument",
                        "ssm:DescribeDocument",
                        "ssm:DescribeInstanceInformation",
                        "ssm:DescribeDocumentParameters",
                        "ssm:DescribeInstanceProperties",
                        "ssm:GetDocument",
                        "ssm:ListTagsForResource",
                        "ssm:ListDocuments",
                        "ssm:ListDocumentVersions",
                        "ssm:SendCommand"
                    ],
                    "Resource": [
                        "*"
                    ],
                    "Effect": "Allow"
                },
                {
                    "Action": [
                        "ec2:DescribeInstances",
                        "iam:PassRole",
                        "iam:ListRoles"
                    ],
                    "Resource": [
                        "*"
                    ],
                    "Effect": "Allow"
                },
                {
                    "Action": [
                        "ssm:StopAutomationExecution",
                        "ssm:StartAutomationExecution",
                        "ssm:DescribeAutomationExecutions",
                        "ssm:GetAutomationExecution"
                    ],
                    "Resource": [
                        "*"
                    ],
                    "Effect": "Allow"
                }
            ]
        }
        ```
    * An IAM user which can assume the above role.

2. **Add `AWSSSMChaosRunner` maven dependency to your tests package** 
    ```
    <dependency>
      <groupId>software.amazon.awsssmchaosrunner</groupId>
      <artifactId>awsssmchaosrunner</artifactId>
      <version>1.3.0</version>
    </dependency> 
    ```

1. **Initialise the SSM Client**
    (Kotlin snippet)
    ```kt
    @Bean
    open fun awsSecurityTokenService(
       credentialsProvider: AWSCredentialsProvider, 
       awsRegion: String
       ): AWSSecurityTokenService {
        return AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsRegion)
            .build()
    }
    
    @Bean
    open fun awsSimpleSystemsManagement(
       securityTokenService: AWSSecurityTokenService,
       awsAccountId: String,
       chaosRunnerRoleName: String
       ): AWSSimpleSystemsManagement {
        val chaosRunnerRoleArn = "arn:aws:iam::$awsAccountId:role/$chaosRunnerRoleName"
        val credentialsProvider = STSAssumeRoleSessionCredentialsProvider
            .Builder(chaosRunnerRoleArn, "ChaosRunnerSession")
            .withStsClient(securityTokenService).build()
    
        return AWSSimpleSystemsManagementClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .build()
    }
    ```
 
1. **Start the fault injection attack before starting the test and stop it after the test**
    (Kotlin snippet)
    ```kt
    import software.amazon.awsssmchaosrunner.attacks.SSMAttack
    import software.amazon.awsssmchaosrunner.attacks.SSMAttack.Companion.getAttack
    ...

    @Before
    override fun initialise(args: Array<String>) {
        if (shouldExecuteChaosRunner()) {
            ssm = applicationContext.getBean(AWSSimpleSystemsManagement::class.java)
            ssmAttack = getAttack(ssm, attackConfiguration)
            command = ssmAttack.start()
        }
    }
    
    @After
    override fun destroy() {
        ssmAttack.stop(command)
    }
    ```
1. **Run the test**

### Usage with AWS FIS
1. **Setup permissions for calling SSM from tests package**

   This can be done in many different ways. The approach described here generates temporary credentials for AWS SSM on each run of the tests. To enable this the following are needed

    * An IAM role with the following permissions.
      (JSON snippet)
        ```json
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Action": [
                        "sts:AssumeRole",
                        "ec2:DescribeInstances",
                        "iam:ListRoles",
                        "ssm:ListCommands",
                        "ssm:SendCommand",
                        "ssm:CancelCommand",
                        "iam:PassRole",
                        "ec2:RebootInstances",
                        "ec2:StopInstances",
                        "ec2:StartInstances",
                        "ec2:TerminateInstances",
                        "fis:InjectApiInternalError",
                        "fis:InjectApiThrottleError",
                        "fis:InjectApiUnavailableError",
                        "fis:ListExperimentTemplates",
                        "fis:ListActions",
                        "fis:ListTargetResourceTypes",
                        "fis:ListExperiments",
                        "fis:GetTargetResourceType",
                        "fis:CreateExperimentTemplate",
                        "fis:DeleteExperimentTemplate",
                        "fis:StopExperiment",
                        "fis:StartExperiment"                    
                    ],
                    "Resource": [
                        "*"
                    ],
                    "Effect": "Allow"
                },
                {
                    "Action": [
                        "iam:CreateServiceLinkedRole"
                    ],
                    "Resource": [
                        "*"
                    ],
                    "Effect": "Allow",
                    "Conditions": {
                          "StringEquals": {
                               "iam:AWSServiceName": "fis.amazonaws.com"
                          }
                    }
                }
            ]
        }
        ```
    * An IAM user which can assume the above role.

2. **Add `AWSSSMChaosRunner` maven dependency to your tests package**
    ```
    <dependency>
      <groupId>software.amazon.awsssmchaosrunner</groupId>
      <artifactId>awsssmchaosrunner</artifactId>
      <version>1.3.0</version>
    </dependency> 
    ```

1. **Initialise the FIS Client**
   (Kotlin snippet)
    ```java
   //Java code snippet
    String executionRoleArn = getenv("EXECUTION_ROLE_ARN");
    Region awsRegion = Region.of(getenv("AWS_REGION"));

    StsClient stsClient = StsClient.builder().build();
    StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
           .refreshRequest(AssumeRoleRequest.builder()
                    .roleArn(executionRoleArn)
                    .roleSessionName("ChaosRunnerSession")
                    .build())
                    .stsClient(stsClient)
                    .build();
    FisClient fisClient = FisClient.builder().credentialsProvider(assumeRoleCredentialsProvider).region(awsRegion).build();
    ```

1. **Configure and execute the FIS failure injection**
    ```java
    String targetsSelectionMode = "ALL";
    String cloudWatchLogGroupArn = "";
    String stopConditionCloudWatchAlarmArn = "";
    String name = "IOStress"; // This failure injection consumes disk space
    String duration = "PT2M";
    Map<String, String> otherFailureInjectionParameters = Collections.emptyMap();
    
    FISAttack.Companion.AttackConfiguration attackConfiguration = new FISAttack.Companion.AttackConfiguration(targets,
            targetsSelectionMode,
            cloudWatchLogGroupArn,
            stopConditionCloudWatchAlarmArn,
            executionRoleArn);
    FISSendCommandAttack.Companion.ActionConfiguration actionConfiguration = new FISSendCommandAttack.Companion.ActionConfiguration(
            name,
            duration,
            awsRegion.toString(),
            otherFailureInjectionParameters
    );
    FISAttack fisAttack = FISSendCommandAttack.Companion.getAttack(fisClient, attackConfiguration, actionConfiguration);
    StartExperimentResponse experiment = fisAttack.start();
    ...
    ...
    boolean deleteExperimentTemplate = false;
    fisAttack.stop(experiment, deleteExperimentTemplate);
    ```

## FAQs

* **What about [Chaos-SSM-Documents](https://github.com/adhorn/chaos-ssm-documents) (github repo) ?**
    
    The idea for AWSSSMChaosRunner came from Chaos-SSM-Documents (and from [medium post](https://medium.com/@adhorn/injecting-chaos-to-amazon-ec2-using-amazon-system-manager-ca95ee7878f5)).

* **Why use AWS SSM ?**
    
    In most cases EC2 fleets are already using the SSM Agent for OS patching, this library leverages this existing agent and reduces
     setup work needed for fault injection.
     
* **What failure injections are available ?**
    
    * [NetworkInterfaceLatency](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/NetworkInterfaceLatencyAttack.kt) - Adds latency to all inbound/outbound calls to 
    a given network interface.
    * [DependencyLatency](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/DependencyLatencyAttack.kt) - Adds latency to inbound/outbound calls to a given 
    external dependency.
    * [MemoryHog](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/MemoryHogAttack.kt) - Hogs virtual memory on the fleet.
    * [CPUHog](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/CPUHogAttack.kt) - Hogs CPU on the fleet.
    * [DiskHog](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/DiskHogAttack.kt) - Hogs disk space on the fleet.
    * [DependencyPacketLossAttack](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/DependencyPacketLossAttack.kt) - Drops packets on inbound/outbound calls to a given external dependency.
    * [AWSServiceLatencyAttack](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/AWSServiceLatencyAttack)
         \- Adds latency to an AWS service using the CIDR ranges returned
         from https://ip-ranges.amazonaws.com/ip-ranges.json. This is
         necessary for services like S3 or DynamoDB where the resolved
         IPAddress can change during the chaos attack.
    * [AWSServicePacketLossAttack](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/AWSServicePacketLossAttack.kt) - Drops packets to an AWS service using the CIDR ranges returned from https://ip-ranges.amazonaws.com/ip-ranges.json. This is necessary for services like S3 or DynamoDB where the resolved IPAddress can change during the chaos attack.
    * [MultiIPAddressLatencyAttack](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/MultiIPAddressLatencyAttack.kt) - Adds latencies to calls to a list of dependencies specified by IPAddress. This could be useful for a router -> host kind of a setup.
    * [MultiIPAddressPacketLossAttack](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/MultiIPAddressPacketLossAttack.kt) - Drops packets from calls to a list of dependencies specified by IPAddress. This could be useful for a router -> host kind of a setup.
    * Fault Injection Simulator (FIS) attacks - All the FIS SSM agent attacks are supported. [Full list](./src/main/kotlin/software/amazon/awsssmchaosrunner/attacks/fis/FISSendCommandAttack.kt)
    
* **What about other failure injections ?**

    You're welcome to send pull requests for other failure injections.
    
* **How is the failure injection rolled back ? / What if AWS SSM fails to stop the failure injection ?**

    SSM is not actually used to stop/roll back the failure injection. The failure injection scripts first schedule the failure rollback 
    (with [at command](https://pubs.opengroup.org/onlinepubs/9699919799/utilities/at.html)) and then start the actual failure injection. This ensures that, barring special cases, the failure injection will be rolled back at a specified time in the future.

* **What languages does AWSSSMChaosRunner support ?**

    AWSSSMChaosRunner can be used as a dependency from Kotlin, Java or Scala.

* **Is there a complete working demo of using this library ?**

    A demonstration can be found in [Demo.kt](./src/test/kotlin/software/amazon/awsssmchaosrunner/attacks/Demo.kt).
    To run the demo:
    
    * Clone this project.
    
    * Build the gradle project successfully (via gradle CLI or IDE).
    
    * Modify the `awsProfile` value to the awsProfile name for your AWS account.
    
    * Comment `@Disabled()` annotation.
    
    * Run the gradle test target. 
    
* **Can AWSSSMChaosRunner be used for [Amazon Elastic Container Service (ECS)](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html) ?**

    Yes. The above EC2 usage steps should be followed after the **SSM agent setups** listed below.
    
    * ECS + EC2 launch type
       * SSM Agent setup 
         
         The SSM Agent is required for using SSM SendCommand API and thus, for using AWSSSMChaosRunner. The base EC2 images include the
         SSM Agent, but the base ECS images do not. It can be installed directly at the host level. This can be achieved with the following
          CloudFormation snippet (YAML):
           ```yml
           # Adapted from https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/quickref-ecs.html
             LaunchConfiguration0:
               Type: AWS::AutoScaling::LaunchConfiguration
               Metadata:
                 # This is processed by cfn-init in the Properties.UserData script below. It installs a
                 # service that monitors for changes in the Metadata just below, causing a configuration
                 # update.
                 #
                 # CloudFormation updates to the LaunchConfiguration's Properties won't take effect on
                 # existing instances. Consequently, any CloudFormation field that could change should go in
                 # the Metadata.
                 AWS::CloudFormation::Init:
                   config:
                     # https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html
                     packages:
                       rpm:
                         # The SSM (Systems Systems Manager) agent is necessary to use `aws ssm send-command`
                         # or 'Run Command' in the AWS-EC2 console. It's also required by InfoSec for our
                         # exception. The base EC2 images include it, but the base ECS images do not.
                         # https://docs.aws.amazon.com/systems-manager/latest/userguide/sysman-install-startup-linux.html
                         amazon-ssm-agent: !Sub https://s3.${AWS::Region}.amazonaws.com/amazon-ssm-${AWS::Region}/latest/linux_amd64/amazon-ssm-agent.rpm
           ```
       * Possible failure injections
           
           SSM SendCommand API will run the underlying failure injection commands directly on the EC2 host. This will affect all tasks
            running on these hosts. The EC2 + ECS host does not impose any additional restrictions regarding what resources can or can't
             be accessed. Thus, all AWSSSMChaosRunner attacks can be run on EC2 + ECS.
            
* **Can AWSSSMChaosRunner be used for [AWS Lambda](https://aws.amazon.com/lambda/) ?**
    
    No.
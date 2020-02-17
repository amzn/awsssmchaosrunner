package com.amazon.awsssmchaosrunner.attacks

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement

class BlackholeDNSAttack constructor(override val ssm: AWSSimpleSystemsManagement, private val duration: Int) : SSMAttack {
    override val documentContent: String
        // TODO store this in .yaml
        // From https://github.com/adhorn/chaos-ssm-documents/blob/master/blackhole-dns-stress.yml
        get() = "---\n" +
                "schemaVersion: '2.2'\n" +
                "description: Blackhole DNS on EC2 instances\n" +
                "mainSteps:\n" +
                "- action: aws:runShellScript\n" +
                "  name: ${this.documentName()}\n" +
                "  inputs:\n" +
                "    runCommand:\n" +
                "    - iptables -A INPUT -p tcp -m tcp --dport 53 -j DROP\n" +
                "    - iptables -A INPUT -p udp -m udp --dport 53 -j DROP\n" +
                "    - sleep $duration\n" +
                "    - iptables -D INPUT -p tcp -m tcp --dport 53 -j DROP\n" +
                "    - iptables -D INPUT -p udp -m udp --dport 53 -j DROP"
}
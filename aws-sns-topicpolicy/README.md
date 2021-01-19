# AWS::SNS::Topic

This package contains CloudFormation resources for SNS topic policy to control access to topics.

The [documentation folder](aws-sns-topicpolicy/docs/README.md) contains the detailed description of the supported topic attributes.

To make changes to the topic policy resource:

1. Update the JSON schema `aws-sns-topic.json`
1. Implement necessary changes in the resource handlers

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/) to enable auto-complete for Lombok-annotated classes.

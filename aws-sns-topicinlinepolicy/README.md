# AWS::SNS::TopicInlinePolicy

This package contains the CloudFormation resource for SNS topic policy.

The AWS::SNS::TopicInlinePolicy section in the CloudFormation User Guide contains the detailed description of each supported property.

To make changes to the **TopicInlinePolicy** resource:

1. Update the JSON schema `aws-sns-topicinlinepolicy.json`
1. Implement your changes to the resource handlers

The CloudFormation CLI automatically generates the correct resource model from the schema, whenever the project is built via Maven. You can also do this manually, using the following command: `cfn generate`.

You should not modify the files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/). You may have to install IDE integrations to enable auto-complete for Lombok-annotated classes.

# AWS::SNS::Subscription

This package contains CloudFormation resources for SNS subscriptions: endpoints to receive messages published to [topics](aws-ivvladim-package-readmes/aws-sns-topic/README.md). 

The [documentation folder](aws-sns-subscription/docs/README.md) contains the detailed description of the supported subscription attributes.

To make changes to the subscription resouce: 

1. Update the JSON schema `aws-sns-subscription.json`
1. Implement changes to the resource handlers.

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

## The CloudFormation Resource Provider Package For Amazon Simple Notification Service (SNS)

This repository contains AWS-owned resource providers for the `AWS::SNS::*` namespace. It contains all of the currently supported CloudFormation resources for Amazon Simple Notification Service ([SNS](https://aws.amazon.com/sns/)).

Users can download the code and deploy the package in an AWS account as a private [AWS CloudFormation Registry](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/registry.html). To learn how to develop new resource types, see [Developing resource types for use in AWS CloudFormation templates](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html). To learn how to develop and deploy custom resources to an AWS account, see [Walkthrough: Develop a resource type](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough.html).


[![Build Status](https://travis-ci.com/aws-cloudformation/aws-cloudformation-resource-providers-sns.svg?branch=master)](https://travis-ci.com/aws-cloudformation/aws-cloudformation-resource-providers-sns)

Developing SNS Resources
------------------------

The CloudFormation CLI (`cfn`) enables you to modify resource providers, such as this one for SNS, which can then be used in CloudFormation. To learn more, see the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) reposiotry in GitHub.

Before you start makling changes to the SNS resource provider, you need to install the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk), as a required dependency:

```shell
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```

Now, you're ready to clone this repository and start making the changes that you need. As part of your development, you will use linting via [pre-commit](https://pre-commit.com/), which is performed automatically upon commit. The continuous integration also runs these checks.

```shell
pre-commit install
```

Manual options are available, in case you don't need to commit:

```shell
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests and coverage checks
mvn verify
```

Deploying SNS Resources to an AWS Account
-----------------------------------------

After cloning this repository into your local workspace, `cd` to one of the folders under `aws-sns-*`.

```shell
# clean and build maven package
mvn clean && mvn package
#  run the submit command to register the resource type in an aws region (e.g. us-east-1)
cfn submit -v --region [us-east-1]
```

For more detailed information on resource submission, see [Submit the resource type](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough.html#resource-type-walkthrough-submit) in the CloudFormation walkthrough.

Official User Guide for SNS Resources in CloudFormation
-------------------------------------------------------

- [AWS::SNS::Subscription](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-sns-subscription.html)
- [AWS::SNS::Topic](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sns-topic.html)
- [AWS::SNS::TopicPolicy](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sns-policy.html)

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for more information.

## Security

See [SECURITY ISSUE NOTIFICATIONS](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the Apache 2.0 License.

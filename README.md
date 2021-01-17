## The CloudFormation Resource Provider Package For AWS Simple Notification Service

This repository contains AWS-owned resource providers for the `AWS::SNS::*` namespace. It contains all of the currently supported CloudFormation Resources for the Amazon Simple Notification Service ([SNS](https://aws.amazon.com/sns/)).

Users can download the code and deploy the package in an AWS account as a private [CloudFormation Registry](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/registry.html). Please see this [link](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html) on how to develop new resource types. Here is a [walkthrough](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough.html) on how to develop and deploy a customized resource to an AWS account.

Usage
-----

The CloudFormation CLI (cfn) allows you to author your own resource providers that can be used by CloudFormation.

Refer to the documentation for the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) for usage instructions.


Development
-----------

First, you will need to install the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk), as it is a required dependency:

```shell
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```

Linting is done via [pre-commit](https://pre-commit.com/), and is performed automatically on commit. The continuous integration also runs these checks.

```shell
pre-commit install
```

Manual options are available so you don't have to commit:

```shell
# run all hooks on all files, mirrors what the CI runs
pre-commit run --all-files
# run unit tests and coverage checks
mvn verify
```

Deploying SNS Resources to an AWS Account
----------------------------------

After cloning this repository into your local workspace, `cd` to one of the folders under `aws-sns-*`.

```shell
# clean and build maven package
mvn clean && mvn package
#  run the submit command to register the resource type in an aws region (e.g. us-east-1)
cfn submit -v --region [us-east-1]
```

Please see [here](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough.html#resource-type-walkthrough-submit) for more detailed information on resource submission.


Official User Guide for SNS CloudFormation Resources
----------------------------------------------------

- [AWS::SNS::Subscription](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-sns-subscription.html)
- [AWS::SNS::Topic](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sns-topic.html)
- [AWS::SNS::TopicPolicy](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sns-policy.html)


## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for more information.

## Security

See [SECURITY ISSUE NOTIFICATIONS](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the Apache 2.0 License.

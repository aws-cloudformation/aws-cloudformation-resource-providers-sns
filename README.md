## The CloudFormation Resource Provider Package For Amazon Simple Notification Service (SNS)

This repository contains AWS-owned resource providers for the `AWS::SNS::*` namespace. It contains all of the currently supported CloudFormation resources for Amazon Simple Notification Service ([SNS](https://aws.amazon.com/sns/)).

Users can download the code and deploy the package in an AWS account as a private [AWS CloudFormation Registry](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/registry.html). To learn how to develop new resource types, see [Developing resource types for use in AWS CloudFormation templates](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html). To learn how to develop and deploy custom resources to an AWS account, see [Walkthrough: Develop a resource type](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-walkthrough.html).

Developing SNS Resources
------------------------

The CloudFormation CLI (`cfn`) enables you to modify resource providers, such as this one for SNS, which can then be used in CloudFormation. To learn more, see the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk) reposiotry in GitHub.

Before you start making changes to the SNS resource provider, you need to install the [CloudFormation CLI](https://github.com/aws-cloudformation/aws-cloudformation-rpdk), as a required dependency:

```shell
pip3 install cloudformation-cli
pip3 install cloudformation-cli-java-plugin
```

Now, you're ready to clone this repository and start making the changes that you need. For that, [pre-commit](https://pre-commit.com/) is the linter that we use to do code style checks. You can install it by following the [Installation Guide](https://pre-commit.com/#install). Linting via `pre-commit` is performed automatically upon commit. The continuous integration also runs these style checks.

```shell
# Install the git hook scripts
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

After cloning this repository into your local workspace, `cd` to one of the folders corresponding to the resource you would like to build, for example `aws-sns-topic/` for the `AWS::SNS::Topic` resource.

```shell
# cd aws-sns-topic/ (the folder for topic resource)
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

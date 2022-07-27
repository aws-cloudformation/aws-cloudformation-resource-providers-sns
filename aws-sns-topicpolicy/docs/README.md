# AWS::SNS::TopicPolicy

Resource Type definition for AWS::SNS::TopicPolicy, this resource associates Amazon SNS topics with a policy.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::TopicPolicy",
    "Properties" : {
        "<a href="#policydocument" title="PolicyDocument">PolicyDocument</a>" : <i>Map</i>,
        "<a href="#topics" title="Topics">Topics</a>" : <i>[ String, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::TopicPolicy
Properties:
    <a href="#policydocument" title="PolicyDocument">PolicyDocument</a>: <i>Map</i>
    <a href="#topics" title="Topics">Topics</a>: <i>
      - String</i>
</pre>

## Properties

#### PolicyDocument

A policy document that contains permissions to add to the specified SNS topics.

_Required_: Yes

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Topics

The Amazon Resource Names (ARN) of the topics to which you want to add the policy. You can use the [Ref](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-ref.html)` function to specify an [AWS::SNS::Topic](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sns-topic.html) resource.

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Id.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Id

The provider-assigned unique ID for this managed resource.

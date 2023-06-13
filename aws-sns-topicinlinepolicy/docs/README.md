# AWS::SNS::TopicInlinePolicy

Schema for AWS::SNS::TopicInlinePolicy

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::TopicInlinePolicy",
    "Properties" : {
        "<a href="#policydocument" title="PolicyDocument">PolicyDocument</a>" : <i>Map</i>,
        "<a href="#topicarn" title="TopicArn">TopicArn</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::TopicInlinePolicy
Properties:
    <a href="#policydocument" title="PolicyDocument">PolicyDocument</a>: <i>Map</i>
    <a href="#topicarn" title="TopicArn">TopicArn</a>: <i>String</i>
</pre>

## Properties

#### PolicyDocument

A policy document that contains permissions to add to the specified SNS topics.

_Required_: Yes

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicArn

The Amazon Resource Name (ARN) of the topic to which you want to add the policy.

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the TopicArn.

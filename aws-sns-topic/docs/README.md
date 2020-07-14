# AWS::SNS::Topic

Resource Type definition for AWS::SNS::Topic

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::Topic",
    "Properties" : {
        "<a href="#contentbaseddeduplication" title="ContentBasedDeduplication">ContentBasedDeduplication</a>" : <i>Boolean</i>,
        "<a href="#displayname" title="DisplayName">DisplayName</a>" : <i>String</i>,
        "<a href="#fifotopic" title="FifoTopic">FifoTopic</a>" : <i>Boolean</i>,
        "<a href="#kmsmasterkeyid" title="KmsMasterKeyId">KmsMasterKeyId</a>" : <i>String</i>,
        "<a href="#subscription" title="Subscription">Subscription</a>" : <i>[ <a href="subscription.md">Subscription</a>, ... ]</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tags.md">Tags</a>, ... ]</i>,
        "<a href="#topicname" title="TopicName">TopicName</a>" : <i>String</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::Topic
Properties:
    <a href="#contentbaseddeduplication" title="ContentBasedDeduplication">ContentBasedDeduplication</a>: <i>Boolean</i>
    <a href="#displayname" title="DisplayName">DisplayName</a>: <i>String</i>
    <a href="#fifotopic" title="FifoTopic">FifoTopic</a>: <i>Boolean</i>
    <a href="#kmsmasterkeyid" title="KmsMasterKeyId">KmsMasterKeyId</a>: <i>String</i>
    <a href="#subscription" title="Subscription">Subscription</a>: <i>
      - <a href="subscription.md">Subscription</a></i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tags.md">Tags</a></i>
    <a href="#topicname" title="TopicName">TopicName</a>: <i>String</i>
</pre>

## Properties

#### ContentBasedDeduplication

Not currently supported by AWS CloudFormation.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DisplayName

The display name to use for an Amazon SNS topic with SNS subscriptions.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### FifoTopic

Not currently supported by AWS CloudFormation.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KmsMasterKeyId

The ID of an AWS-managed customer master key (CMK) for Amazon SNS or a custom CMK

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Subscription

The SNS subscriptions (endpoints) for this topic.

_Required_: No

_Type_: List of <a href="subscription.md">Subscription</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tags.md">Tags</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicName

The name of the topic

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Pattern_: <code>[a-zA-Z0-9_-]+</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Id.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Id

Returns the <code>Id</code> value.


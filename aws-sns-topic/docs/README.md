# AWS::SNS::Topic

Resource Type definition for AWS::SNS::Topic

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::Topic",
    "Properties" : {
        "<a href="#displayname" title="DisplayName">DisplayName</a>" : <i>String</i>,
        "<a href="#kmsmasterkeyid" title="KmsMasterKeyId">KmsMasterKeyId</a>" : <i>String</i>,
        "<a href="#dataprotectionpolicy" title="DataProtectionPolicy">DataProtectionPolicy</a>" : <i>Map</i>,
        "<a href="#subscription" title="Subscription">Subscription</a>" : <i>[ [ <a href="subscription.md">Subscription</a>, ... ], ... ]</i>,
        "<a href="#fifotopic" title="FifoTopic">FifoTopic</a>" : <i>Boolean</i>,
        "<a href="#contentbaseddeduplication" title="ContentBasedDeduplication">ContentBasedDeduplication</a>" : <i>Boolean</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#topicname" title="TopicName">TopicName</a>" : <i>String</i>,
        "<a href="#signatureversion" title="SignatureVersion">SignatureVersion</a>" : <i>String</i>,
        "<a href="#tracingconfig" title="TracingConfig">TracingConfig</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::Topic
Properties:
    <a href="#displayname" title="DisplayName">DisplayName</a>: <i>String</i>
    <a href="#kmsmasterkeyid" title="KmsMasterKeyId">KmsMasterKeyId</a>: <i>String</i>
    <a href="#dataprotectionpolicy" title="DataProtectionPolicy">DataProtectionPolicy</a>: <i>Map</i>
    <a href="#subscription" title="Subscription">Subscription</a>: <i>
      -
      - <a href="subscription.md">Subscription</a></i>
    <a href="#fifotopic" title="FifoTopic">FifoTopic</a>: <i>Boolean</i>
    <a href="#contentbaseddeduplication" title="ContentBasedDeduplication">ContentBasedDeduplication</a>: <i>Boolean</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#topicname" title="TopicName">TopicName</a>: <i>String</i>
    <a href="#signatureversion" title="SignatureVersion">SignatureVersion</a>: <i>String</i>
    <a href="#tracingconfig" title="TracingConfig">TracingConfig</a>: <i>String</i>
</pre>

## Properties

#### DisplayName

The display name to use for an Amazon SNS topic with SMS subscriptions.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KmsMasterKeyId

The ID of an AWS-managed customer master key (CMK) for Amazon SNS or a custom CMK. For more information, see Key Terms. For more examples, see KeyId in the AWS Key Management Service API Reference.

This property applies only to [server-side-encryption](https://docs.aws.amazon.com/sns/latest/dg/sns-server-side-encryption.html).

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DataProtectionPolicy

The body of the policy document you want to use for this topic.

You can only add one policy per topic.

The policy must be in JSON string format.

Length Constraints: Maximum length of 30720

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Subscription

The SNS subscriptions (endpoints) for this topic.

_Required_: No

_Type_: List of List of <a href="subscription.md">Subscription</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### FifoTopic

Set to true to create a FIFO topic.

_Required_: No

_Type_: Boolean

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### ContentBasedDeduplication

Enables content-based deduplication for FIFO topics. By default, ContentBasedDeduplication is set to false. If you create a FIFO topic and this attribute is false, you must specify a value for the MessageDeduplicationId parameter for the Publish action.

When you set ContentBasedDeduplication to true, Amazon SNS uses a SHA-256 hash to generate the MessageDeduplicationId using the body of the message (but not the attributes of the message).

(Optional) To override the generated value, you can specify a value for the the MessageDeduplicationId parameter for the Publish action.



_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Tags

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicName

The name of the topic you want to create. Topic names must include only uppercase and lowercase ASCII letters, numbers, underscores, and hyphens, and must be between 1 and 256 characters long. FIFO topic names must end with .fifo.

If you don't specify a name, AWS CloudFormation generates a unique physical ID and uses that ID for the topic name. For more information, see Name Type.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SignatureVersion

Version of the Amazon SNS signature used. If the SignatureVersion is 1, Signature is a Base64-encoded SHA1withRSA signature of the Message, MessageId, Type, Timestamp, and TopicArn values. If the SignatureVersion is 2, Signature is a Base64-encoded SHA256withRSA signature of the Message, MessageId, Type, Timestamp, and TopicArn values.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TracingConfig

Tracing mode of an Amazon SNS topic. By default TracingConfig is set to PassThrough, and the topic passes through the tracing header it receives from an SNS publisher to its subscriptions. If set to Active, SNS will vend X-Ray segment data to topic owner account if the sampled flag in the tracing header is true. Only supported on standard topics.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the TopicArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### TopicArn

Returns the <code>TopicArn</code> value.

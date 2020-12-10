# AWS::SNS::Subscription

The AWS::SNS::Subscription resource subscribes an endpoint to an Amazon Simple Notification Service (Amazon SNS) topic. For a subscription to be created, the owner of the endpoint must confirm the subscription.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::Subscription",
    "Properties" : {
        "<a href="#deliverypolicy" title="DeliveryPolicy">DeliveryPolicy</a>" : <i>Map</i>,
        "<a href="#endpoint" title="Endpoint">Endpoint</a>" : <i>String</i>,
        "<a href="#protocol" title="Protocol">Protocol</a>" : <i>String</i>,
        "<a href="#rawmessagedelivery" title="RawMessageDelivery">RawMessageDelivery</a>" : <i>Boolean</i>,
        "<a href="#region" title="Region">Region</a>" : <i>String</i>,
        "<a href="#subscriptionrolearn" title="SubscriptionRoleArn">SubscriptionRoleArn</a>" : <i>String</i>,
        "<a href="#topicarn" title="TopicArn">TopicArn</a>" : <i>String</i>,
        "<a href="#filterpolicy" title="FilterPolicy">FilterPolicy</a>" : <i>Map</i>,
        "<a href="#redrivepolicy" title="RedrivePolicy">RedrivePolicy</a>" : <i>Map</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::Subscription
Properties:
    <a href="#deliverypolicy" title="DeliveryPolicy">DeliveryPolicy</a>: <i>Map</i>
    <a href="#endpoint" title="Endpoint">Endpoint</a>: <i>String</i>
    <a href="#protocol" title="Protocol">Protocol</a>: <i>String</i>
    <a href="#rawmessagedelivery" title="RawMessageDelivery">RawMessageDelivery</a>: <i>Boolean</i>
    <a href="#region" title="Region">Region</a>: <i>String</i>
    <a href="#subscriptionrolearn" title="SubscriptionRoleArn">SubscriptionRoleArn</a>: <i>String</i>
    <a href="#topicarn" title="TopicArn">TopicArn</a>: <i>String</i>
    <a href="#filterpolicy" title="FilterPolicy">FilterPolicy</a>: <i>Map</i>
    <a href="#redrivepolicy" title="RedrivePolicy">RedrivePolicy</a>: <i>Map</i>
</pre>

## Properties

#### DeliveryPolicy

The delivery policy JSON assigned to the subscription. Enables the subscriber to define the message delivery retry strategy in the case of an HTTP/S endpoint subscribed to the topic. For more information, see [GetSubscriptionAttributes](https://docs.aws.amazon.com/sns/latest/api/API_GetSubscriptionAttributes.html) in the <i>Amazon Simple Notification Service API Reference</i> and [Message Delivery Retries](https://docs.aws.amazon.com/sns/latest/dg/sns-message-delivery-retries.html) in the <i>Amazon SNS Developer Guide</i>.

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Endpoint

The subscription's endpoint. The endpoint value depends on the protocol that you specify. For more information, see the Endpoint parameter of the [Subscribe](https://docs.aws.amazon.com/sns/latest/api/API_Subscribe.html) action in the <i>Amazon Simple Notification Service API Reference</i>.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Protocol

The subscription's protocol. For more information, see the `Protocol` parameter of the [Subscribe](https://docs.aws.amazon.com/sns/latest/api/API_Subscribe.html) action in the Amazon Simple Notification Service API Reference.

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### RawMessageDelivery

When set to `true`, enables raw message delivery. Raw messages don't contain any JSON formatting and can be sent to Amazon SQS and HTTP/S endpoints. For more information, see [GetSubscriptionAttributes](https://docs.aws.amazon.com/sns/latest/api/API_GetSubscriptionAttributes.html) in the <i>Amazon Simple Notification Service API Reference</i>.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Region

For cross-region subscriptions, the region in which the topic resides.

If no region is specified, CloudFormation uses the region of the caller as the default.

If you perform an update operation that only updates the Region property of a `AWS::SNS::Subscription` resource, that operation will fail unless you are either:

 - Updating the Region from NULL to the caller region.

 - Updating the Region from the caller region to NULL.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SubscriptionRoleArn

The ARN of the IAM role that has the following:
    * Permission to write to the Amazon Kinesis Data Firehose delivery stream
    * Amazon SNS listed as a trusted entity.
Specifying a valid ARN for this attribute is required for Amazon Kinesis Data Firehose delivery stream subscriptions. For more information, see [Fanout to Amazon Kinesis Data Firehose delivery streams](https://alpha-docs-aws.amazon.com/sns/latest/dg/sns-kinesis-subscriber.html) in the _Amazon SNS Developer Guide_.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicArn

The ARN of the topic to subscribe to.

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### FilterPolicy

The filter policy JSON assigned to the subscription. Enables the subscriber to filter out unwanted messages. For more information, see [GetSubscriptionAttributes](https://docs.aws.amazon.com/sns/latest/api/API_GetSubscriptionAttributes.html) in the <i>Amazon Simple Notification Service API Reference</i> and [Message Filtering](https://docs.aws.amazon.com/sns/latest/dg/sns-message-filtering.html) in the <i>Amazon SNS Developer Guide</i>.

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RedrivePolicy

When specified, sends undeliverable messages to the specified Amazon SQS dead-letter queue. Messages that can't be delivered due to client errors (for example, when the subscribed endpoint is unreachable) or server errors (for example, when the service that powers the subscribed endpoint becomes unavailable) are held in the dead-letter queue for further analysis or reprocessing.

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the SubscriptionArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### SubscriptionArn

This is the subscription amazon resource name generated at creation time.

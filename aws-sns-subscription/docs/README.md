# AWS::SNS::Subscription

The AWS::SNS::Subscription resource subscribes an endpoint to an Amazon Simple Notification Service (Amazon SNS) topic. For a subscription to be created, the owner of the endpoint must confirm the subscription.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::SNS::Subscription",
    "Properties" : {
        "<a href="#deliverypolicy" title="DeliveryPolicy">DeliveryPolicy</a>" : <i><a href="deliverypolicy.md">DeliveryPolicy</a></i>,
        "<a href="#endpoint" title="Endpoint">Endpoint</a>" : <i>String</i>,
        "<a href="#owner" title="Owner">Owner</a>" : <i>String</i>,
        "<a href="#protocol" title="Protocol">Protocol</a>" : <i>String</i>,
        "<a href="#rawmessagedelivery" title="RawMessageDelivery">RawMessageDelivery</a>" : <i>Boolean</i>,
        "<a href="#region" title="Region">Region</a>" : <i>String</i>,
        "<a href="#topicarn" title="TopicArn">TopicArn</a>" : <i>String</i>,
        "<a href="#filterpolicy" title="FilterPolicy">FilterPolicy</a>" : <i><a href="filterpolicy.md">FilterPolicy</a></i>,
        "<a href="#redrivepolicy" title="RedrivePolicy">RedrivePolicy</a>" : <i><a href="redrivepolicy.md">RedrivePolicy</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::SNS::Subscription
Properties:
    <a href="#deliverypolicy" title="DeliveryPolicy">DeliveryPolicy</a>: <i><a href="deliverypolicy.md">DeliveryPolicy</a></i>
    <a href="#endpoint" title="Endpoint">Endpoint</a>: <i>String</i>
    <a href="#owner" title="Owner">Owner</a>: <i>String</i>
    <a href="#protocol" title="Protocol">Protocol</a>: <i>String</i>
    <a href="#rawmessagedelivery" title="RawMessageDelivery">RawMessageDelivery</a>: <i>Boolean</i>
    <a href="#region" title="Region">Region</a>: <i>String</i>
    <a href="#topicarn" title="TopicArn">TopicArn</a>: <i>String</i>
    <a href="#filterpolicy" title="FilterPolicy">FilterPolicy</a>: <i><a href="filterpolicy.md">FilterPolicy</a></i>
    <a href="#redrivepolicy" title="RedrivePolicy">RedrivePolicy</a>: <i><a href="redrivepolicy.md">RedrivePolicy</a></i>
</pre>

## Properties

#### DeliveryPolicy

The delivery policy JSON assigned to the subscription. Enables the subscriber to define the message delivery retry strategy in the case of an HTTP/S endpoint subscribed to the topic.

_Required_: No

_Type_: <a href="deliverypolicy.md">DeliveryPolicy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Endpoint

The subscription's endpoint. The endpoint value depends on the protocol that you specify.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Owner

The AWS account ID of the subscription's owner.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Protocol

The subscription's protocol. For more information, see the Protocol parameter of the Subscribe action in the Amazon Simple Notification Service API Reference.

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### RawMessageDelivery

When set to true, enables raw message delivery. Raw messages don't contain any JSON formatting and can be sent to Amazon SQS and HTTP/S endpoints.

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Region

For cross-region subscriptions, the region in which the topic resides. If no region is specified, CloudFormation uses the region of the caller as the default.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TopicArn

The ARN of the topic to subscribe to.

_Required_: Yes

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### FilterPolicy

The filter policy JSON assigned to the subscription. Enables the subscriber to filter out unwanted messages.

_Required_: No

_Type_: <a href="filterpolicy.md">FilterPolicy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RedrivePolicy

When specified, sends undeliverable messages to the specified Amazon SQS dead-letter queue. Messages that can't be delivered due to client errors (for example, when the subscribed endpoint is unreachable) or server errors (for example, when the service that powers the subscribed endpoint becomes unavailable) are held in the dead-letter queue for further analysis or reprocessing.

_Required_: No

_Type_: <a href="redrivepolicy.md">RedrivePolicy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the SubscriptionArn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### SubscriptionArn

Returns the <code>SubscriptionArn</code> value.


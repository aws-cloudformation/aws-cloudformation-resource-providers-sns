# AWS::SNS::Subscription DeliveryPolicy

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#mindelaytarget" title="minDelayTarget">minDelayTarget</a>" : <i>Double</i>,
    "<a href="#maxdelaytarget" title="maxDelayTarget">maxDelayTarget</a>" : <i>Double</i>,
    "<a href="#numretries" title="numRetries">numRetries</a>" : <i>Double</i>,
    "<a href="#numnodelayretries" title="numNoDelayRetries">numNoDelayRetries</a>" : <i>Double</i>,
    "<a href="#nummindelayretries" title="numMinDelayRetries">numMinDelayRetries</a>" : <i>Double</i>,
    "<a href="#nummaxdelayretries" title="numMaxDelayRetries">numMaxDelayRetries</a>" : <i>Double</i>,
    "<a href="#backofffunction" title="backoffFunction">backoffFunction</a>" : <i>String</i>,
    "<a href="#maxreceivespersecond" title="maxReceivesPerSecond">maxReceivesPerSecond</a>" : <i>Double</i>
}
</pre>

### YAML

<pre>
<a href="#mindelaytarget" title="minDelayTarget">minDelayTarget</a>: <i>Double</i>
<a href="#maxdelaytarget" title="maxDelayTarget">maxDelayTarget</a>: <i>Double</i>
<a href="#numretries" title="numRetries">numRetries</a>: <i>Double</i>
<a href="#numnodelayretries" title="numNoDelayRetries">numNoDelayRetries</a>: <i>Double</i>
<a href="#nummindelayretries" title="numMinDelayRetries">numMinDelayRetries</a>: <i>Double</i>
<a href="#nummaxdelayretries" title="numMaxDelayRetries">numMaxDelayRetries</a>: <i>Double</i>
<a href="#backofffunction" title="backoffFunction">backoffFunction</a>: <i>String</i>
<a href="#maxreceivespersecond" title="maxReceivesPerSecond">maxReceivesPerSecond</a>: <i>Double</i>
</pre>

## Properties

#### minDelayTarget

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### maxDelayTarget

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### numRetries

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### numNoDelayRetries

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### numMinDelayRetries

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### numMaxDelayRetries

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### backoffFunction

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### maxReceivesPerSecond

_Required_: No

_Type_: Double

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)


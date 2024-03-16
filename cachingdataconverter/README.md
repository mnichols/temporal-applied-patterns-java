# caching data converter

## Prerequisites

1. Redis
2. Temporal server locally running


This pattern is useful for those who want to keep all business data in their own environment.
Recall that Temporal receives payload data (inputs and outputs of workflows, activities, signals, etc) for storage
along with the Event messages required for Workflow Execution. 

Protecting against sensitive data leakage can usually be done simply by implementing a `PayloadCodec` which
encrypts and decrypts data being used in the Temporal client, thereby preventing Temporal service from "seeing"
your business data.

There might be more restrictive cases though where data _must_ not leave your own execution environment, necessitating 
a kind of payload cache that must meet the following goals:

#### Very low latency

A WorkflowTask has 10 seconds to complete alot of work, including DataConverter implementation consumption. Redis is chosen in this sample with a smart data model to keep latency down.

#### TTL 

We know we don't want to keep old data forever in our caches but the volume of data required for these executions can grow immensely.
The challenges are that Workflow executions are wildly variable in how long data must be preserved to meet your Workflow Execution needs. 
Even after a Workflow is Completed or Canceled, the Payload data must remain available for Queries.
But what about Workflow Executions that must remain open for years? How can you ensure the cache you are introducing here remains ready to serve the Payloads for these executions?

One possibility is to introduce a Worker interceptor that will set the TTL of all the records meeting a WorkflowID to the
Namespace `Retention Policy` duration (eg 30 days). 
This [RedisCacheCleanerImpl](src/main/java/io/temporal/applied/patterns/cachingdataconverter/temporal/RedisCachingPayloadConverterImpl.java) is
used to mark a workflow "evictable" by a [CacheWorkflowInboundCallsInterceptor](src/main/java/io/temporal/applied/patterns/cachingdataconverter/temporal/CacheWorkflowInboundCallsInterceptor.java)
after the workflow has "Completed" or "Canceled".


**CAUTION**:
_This was attempted in the implementation found in this repo but ultimately you cannot reliably get the `RunId` for a workflow which makes marking a history log as evictable difficult._
Currently this implementation invocation is disabled in the repo but is left for demonstration of
_where_ such a "hook" might be used.

#### Converter Server

Returning a simple cache key for the payload is great for hiding data, but what if I want to debug the actual
data in the workflow? Temporal users typically implement a [CodecServer](https://docs.temporal.io/dataconversion#codec-server)
for this purpose. We are using a `PayloadConverter` (not a `PayloadCodec`) implementation in our sample, but the
result is the same. See an implementation of this [here](frontend/src/main/java/io/temporal/applied/patterns/cachingdataconverter/APIController.java).

Note that the `/decode` handler uses the Redis implementation to yank out Payloads from the cache and serve up the original data in your UI.

See [here](https://docs.temporal.io/production-deployment/data-encryption#set-your-codec-server-endpoints-with-web-ui-and-cli) for steps on configuring your UI to use this "Codec Server".

#### Running The Sample

Run the `backend` and `frontend` Spring Boot Applications. 
Open your console and issue a POST to the `/workflows` endpoint at `localhost:3030`. 

Here I am using _httpie_ to execute the request:

```bash
http -v http://localhost:3030/workflows workflowId=mywid value=42 replyTimeoutSecs=3
```

This should return something like :

```text
"workflow 'mywid' replied with value 'ok-mywid-42' validated as 'true'"
```

This means the underlying workflow was executed and replied back with the `value` you provided, but if you look
in the UI, you'll see only the `cache-key` being sent to Temporal.

You can also inspect payloads doing something like

```text
temporal workflow show --workflow-id mywid -o json
```

Here you'll see payloads look something like:

```json
  <snip>
       "input": {
          "payloads": [
            {
              "metadata": {
                "cache-key": "MGM1ZGEyNDktZDUwYy00ZWZhLWI4YWItNTcxMjdmM2JlOTBh",
                "encoding": "Y2FjaGVkL3BsYWlu",
                "workflow-id": "bXl3aWQ="
              },
              "data": "IjBjNWRhMjQ5LWQ1MGMtNGVmYS1iOGFiLTU3MTI3ZjNiZTkwYSI="
            }
          ]
        },
```


## Garbage Collection Issues
1. Query input is not in history
2. Activities can time out as well
3. 


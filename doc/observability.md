
# api-svc

## Log aggregation

The ARDC RAiD service uses AWS CloudWatch for log aggregation and insights.


Raido has a specific logging strategy to keep our logs manageable and readable,
see [logging.md](../api-svc/doc/logging.md).


## Metrics

Metrics infrastructure has been simplified. The previous Micrometer-based
metrics setup (with JMX for local dev and CloudWatch for AWS) has been removed.

Currently, observability relies on CloudWatch Logs and log-based metrics.


# raid-agency-app

There's no centralised observability for the raid-agency-app at the moment.

The only way to debug a frontend issue is to be on the user's machine
and view the developer console.

Eventually, we will implement some kind of client-side error reporting, 
probably some kind of home-grown solution that logs to cloudwatch - like
BugSnag or Sentry, etc. but trading off cost for usability.

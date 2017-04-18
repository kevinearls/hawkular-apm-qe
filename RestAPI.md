## Basics of the Jaeger REST API

See the **RegisterRoutes** function in handler.go, currently here: https://github.com/uber/jaeger/blob/master/cmd/query/app/handler.go#L120-L130,
and the **parse** function in query_parser.go, currently here: https://github.com/uber/jaeger/blob/master/cmd/query/app/query_parser.go#L68-L81

## Operations
1. http://localhost:3001/api/**services**  -- returns a list of service names
2. http://localhost:3001/api/**operations**?service=qe-automation  -- returns a list of operations; requires service name
3. http://localhost:3001/api/**traces**?service=qe-automation
    1. This requires **servicename**, and accepts the following options
        2. **limit**=n  -- number of traces (most recent?) to receive
	    3. **start**=time in unix microseconds
	    4. **end**=time in unix microseconds
	    5. **minDuration**=strValue (units are "ns", "us" (or "µs"), "ms", "s", "m", "h")
	    6. **maxDuration**=strValue (same units as minDuration)
	    7. **tags**   (Not sure how these work yet.)
	        1. **tag** ::= 'tag=' key | 'tag=' keyvalue
	        2. *key* := strValue
	        3. *keyValue* := strValue ':' strValue
4. http://localhost:3001/api/**traces**/{traceId} -- return a specific trace
5. http://localhost:3001/api/**archive**/{%s} -- TODO 
6. http://localhost:3001/api/**archives**/{%s} -- TODO 
7. http://localhost:3001/api/**dependencies**  -- TODO
8. http://localhost:3001/api/**services**/{%s}/**operations** -- Get operations for a service.  Legacy?  

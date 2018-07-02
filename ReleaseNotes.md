# Release Notes

## 1.0.43 / 2018-07-02 Taking latest IDL version
New IDL changes include expression tree based searching for timeline.

## 1.0.42 / 2018-06-22 Taking latest IDL version
New IDL changes include expression tree based searching in reader.

## 1.0.41 / 2018-06-22 Change haystack-metrics version to 2.0.1

## 1.0.40 / 2018-06-21 Add tag information to source and destination of a graph edge. This would ensure that we can 
pass additional information to the service graph.

## 1.0.39 / 2018-06-07 Make JSON detector except JsonElement instead of JsonObject
The root JSON structure might be a JsonArray instead of a JsonObject, so the code now excepts a JsonElement (the base
class of both JsonArray and JsonObject)

## 1.0.38 / 2018-06-07 Add secret detection for parsed JSON

## 1.0.37 / 2018-06-05 No code changes
Handle last logged time properly at startup

## 1.0.36 / 2018-06-04 No code changes
Had to tag again

## 1.0.35 / 2018-06-04 Make SpanNameAndCountRecorder constructor public
Also fixed a typo in a variable name.

## 1.0.34 / 2018-06-04 Add SpanNameAndCountRecorder
Finish implementing secret ID logging for spans.

## 1.0.33 / 2018-05-31 Add SpanNameAndCountRecorder
There are no plans to release this code until the secret ID logging is in place.

## 1.0.32 / 2018-05-30 Refactoring in preparation for secret ID logging
There are no plans to release this code until the secret ID logging is in place.

## 1.0.31 / 2018-05-24 Look for GB and FR phone numbers
If this scales, additional Western European regions will be added.
Also made the IP v4 checker regular expression more correct.

## 1.0.30 / 2018-05-24 1.0.28 missed a use of Maven StringUtils.isNotEmpty(); fixed this
The 1.0.28 tag was not applied to the correct code, so building 1.0.29.

## 1.0.29 / 2018-05-24 No functional changes: reformatted code in Java tests
The 1.0.28 tag was not applied to the correct code, so building 1.0.29.

## 1.0.28 / 2018-05-24 Use Guava !Strings.isNullOrEmpty() instead of Maven StringUtils.isNotEmpty()

## 1.0.27 / 2018-05-24 Don't spam logs when white list cannot be fetched from S3 (only write error once per hour)

## 1.0.26 / 2018-05-24 Make more dependencies provided

## 1.0.25 / 2018-05-23 Make SpanSecretMasker.Factory.createCounter() return a counter in the "errors" metric group
Our use of PCI/PII finders has evolved from "detection" to "masking" and the counters that are counting what secret
data has been found need to show on the same Grafana graph, so that metric names must match, which they did not before
this change.

## 1.0.24 / 2018-05-22 Write SpanSecretMasker
This class masks secrets when they are found in Span objects.

## 1.0.23 / 2018-05-17 Make XmlDetector not implement ValueMapper
This class is only used by haystack-data-scrubber, which doesn't have a dependency on any Kafka code.
(ValueMapper is a Kafka class.) The XmlDetector.apply() method and its tests are now commented out. 

## 1.0.22 / 2018-05-17 Make SpanS3ConfigFetcher.SpanFactory and XmlS3ConfigFetcher.XmlFactory public
These classes need to be public so that the can be wired by Spring in the haystack-pipes package.

## 1.0.21 / 2018-05-17 Added an XML secret detector

## 1.0.20 / 2018-05-14 Add some convenience constructors to to some of the classes moved from Pipes

## 1.0.19 / 2018-05-10 Moved more Java classes from Pipes

## 1.0.18 / 2018-05-09 Updated idl with timeline trace count entities

## 1.0.17 / 2018-05-08 Make S3ConfigFetcher.Factory class public

## 1.0.16 / 2018-05-08 Make some migrated methods public
These methods were package private in haystack-pipes but need to be public after being moved.

## 1.0.15 / 2018-05-08 More Java classes from secret-detector
Copy some more classes from haystack-pipes/pipes-commons:
* Detector
* FinderNameAndServiceName
* S3ConfigFetcher
* WhiteListConfig
* WhiteListItem
Also copied some unit tests that were for the above classes and classes from the 1.0.14 change.

## 1.0.14 / 2018-05-04 Java classes from secret-detector
Copy four secret finders from haystack-pipes/pipes-commons:
* HaystackCompositeCreditCardFinder
* HaystackCreditCardFinder
* HaystackPhoneNumberFinder
* NonLocalIpV4AddressFinder
Putting them in haystack-commons permits their use in haystack-collector, to detect and mask PCI/PII data as spans are
read from Kinesis and then written to Kafka.

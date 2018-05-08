# Release Notes

## 1.0.16 / 2018-05-08 Make some ,migrated methods public
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
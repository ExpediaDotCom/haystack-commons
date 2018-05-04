# Release Notes

## 1.0.14 / 2018-05-04 No changes
Copy four secret finders from haystack-pipes/pipes-commons:
* HaystackCompositeCreditCardFinder
* HaystackCreditCardFinder
* HaystackPhoneNumberFinder
* NonLocalIpV4AddressFinder
Putting them in haystack-commons permits their use in haystack-collector, to detect and mask PCI/PII data as spans are
read from Kinesis and then written to Kafka.
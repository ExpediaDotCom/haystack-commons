package com.expedia.www.haystack.commons.entities.encodings

import com.expedia.www.haystack.commons.unit.UnitTestSpec

class EncodingFactorySpec extends UnitTestSpec {
  "EncodingFactory" should {

    "return a NoopEncoding by default for null" in {
      When("encoding is null")
      val encoding = EncodingFactory.newInstance(null)

      Then("should be a NoopEncoding")
      encoding shouldBe an[NoopEncoding]
    }

    "return a NoopEncoding by default for empty string" in {
      When("encoding is empty string")
      val encoding = EncodingFactory.newInstance("")

      Then("should be a NoopEncoding")
      encoding shouldBe an[NoopEncoding]
    }

    "return a Base64Encoding when value = base64" in {
      When("encoding is empty string")
      val encoding = EncodingFactory.newInstance(EncodingFactory.BASE_64)

      Then("should be a Base64Encoding")
      encoding shouldBe an[Base64Encoding]
    }

    "return a Base64Encoding when value = baSe64" in {
      When("encoding is empty string")
      val encoding = EncodingFactory.newInstance("baSe64")

      Then("should be a Base64Encoding")
      encoding shouldBe an[Base64Encoding]
    }

    "return a PeriodReplacementEncoding when value = periodreplacement" in {
      When("encoding is empty string")
      val encoding = EncodingFactory.newInstance("periodreplacement")

      Then("should be a PeriodReplacementEncoding")
      encoding shouldBe an[PeriodReplacementEncoding]
    }

    "return a PeriodReplacementEncoding when value = periodReplacement" in {
      When("encoding is empty string")
      val encoding = EncodingFactory.newInstance(EncodingFactory.PERIOD_REPLACEMENT)

      Then("should be a PeriodReplacementEncoding")
      encoding shouldBe an[PeriodReplacementEncoding]
    }
  }
}

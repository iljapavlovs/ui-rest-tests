@ui
@cheese
Feature: Google Search For Cheese

  Scenario: Search for cheese2
    Given I navigate to Google page
    When I search for "Cheese!"
    Then first result should contain word "cheese"
    And I purposefully fail this scenario to get a screenshot
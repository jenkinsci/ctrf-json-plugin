# ctrf-json-plugin

## Introduction

The ctrf-json-plugin for Jenkins allows users to publish Common Test Report Format (CTRF) JSON test results in Jenkins, providing a standardized way to report and visualize test outcomes.

## What is CTRF?

[Common Test Report Format](https://ctrf.io) is a JSON test report schema that provides standardized JSON test results reports.

*Consistency Across Tools:* Different testing tools and frameworks often produce reports in varied formats. CTRF provides a common schema, making it easy to understand and compare reports, regardless of the testing tool used.

*Language and Framework Agnostic:* A universal reporting schema that works seamlessly with any programming language and testing framework.

*Facilitates Enhanced Programmatic Use:* Programatically interacting with test outcomes across multiple tools becomes more straightforward.

## Installation

1. **Install the Plugin:** Navigate to your Jenkins instance's **Manage Jenkins** > **Manage Plugins** section. Search for the **ctrf-json-plugin** under the **Available** tab, select it, and click **Install without restart**.

2. **Restart Jenkins:** Although not always necessary, a restart ensures all plugin features are properly loaded.

## Configuration

1. **Create or Configure a Job:** Go to your Jenkins dashboard and either create a new job or configure an existing one.

2. **Add Post Build Step:** In the job configuration, navigate to the **Post Build Actions** section and add a post build step named **Publish CTRF test result report**. This option becomes available after installing the ctrf-json-plugin.

3. **Configure the Plugin:** In the plugin configuration section, specify the pattern for your CTRF JSON files. For example, `**/ctrf/*.json` would match all JSON files within a `ctrf` directory in your workspace.

4. **Save and Run:** Save your job configuration and execute the job. Upon completion, Jenkins will process the specified CTRF JSON files and publish the results.

## Contributing

Contributions to the ctrf-json-plugin are welcome! Please refer to the [CONTRIBUTING.md](CONTRIBUTING.md) file for guidelines on how to make contributions.

## LICENSE

The ctrf-json-plugin is open-sourced software licensed under the [MIT license](LICENSE.md).

to build - mvn clean install -DskipTests

run jenkins on local with latest changes - 
mvn clean package -DskipTests
mvn clean install -DskipTests
mvn hpi:run
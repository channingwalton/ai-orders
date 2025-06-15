# Project Prompts Index

This directory contains all prompts and instructions for the Order Management System project.

## Template Structure

Each prompt should include:
- **Objective**: What you want to achieve
- **Requirements**: Detailed specifications
- **Acceptance Criteria**: Checklist of completion criteria
- **Status**: Current state with emoji indicator

## Process for implementing features

- Refer to the libs in the reference directory
- Refer to the examples in the example directory for code style
- Ensure the feature document has a status and checklist of acceptance criteria
- run `sbt commitCheck` to ensure all tests pass and code is formatted
- Update the project README
- Use bloop to run tests and compile code, use sbt commitCheck for final checks
- When commitCheck passes, commit and push code except except the reference and examples directories

## Status Indicators

- 🟢 **Complete** - Task finished and tested
- 🟡 **In Progress** - Currently being worked on
- ⚪ **Pending** - Not started yet
- ❌ **Blocked** - Cannot proceed due to dependencies

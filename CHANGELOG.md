# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Plugin folder utilities
- Licenses bundled into builds

### Removed
- ArrayOperations util


## [0.2.0] - 2021-08-02
### Added
- Cyclic plugin dependency resolution
- Changed plugin yaml file entries
- Event testing utility
- Changelog

### Changed
- Plugin dependencies now load first, and besides that load order is randomized
- Plugin management is now done using plugin names, not plugin objects directly

## [0.1.0] - 2021-06-10
### Added
- Added Lombok, bundled in for use in plugins
- Logger factory, log annotations

### Changed
- Made Plugin an abstract class, added default methods
- Versioned jar output
- Standardized library folder structure
- Extract name and versions out of plugins

### Removed
- Console GUI

## [0.0.1] - 2019-05-03
### Added
- Event system
- Logging
- Scripting Engine class
- Utilities
	- Array operations
	- Binary Tree
	- File utils
	- Safe resource loading
	- System properties
- Permissions
- Localization

### Changed
- Switched to Semantic Versioning
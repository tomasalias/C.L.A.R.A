# GitHub Workflows for C.L.A.R.A Plugin

This directory contains GitHub Actions workflows for building and testing the C.L.A.R.A (Cheat.Limiting.Adaptive.Response.Algorithm) Minecraft plugin with Gemini 2.5 Flash Lite AI integration.

## Workflows

### 1. `build.yml` - Main Build Workflow
**Primary build workflow for the plugin package**

- **Triggers**: Push to main/master/develop, Pull Requests, Releases
- **Features**:
  - Uses Java 21 (as required by the plugin)
  - Handles Minecraft dependencies (Spigot API, Brigadier) with mock installs
  - Builds plugin JAR with Maven
  - Verifies Gemini AI dependencies are included
  - Uploads build artifacts
  - Creates release assets automatically

### 2. `ci.yml` - Alternative CI Build  
**Fallback build using official Spigot BuildTools**

- **Triggers**: Push/PR to main branches
- **Features**:
  - Downloads and runs Spigot BuildTools for authentic dependencies
  - Attempts compilation even if dependencies fail
  - Provides alternative build path for complex dependency scenarios

### 3. `gemini-test.yml` - AI Integration Testing
**Validates Gemini 2.5 Flash Lite integration**

- **Triggers**: Changes to source code or pom.xml
- **Features**:
  - Verifies Gemini API dependencies (okhttp, gson)
  - Checks for AI-related source files and commands
  - Validates configuration structure
  - Reports integration status

## Plugin Features Verified

✅ **Gemini AI Integration**: Uses Google's Gemini 2.5 Flash Lite model
✅ **HTTP Dependencies**: OkHttp for API communication  
✅ **JSON Processing**: Gson for API data handling
✅ **AI Commands**: `/ai status`, `/ai analyze`, `/ai test`, etc.
✅ **Java 21 Compatible**: Modern Java version support
✅ **Maven Build**: Standard build tool configuration

## Build Artifacts

The main build workflow produces:
- **Plugin JAR**: Ready-to-use Minecraft plugin file
- **Shaded Dependencies**: Includes Gemini AI integration libraries
- **Release Assets**: Automatic uploads for GitHub releases

## Usage

These workflows run automatically on:
- Code pushes to main branches
- Pull request creation/updates
- GitHub release creation

Manual workflow dispatch is also supported for testing purposes.

## Dependencies Handled

The workflows handle several challenging Minecraft-specific dependencies:
- `org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT`
- `com.mojang:brigadier:1.2.9`
- Standard Maven Central dependencies (okhttp, gson, etc.)

## Configuration

No additional configuration is required. The workflows use the existing `pom.xml` and automatically detect the build requirements.
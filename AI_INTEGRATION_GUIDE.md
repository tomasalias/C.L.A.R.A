# C.L.A.R.A AI Integration Documentation

## Overview

C.L.A.R.A now features advanced AI integration using Google's Gemini AI to enhance cheat detection capabilities. This system combines traditional rule-based checks with AI-powered behavior analysis for more accurate and intelligent anti-cheat detection.

## Features

### Core AI Components

1. **Gemini API Integration**: Direct integration with Google's Gemini AI for advanced behavior analysis
2. **Intelligent Prompting**: Sophisticated prompt engineering designed specifically for Minecraft cheat detection
3. **Behavior Data Collection**: Comprehensive collection of player movement, violation, and contextual data
4. **AI-Enhanced Checks**: Traditional checks enhanced with AI decision-making
5. **Configurable Thresholds**: Flexible configuration for different confidence levels and actions

### Supported Analysis Types

- **Speed Hack Detection**: AI analyzes movement patterns for impossible speeds
- **Flight Hack Detection**: Intelligent analysis of vertical movement and air time
- **Reach Hack Detection**: Context-aware reach distance analysis
- **Timer Hack Detection**: Pattern analysis for packet timing manipulation
- **Behavioral Pattern Recognition**: Detection of consistent cheating patterns

## Installation and Setup

### 1. Prerequisites

- Java 21 or higher
- Spigot/Paper 1.20.4+
- Internet connection for Gemini AI API

### 2. API Key Configuration

1. Obtain a Gemini AI API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Edit `plugins/C.L.A.R.A/config.yml`
3. Replace `YOUR_GEMINI_API_KEY_HERE` with your actual API key

```yaml
ai:
  enabled: true
  gemini:
    api_key: "your-actual-api-key-here"
```

### 3. Configuration Options

#### AI Settings
```yaml
ai:
  enabled: true                    # Enable/disable AI integration
  gemini:
    api_key: "YOUR_KEY_HERE"      # Your Gemini API key
    model: "gemini-pro"           # AI model to use
    temperature: 0.3              # Response creativity (0.0-1.0)
    max_tokens: 1000              # Maximum response length
    confidence_threshold: 0.7     # Minimum confidence for AI decisions
    batch_analysis: true          # Enable batch processing
    batch_size: 5                 # Number of players per batch
    analysis_delay_ms: 2000       # Delay between analyses
```

#### Enhanced Checks
```yaml
checks:
  ai_enhanced:
    speed: true     # AI-enhance speed checks
    flight: true    # AI-enhance flight checks
    reach: true     # AI-enhance reach checks
    timer: false    # Traditional timer check is already accurate
```

#### Actions
```yaml
actions:
  high_confidence_action: "kick"    # Action for high AI confidence
  medium_confidence_action: "warn"  # Action for medium confidence
  min_action_confidence: 0.6        # Minimum confidence to take action
```

## Commands

### `/ai status`
Shows the current status of the AI system including:
- AI service status (enabled/disabled)
- Model configuration
- Enhanced check status
- Online player count

### `/ai test <player>`
Performs a test AI analysis on the specified player by simulating a check violation.

### `/ai analyze <player>`
Performs a full AI analysis on the specified player's current behavior data.

### `/ai testconnection`
Tests the connection to the Gemini AI API to verify configuration.

### `/ai reload`
Reloads the AI configuration from the config file.

### `/ai cleanup`
Cleans up AI caches and temporary data.

## How It Works

### 1. Behavior Data Collection
The system continuously collects comprehensive data about each player:

- **Movement Data**: Position, velocity, speed, rotation
- **Violation Data**: Recent check violations and patterns
- **Context Data**: Server performance, player ping, environment

### 2. AI Analysis Process
When a traditional check detects a violation:

1. **Data Collection**: Gather comprehensive behavior data
2. **AI Query**: Send structured prompt to Gemini AI
3. **Analysis**: AI analyzes patterns and provides assessment
4. **Decision Making**: System takes action based on AI recommendation

### 3. Intelligent Prompting
The system uses specialized prompts that:

- Explain Minecraft game mechanics to the AI
- Provide context about legitimate vs. suspicious behaviors
- Include server performance considerations
- Request structured JSON responses

### 4. Action Decision Matrix

| AI Confidence | Traditional Check | Action Taken |
|---------------|-------------------|--------------|
| High (>0.8)   | Any violation     | Kick/Ban     |
| Medium (0.6-0.8) | High severity  | Warn         |
| Low (<0.6)    | High severity     | Log only     |
| Any           | Low severity      | Monitor      |

## Benefits

### Reduced False Positives
AI can distinguish between:
- Server lag vs. actual speed hacks
- Legitimate movement (boats, ice) vs. cheats
- Network issues vs. timer manipulation

### Pattern Recognition
AI excels at detecting:
- Consistent cheating patterns over time
- Subtle behavioral changes
- Complex cheat combinations

### Adaptive Learning
The system improves by:
- Learning from server-specific conditions
- Adapting to new cheat methods
- Considering player behavior context

## Troubleshooting

### Common Issues

**AI Service Disabled**
- Check API key configuration
- Verify internet connection
- Test connection with `/ai testconnection`

**High Response Times**
- Check server internet speed
- Consider reducing `batch_size`
- Increase `analysis_delay_ms`

**False Positives**
- Lower `confidence_threshold`
- Adjust `min_action_confidence`
- Review individual check configurations

**API Errors**
- Verify API key validity
- Check Gemini AI service status
- Review console logs for detailed errors

### Performance Optimization

1. **Batch Analysis**: Enable for better performance with multiple players
2. **Caching**: Results are cached for 30 seconds to avoid duplicate analyses
3. **Async Processing**: All AI analysis runs asynchronously to avoid server lag
4. **Rate Limiting**: Built-in delays prevent API rate limit issues

## Security Considerations

- **API Key Security**: Never share your API key publicly
- **Data Privacy**: Player data is only sent to Gemini AI for analysis
- **Fallback System**: Traditional checks work even if AI fails
- **Local Processing**: All decision-making happens on your server

## API Usage and Costs

- Gemini AI API has usage-based pricing
- Typical analysis uses ~100-500 tokens per request
- Monitor usage through Google Cloud Console
- Consider implementing additional rate limiting for high-traffic servers

## Support

For support with AI integration:
1. Check console logs for detailed error messages
2. Use `/ai status` to verify configuration
3. Test with `/ai testconnection`
4. Review this documentation for troubleshooting steps

## Advanced Configuration

### Custom Prompts
The prompt engine can be customized by modifying:
- `AC.AI.PromptEngine` class
- Analysis framework descriptions
- Response format requirements

### Integration Points
Add AI analysis to custom checks by:
1. Recording violations with `BehaviorDataCollector`
2. Calling `AIEnhancedCheckHandler.processCheckViolation()`
3. Handling results through the callback system

### Monitoring
Enable detailed logging by setting:
```yaml
logging:
  log_ai_analysis: true
  detailed_logging: true
  console_level: "INFO"
```

This provides comprehensive visibility into AI decision-making processes.